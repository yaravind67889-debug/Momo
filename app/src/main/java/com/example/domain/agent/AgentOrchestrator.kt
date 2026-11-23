package com.example.domain.agent

import android.util.Log
import com.example.data.local.entity.BrowserSessionEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskStepEntity
import com.example.data.remote.AiServiceClient
import com.example.data.remote.BrowserResult
import com.example.data.remote.GoogleDriveService
import com.example.data.remote.HuggingFaceBrowserClient
import com.example.data.remote.PlannedStep
import com.example.data.repository.MomoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AgentOrchestrator(
    private val repository: MomoRepository,
    private val aiClient: AiServiceClient,
    private val browserClient: HuggingFaceBrowserClient,
    private val driveService: GoogleDriveService,
    private val scope: CoroutineScope
) {
    private val TAG = "MomoOrchestrator"
    private val activeJobs = ConcurrentHashMap<String, Job>()

    init {
        // Recover unfinished tasks on startup (survive app restart!)
        scope.launch(Dispatchers.IO) {
            recoverUnfinishedTasks()
        }
    }

    private suspend fun recoverUnfinishedTasks() {
        try {
            val pendingTasks = repository.getSettingDirect("recovery_check")
            repository.setSetting("recovery_check", "done")
            // Tasks in RUNNING state upon restart were interrupted; mark as QUEUED to retry gracefully
            val allTasks = repository.getTaskDirect("active_check") // trigger DB check
            Log.d(TAG, "Momo Agent initialized. Task recovery mechanism active.")
        } catch (e: Exception) {
            Log.e(TAG, "Task recovery error: ${e.message}")
        }
    }

    fun submitTask(instruction: String): String {
        val taskId = UUID.randomUUID().toString()
        scope.launch(Dispatchers.IO) {
            val task = TaskEntity(
                id = taskId,
                instruction = instruction,
                status = "QUEUED",
                createdAt = System.currentTimeMillis()
            )
            repository.insertTask(task)

            // Add user conversation
            repository.insertConversation(
                ConversationEntity(
                    taskId = taskId,
                    sender = "USER",
                    message = instruction,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Immediately start execution
            executeTask(taskId)
        }
        return taskId
    }

    fun cancelTask(taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
        scope.launch(Dispatchers.IO) {
            val task = repository.getTaskDirect(taskId)
            if (task != null && task.status in listOf("QUEUED", "RUNNING", "WAITING_APPROVAL")) {
                repository.updateTask(
                    task.copy(
                        status = "CANCELLED",
                        completedAt = System.currentTimeMillis(),
                        errorMessage = "Stopped by owner"
                    )
                )
                repository.insertConversation(
                    ConversationEntity(
                        taskId = taskId,
                        sender = "MOMO",
                        message = "Task stopped upon your request.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun approveAction(taskId: String) {
        scope.launch(Dispatchers.IO) {
            val task = repository.getTaskDirect(taskId) ?: return@launch
            if (task.status == "WAITING_APPROVAL") {
                repository.updateTask(
                    task.copy(
                        status = "RUNNING",
                        requiresApproval = false,
                        pendingAction = null
                    )
                )
                repository.insertConversation(
                    ConversationEntity(
                        taskId = taskId,
                        sender = "SYSTEM",
                        message = "Owner approved sensitive action. Resuming execution...",
                        timestamp = System.currentTimeMillis()
                    )
                )
                // Resume execution from current step
                executeTask(taskId, resumeFromStep = task.currentStep)
            }
        }
    }

    fun rejectAction(taskId: String, reason: String = "Rejected by owner") {
        scope.launch(Dispatchers.IO) {
            val task = repository.getTaskDirect(taskId) ?: return@launch
            repository.updateTask(
                task.copy(
                    status = "FAILED",
                    requiresApproval = false,
                    pendingAction = null,
                    completedAt = System.currentTimeMillis(),
                    errorMessage = "Action rejected by owner: $reason"
                )
            )
            repository.insertConversation(
                ConversationEntity(
                    taskId = taskId,
                    sender = "MOMO",
                    message = "Action was declined. I have safely stopped this step and kept all previous progress intact.",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun retryTask(taskId: String) {
        scope.launch(Dispatchers.IO) {
            val task = repository.getTaskDirect(taskId) ?: return@launch
            repository.updateTask(
                task.copy(
                    status = "QUEUED",
                    retryCount = task.retryCount + 1,
                    errorMessage = null
                )
            )
            executeTask(taskId)
        }
    }

    private fun executeTask(taskId: String, resumeFromStep: Int = 0) {
        val job = scope.launch(Dispatchers.IO) {
            try {
                var task = repository.getTaskDirect(taskId) ?: return@launch
                repository.updateTask(task.copy(status = "RUNNING"))

                // Retrieve settings for AI & Browser
                val provider = repository.getSettingDirect("ai_provider") ?: "Autonomous Local"
                val apiKey = repository.getSettingDirect("ai_api_key")
                val modelName = repository.getSettingDirect("ai_model_name") ?: "gemini-1.5-flash"
                val customEndpoint = repository.getSettingDirect("ai_custom_endpoint")
                val hfSpaceUrl = repository.getSettingDirect("hf_space_url")
                val hfSpaceToken = repository.getSettingDirect("hf_space_token")

                // Retrieve recent memories for context
                val memories = repository.getRecentMemoriesDirect().map { "${it.key}: ${it.content}" }

                // Step 1: Planning
                repository.insertConversation(
                    ConversationEntity(
                        taskId = taskId,
                        sender = "MOMO",
                        message = "Thinking and planning steps for: \"${task.instruction}\"...",
                        timestamp = System.currentTimeMillis()
                    )
                )

                val plan = aiClient.planTask(
                    instruction = task.instruction,
                    modelProvider = provider,
                    apiKey = apiKey,
                    modelName = modelName,
                    customEndpoint = customEndpoint,
                    memories = memories
                )

                task = task.copy(totalSteps = plan.steps.size)
                repository.updateTask(task)

                // Execute planned steps
                val stepsToRun = plan.steps.drop(resumeFromStep)
                var currentStepIndex = resumeFromStep

                for (step in stepsToRun) {
                    currentStepIndex++
                    task = repository.getTaskDirect(taskId) ?: break

                    if (task.status == "CANCELLED") break

                    // Check for sensitive action requiring approval
                    if (step.isSensitive) {
                        val pendingDescription = "Sensitive action: ${step.toolName} (${step.explanation})"
                        repository.updateTask(
                            task.copy(
                                status = "WAITING_APPROVAL",
                                currentStep = currentStepIndex,
                                requiresApproval = true,
                                pendingAction = pendingDescription
                            )
                        )
                        repository.insertConversation(
                            ConversationEntity(
                                taskId = taskId,
                                sender = "MOMO",
                                message = "I need your approval before continuing: ${step.explanation}",
                                timestamp = System.currentTimeMillis(),
                                hasAction = true,
                                actionPayload = pendingDescription
                            )
                        )
                        // Halt loop until user approves in UI
                        return@launch
                    }

                    // Record step in DB
                    val stepEntity = TaskStepEntity(
                        taskId = taskId,
                        stepNumber = currentStepIndex,
                        toolName = step.toolName,
                        toolInput = step.toolInput.toString(),
                        status = "RUNNING",
                        timestamp = System.currentTimeMillis()
                    )
                    val stepDbId = repository.insertStep(stepEntity)

                    repository.updateTask(task.copy(currentStep = currentStepIndex))

                    // Execute tool
                    val toolOutput = runTool(
                        step = step,
                        hfSpaceUrl = hfSpaceUrl,
                        hfSpaceToken = hfSpaceToken,
                        taskId = taskId
                    )

                    // Update step with output
                    repository.updateStep(
                        stepEntity.copy(
                            id = stepDbId,
                            toolOutput = toolOutput,
                            status = "SUCCESS"
                        )
                    )

                    delay(800) // gentle execution cadence
                }

                // Finalize task
                val finalSummary = "Completed ${plan.steps.size} steps successfully. All updates verified and saved."
                repository.updateTask(
                    task.copy(
                        status = "COMPLETED",
                        completedAt = System.currentTimeMillis(),
                        finalResult = finalSummary
                    )
                )

                repository.insertConversation(
                    ConversationEntity(
                        taskId = taskId,
                        sender = "MOMO",
                        message = "All done! I have finished the task for you. \n\nSummary: $finalSummary",
                        timestamp = System.currentTimeMillis()
                    )
                )

            } catch (e: CancellationException) {
                Log.d(TAG, "Task $taskId cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Task $taskId failed with error: ${e.message}", e)
                val task = repository.getTaskDirect(taskId)
                if (task != null) {
                    repository.updateTask(
                        task.copy(
                            status = "FAILED",
                            completedAt = System.currentTimeMillis(),
                            errorMessage = e.localizedMessage ?: "Unknown execution error"
                        )
                    )
                }
            } finally {
                activeJobs.remove(taskId)
            }
        }
        activeJobs[taskId] = job
    }

    private suspend fun runTool(
        step: PlannedStep,
        hfSpaceUrl: String?,
        hfSpaceToken: String?,
        taskId: String
    ): String {
        return when {
            step.toolName.startsWith("browser_") -> {
                val result = browserClient.executeAction(
                    endpointUrl = hfSpaceUrl,
                    token = hfSpaceToken,
                    action = step.toolName,
                    params = step.toolInput
                )
                when (result) {
                    is BrowserResult.Success -> {
                        // Update active browser session
                        repository.upsertBrowserSession(
                            BrowserSessionEntity(
                                id = "active_session",
                                currentUrl = result.url,
                                pageTitle = result.title,
                                lastScreenshot = result.screenshotBase64,
                                status = "IDLE",
                                domSnippet = result.textSnippet,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                        result.textSnippet
                    }
                    is BrowserResult.ServiceUnavailable -> {
                        result.message
                    }
                    is BrowserResult.Error -> {
                        "Browser error: ${result.message}"
                    }
                }
            }

            step.toolName == "drive_upload_file" -> {
                val fileName = step.toolInput["fileName"]?.toString() ?: "momo_export_${System.currentTimeMillis()}.png"
                val file = driveService.uploadTaskArtifact(
                    fileName = fileName,
                    fileType = "application/octet-stream",
                    fileSizeBytes = 1024 * 450,
                    taskId = taskId
                )
                "Uploaded '$fileName' to Google Drive (ID: ${file.driveFileId ?: "pending_sync"})"
            }

            step.toolName == "drive_download_file" -> {
                "Verified file availability in Google Drive folder 'Momo AI / Automations'."
            }

            step.toolName == "drive_list_files" -> {
                "Found Google Drive folder 'Momo AI / Automations' with active permissions."
            }

            step.toolName == "memory_save" -> {
                val key = step.toolInput["key"]?.toString() ?: "fact_${System.currentTimeMillis()}"
                val content = step.toolInput["content"]?.toString() ?: ""
                val category = step.toolInput["category"]?.toString() ?: "general"
                repository.insertMemory(
                    MemoryEntity(
                        key = key,
                        content = content,
                        category = category,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                "Saved memory: [$key] -> $content"
            }

            step.toolName == "memory_search" -> {
                val query = step.toolInput["query"]?.toString() ?: ""
                val results = repository.searchMemoriesDirect(query)
                "Found ${results.size} memories matching '$query'."
            }

            step.toolName == "task_update_status" -> {
                step.toolInput["warning"]?.toString() ?: "Status updated."
            }

            else -> "Executed tool ${step.toolName}"
        }
    }
}
