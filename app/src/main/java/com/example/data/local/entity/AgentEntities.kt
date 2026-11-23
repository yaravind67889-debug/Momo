package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: String? = null,
    val sender: String, // USER, MOMO, SYSTEM
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hasAction: Boolean = false,
    val actionPayload: String? = null
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val content: String,
    val category: String = "general", // preference, credential_hint, fact, task_learning, general
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "browser_sessions")
data class BrowserSessionEntity(
    @PrimaryKey
    val id: String = "active_session",
    val currentUrl: String = "https://google.com",
    val pageTitle: String = "New Tab",
    val lastScreenshot: String? = null,
    val status: String = "IDLE", // IDLE, NAVIGATING, INTERACTING, ERROR
    val domSnippet: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "drive_files")
data class DriveFileEntity(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val driveFileId: String? = null,
    val taskId: String? = null,
    val uploadedAt: Long = System.currentTimeMillis(),
    val downloadUrl: String? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "agent_settings")
data class SettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
