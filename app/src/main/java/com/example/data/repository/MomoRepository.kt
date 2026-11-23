package com.example.data.repository

import com.example.data.local.MomoDatabase
import com.example.data.local.entity.BrowserSessionEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.DriveFileEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskStepEntity
import kotlinx.coroutines.flow.Flow

class MomoRepository(private val database: MomoDatabase) {

    // Tasks
    val allTasks: Flow<List<TaskEntity>> = database.taskDao().getAllTasks()
    val activeTasks: Flow<List<TaskEntity>> = database.taskDao().getActiveTasks()
    val recentSteps: Flow<List<TaskStepEntity>> = database.taskDao().getRecentSteps()

    fun getTask(id: String): Flow<TaskEntity?> = database.taskDao().getTaskById(id)
    suspend fun getTaskDirect(id: String): TaskEntity? = database.taskDao().getTaskByIdDirect(id)
    fun getStepsForTask(taskId: String): Flow<List<TaskStepEntity>> = database.taskDao().getStepsForTask(taskId)

    suspend fun insertTask(task: TaskEntity) = database.taskDao().insertTask(task)
    suspend fun updateTask(task: TaskEntity) = database.taskDao().updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = database.taskDao().deleteTask(task)
    suspend fun deleteTaskById(id: String) {
        database.taskDao().deleteStepsForTask(id)
        database.taskDao().deleteTaskById(id)
    }

    suspend fun insertStep(step: TaskStepEntity): Long = database.taskDao().insertStep(step)
    suspend fun updateStep(step: TaskStepEntity) = database.taskDao().updateStep(step)

    // Conversations
    val allConversations: Flow<List<ConversationEntity>> = database.conversationDao().getAllConversations()
    suspend fun insertConversation(conv: ConversationEntity): Long = database.conversationDao().insertConversation(conv)
    suspend fun clearConversations() = database.conversationDao().clearAllConversations()

    // Memories
    val allMemories: Flow<List<MemoryEntity>> = database.memoryDao().getAllMemories()
    fun searchMemories(query: String): Flow<List<MemoryEntity>> = database.memoryDao().searchMemories(query)
    suspend fun searchMemoriesDirect(query: String): List<MemoryEntity> = database.memoryDao().searchMemoriesDirect(query)
    suspend fun getRecentMemoriesDirect(): List<MemoryEntity> = database.memoryDao().getRecentMemoriesDirect()
    suspend fun insertMemory(memory: MemoryEntity): Long = database.memoryDao().insertMemory(memory)
    suspend fun updateMemory(memory: MemoryEntity) = database.memoryDao().updateMemory(memory)
    suspend fun deleteMemory(memory: MemoryEntity) = database.memoryDao().deleteMemory(memory)
    suspend fun deleteMemoryById(id: Long) = database.memoryDao().deleteMemoryById(id)

    // Browser Sessions
    val browserSession: Flow<BrowserSessionEntity?> = database.browserDao().getSession()
    suspend fun getBrowserSessionDirect(): BrowserSessionEntity? = database.browserDao().getSessionDirect()
    suspend fun upsertBrowserSession(session: BrowserSessionEntity) = database.browserDao().upsertSession(session)

    // Drive Files
    val allDriveFiles: Flow<List<DriveFileEntity>> = database.driveFileDao().getAllFiles()
    suspend fun insertDriveFile(file: DriveFileEntity) = database.driveFileDao().insertFile(file)
    suspend fun updateDriveFile(file: DriveFileEntity) = database.driveFileDao().updateFile(file)
    suspend fun deleteDriveFile(file: DriveFileEntity) = database.driveFileDao().deleteFile(file)
    suspend fun deleteDriveFileById(id: String) = database.driveFileDao().deleteFileById(id)

    // Settings
    val allSettings: Flow<List<SettingEntity>> = database.settingDao().getAllSettings()
    fun getSetting(key: String): Flow<SettingEntity?> = database.settingDao().getSetting(key)
    suspend fun getSettingDirect(key: String): String? = database.settingDao().getSettingDirect(key)?.value
    suspend fun setSetting(key: String, value: String) = database.settingDao().upsertSetting(SettingEntity(key, value))
}
