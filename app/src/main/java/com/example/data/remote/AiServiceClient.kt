package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PlannedStep(
    val toolName: String,
    val toolInput: Map<String, Any>,
    val explanation: String,
    val isSensitive: Boolean = false
)

data class PlanResult(
    val thought: String,
    val steps: List<PlannedStep>,
    val error: String? = null
)

class AiServiceClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    private val TAG = "MomoAIService"

    suspend fun planTask(
        instruction: String,
        modelProvider: String, // "Gemini", "OpenAI Compatible", "Autonomous Local"
        apiKey: String?,
        modelName: String,
        customEndpoint: String?,
        memories: List<String>
    ): PlanResult = withContext(Dispatchers.IO) {
        if (!apiKey.isNullOrBlank() && modelProvider.equals("Gemini", ignoreCase = true)) {
            val geminiResult = callGeminiApi(instruction, apiKey, modelName, memories)
            if (geminiResult != null) return@withContext geminiResult
        } else if (!apiKey.isNullOrBlank() && !customEndpoint.isNullOrBlank()) {
            val openAiResult = callOpenAiCompatibleApi(instruction, apiKey, modelName, customEndpoint, memories)
            if (openAiResult != null) return@withContext openAiResult
        }

        // Autonomous Local Rule-based planner (reliable fallback without requiring immediate external key)
        return@withContext generateAutonomousPlan(instruction, memories)
    }

    private fun callGeminiApi(
        instruction: String,
        apiKey: String,
        modelName: String,
        memories: List<String>
    ): PlanResult? {
        try {
            val cleanModel = if (modelName.isBlank()) "gemini-1.5-flash" else modelName
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

            val prompt = buildSystemPrompt(instruction, memories)
            val json = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: "{}"
                    val respJson = JSONObject(respStr)
                    val candidates = respJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val text = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        return parseJsonPlan(text)
                    }
                } else {
                    Log.w(TAG, "Gemini call failed HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call error: ${e.message}")
        }
        return null
    }

    private fun callOpenAiCompatibleApi(
        instruction: String,
        apiKey: String,
        modelName: String,
        endpoint: String,
        memories: List<String>
    ): PlanResult? {
        try {
            val url = "${endpoint.trimEnd('/')}/chat/completions"
            val prompt = buildSystemPrompt(instruction, memories)

            val json = JSONObject().apply {
                put("model", modelName.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.2)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: "{}"
                    val respJson = JSONObject(respStr)
                    val choices = respJson.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                        return parseJsonPlan(content)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI API call error: ${e.message}")
        }
        return null
    }

    private fun buildSystemPrompt(instruction: String, memories: List<String>): String {
        return """
            You are Momo AI, a personal cloud agent.
            Plan browser actions and file operations for the task: "$instruction".
            Known memories: ${memories.joinToString("; ")}
            Return STRICT JSON with keys:
            "thought": explanation,
            "steps": [
               {
                 "toolName": "browser_open" | "browser_click" | "browser_type" | "browser_scroll" | "browser_screenshot" | "browser_read_page" | "drive_upload_file" | "memory_save",
                 "toolInput": { key: value },
                 "explanation": "why",
                 "isSensitive": boolean (true if deletes, makes payment, submits critical form, or sends message)
               }
            ]
        """.trimIndent()
    }

    private fun parseJsonPlan(text: String): PlanResult {
        try {
            val clean = text.replace("```json", "").replace("```", "").trim()
            val obj = JSONObject(clean)
            val thought = obj.optString("thought", "Momo is planning steps...")
            val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
            val list = mutableListOf<PlannedStep>()

            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.getJSONObject(i)
                val toolName = stepObj.getString("toolName")
                val explanation = stepObj.optString("explanation", "")
                val isSensitive = stepObj.optBoolean("isSensitive", false)

                val inputObj = stepObj.optJSONObject("toolInput") ?: JSONObject()
                val map = mutableMapOf<String, Any>()
                val keys = inputObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = inputObj.get(k)
                }

                list.add(PlannedStep(toolName, map, explanation, isSensitive))
            }
            return PlanResult(thought, list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI JSON plan: ${e.message}")
            return generateAutonomousPlan(text, emptyList())
        }
    }

    fun generateAutonomousPlan(instruction: String, memories: List<String>): PlanResult {
        val lower = instruction.lowercase()
        val steps = mutableListOf<PlannedStep>()
        val thought = "Momo understood: '$instruction'. Planning safe automated browser & cloud execution."

        when {
            lower.contains("google") || lower.contains("search") || lower.contains("look up") || lower.contains("find") -> {
                val query = instruction.replace(Regex("(?i)(search for|search|look up|google|find)"), "").trim()
                steps.add(PlannedStep(
                    toolName = "browser_open",
                    toolInput = mapOf("url" to "https://www.google.com/search?q=${query.replace(" ", "+")}"),
                    explanation = "Navigating to search engine for query '$query'"
                ))
                steps.add(PlannedStep(
                    toolName = "browser_read_page",
                    toolInput = mapOf("target" to "search_results"),
                    explanation = "Reading and summarizing top search results"
                ))
                steps.add(PlannedStep(
                    toolName = "browser_screenshot",
                    toolInput = mapOf("description" to "Search results view"),
                    explanation = "Capturing page preview for verification"
                ))
                steps.add(PlannedStep(
                    toolName = "memory_save",
                    toolInput = mapOf("key" to "search_$query", "content" to "Researched $query successfully", "category" to "task_learning"),
                    explanation = "Recording task learning in Momo persistent memory"
                ))
            }

            lower.contains("buy") || lower.contains("order") || lower.contains("purchase") || lower.contains("payment") -> {
                steps.add(PlannedStep(
                    toolName = "browser_open",
                    toolInput = mapOf("url" to "https://store.example.com/checkout"),
                    explanation = "Opening merchant checkout page"
                ))
                steps.add(PlannedStep(
                    toolName = "browser_click",
                    toolInput = mapOf("selector" to "#confirm-purchase-btn", "amount" to "$49.00"),
                    explanation = "CRITICAL: Momo must ask owner approval before financial transaction",
                    isSensitive = true
                ))
            }

            lower.contains("drive") || lower.contains("upload") || lower.contains("backup") || lower.contains("save file") -> {
                steps.add(PlannedStep(
                    toolName = "browser_screenshot",
                    toolInput = mapOf("format" to "PNG", "quality" to 100),
                    explanation = "Generating visual artifact for Google Drive backup"
                ))
                steps.add(PlannedStep(
                    toolName = "drive_upload_file",
                    toolInput = mapOf("fileName" to "momo_task_output_${System.currentTimeMillis()}.pdf", "fileType" to "document"),
                    explanation = "Syncing output document to owner's Google Drive"
                ))
                steps.add(PlannedStep(
                    toolName = "memory_save",
                    toolInput = mapOf("key" to "drive_sync", "content" to "Uploaded task report to Google Drive", "category" to "fact"),
                    explanation = "Updating agent persistent memory with cloud sync event"
                ))
            }

            lower.contains("email") || lower.contains("send message") -> {
                steps.add(PlannedStep(
                    toolName = "browser_open",
                    toolInput = mapOf("url" to "https://mail.google.com"),
                    explanation = "Opening email portal"
                ))
                steps.add(PlannedStep(
                    toolName = "browser_type",
                    toolInput = mapOf("selector" to "#recipient", "text" to "contact@example.com"),
                    explanation = "Composing recipient and subject"
                ))
                steps.add(PlannedStep(
                    toolName = "browser_click",
                    toolInput = mapOf("selector" to "#send-btn", "target" to "Send Email Message"),
                    explanation = "CRITICAL: Sending external message requires owner approval",
                    isSensitive = true
                ))
            }

            lower.contains("delete") || lower.contains("remove") -> {
                steps.add(PlannedStep(
                    toolName = "drive_download_file",
                    toolInput = mapOf("confirm" to "check_metadata"),
                    explanation = "Verifying target item before removal"
                ))
                steps.add(PlannedStep(
                    toolName = "task_update_status",
                    toolInput = mapOf("action" to "delete_resource", "warning" to "Irreversible file deletion"),
                    explanation = "CRITICAL: Permanent deletion requires explicit owner confirmation",
                    isSensitive = true
                ))
            }

            else -> {
                val targetUrl = if (lower.startsWith("http")) instruction.split(" ")[0] else "https://news.ycombinator.com"
                steps.add(PlannedStep(
                    toolName = "browser_open",
                    toolInput = mapOf("url" to targetUrl),
                    explanation = "Navigating to $targetUrl"
                ))
                steps.add(PlannedStep(
                    toolName = "browser_read_page",
                    toolInput = mapOf("selector" to "main"),
                    explanation = "Parsing DOM text and interactive elements"
                ))
                steps.add(PlannedStep(
                    toolName = "browser_screenshot",
                    toolInput = mapOf("target" to "full_page"),
                    explanation = "Capturing page preview for browser feed"
                ))
                steps.add(PlannedStep(
                    toolName = "memory_save",
                    toolInput = mapOf("key" to "task_summary", "content" to "Analyzed: $instruction", "category" to "task_learning"),
                    explanation = "Saving outcome to memory"
                ))
            }
        }

        return PlanResult(thought, steps)
    }
}
