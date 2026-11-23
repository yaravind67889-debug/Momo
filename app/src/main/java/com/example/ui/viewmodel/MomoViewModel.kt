package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.MomoDatabase
import com.example.data.local.entity.BrowserSessionEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.DriveFileEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskStepEntity
import com.example.data.remote.AiServiceClient
import com.example.data.remote.GoogleDriveService
import com.example.data.remote.HealthStatus
import com.example.data.remote.HuggingFaceBrowserClient
import com.example.data.repository.MomoRepository
import com.example.domain.agent.AgentOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MomoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MomoDatabase.getDatabase(application)
    val repository = MomoRepository(database)
    val aiClient = AiServiceClient()
    val browserClient = HuggingFaceBrowserClient()
    val driveService = GoogleDriveService(application, repository)

    val orchestrator = AgentOrchestrator(
        repository = repository,
        aiClient = aiClient,
        browserClient = browserClient,
        driveService = driveService,
        scope = viewModelScope
    )

    // Reactive UI States
    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<TaskEntity>> = repository.activeTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSteps: StateFlow<List<TaskStepEntity>> = repository.recentSteps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDriveFiles: StateFlow<List<DriveFileEntity>> = repository.allDriveFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val browserSession: StateFlow<BrowserSessionEntity?> = repository.browserSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current pending approval task
    val pendingApprovalTask: StateFlow<TaskEntity?> = activeTasks
        .map { tasks -> tasks.firstOrNull { it.status == "WAITING_APPROVAL" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Health Status
    private val _hfHealthStatus = MutableStateFlow(
        HealthStatus(isOnline = true, spaceState = "LOCAL_SANDBOX", message = "Ready in Momo Sandbox")
    )
    val hfHealthStatus: StateFlow<HealthStatus> = _hfHealthStatus.asStateFlow()

    // Settings States
    val aiProvider = repository.getSetting("ai_provider")
        .map { it?.value ?: "Autonomous Local" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Autonomous Local")

    val aiApiKey = repository.getSetting("ai_api_key")
        .map { it?.value ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val aiModelName = repository.getSetting("ai_model_name")
        .map { it?.value ?: "gemini-1.5-flash" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-1.5-flash")

    val aiCustomEndpoint = repository.getSetting("ai_custom_endpoint")
        .map { it?.value ?: "https://api.openai.com/v1" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://api.openai.com/v1")

    val hfSpaceUrl = repository.getSetting("hf_space_url")
        .map { it?.value ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val hfSpaceToken = repository.getSetting("hf_space_token")
        .map { it?.value ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isDriveConnected = repository.getSetting("google_drive_connected")
        .map { it?.value == "true" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val driveAccount = repository.getSetting("google_drive_account")
        .map { it?.value ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        // Seed default initial welcome message and default settings if empty
        viewModelScope.launch(Dispatchers.IO) {
            val seeded = repository.getSettingDirect("initialized_seed")
            if (seeded != "true") {
                repository.setSetting("initialized_seed", "true")
                repository.insertConversation(
                    ConversationEntity(
                        sender = "MOMO",
                        message = "Hi! I'm Momo, your personal AI agent. I can browse the web, plan tasks, remember details, and backup files to your Google Drive. What can I do for you today?",
                        timestamp = System.currentTimeMillis()
                    )
                )
                // Seed initial memories
                repository.insertMemory(
                    MemoryEntity(
                        key = "preferred_browser",
                        content = "Chromium Playwright on Hugging Face Spaces",
                        category = "preference"
                    )
                )
                repository.insertMemory(
                    MemoryEntity(
                        key = "owner_name",
                        content = "Primary Owner",
                        category = "fact"
                    )
                )
            }
            checkHfHealth()
        }
    }

    fun submitTask(instruction: String) {
        if (instruction.isNotBlank()) {
            orchestrator.submitTask(instruction.trim())
        }
    }

    fun cancelTask(taskId: String) {
        orchestrator.cancelTask(taskId)
    }

    fun retryTask(taskId: String) {
        orchestrator.retryTask(taskId)
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTaskById(taskId)
        }
    }

    fun approveAction(taskId: String) {
        orchestrator.approveAction(taskId)
    }

    fun rejectAction(taskId: String, reason: String = "Rejected by owner") {
        orchestrator.rejectAction(taskId, reason)
    }

    fun checkHfHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            val url = repository.getSettingDirect("hf_space_url")
            val token = repository.getSettingDirect("hf_space_token")
            val status = browserClient.checkHealth(url, token)
            _hfHealthStatus.value = status
        }
    }

    fun addMemory(key: String, content: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMemory(
                MemoryEntity(
                    key = key.trim(),
                    content = content.trim(),
                    category = category,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemoryById(id)
        }
    }

    fun connectDrive(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            driveService.connectDrive(email)
        }
    }

    fun disconnectDrive() {
        viewModelScope.launch(Dispatchers.IO) {
            driveService.disconnectDrive()
        }
    }

    fun deleteFile(fileId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDriveFileById(fileId)
        }
    }

    fun saveAiSettings(provider: String, key: String, model: String, endpoint: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("ai_provider", provider)
            repository.setSetting("ai_api_key", key)
            repository.setSetting("ai_model_name", model)
            repository.setSetting("ai_custom_endpoint", endpoint)
        }
    }

    fun saveHfSettings(url: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("hf_space_url", url)
            repository.setSetting("hf_space_token", token)
            checkHfHealth()
        }
    }

    fun manualBrowserNavigate(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val hfUrl = repository.getSettingDirect("hf_space_url")
            val token = repository.getSettingDirect("hf_space_token")
            val result = browserClient.executeAction(hfUrl, token, "browser_open", mapOf("url" to url))
            if (result is com.example.data.remote.BrowserResult.Success) {
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
            }
        }
    }
}
