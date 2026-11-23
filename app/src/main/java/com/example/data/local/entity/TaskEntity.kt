package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val instruction: String,
    val status: String = "QUEUED", // QUEUED, RUNNING, WAITING_APPROVAL, COMPLETED, FAILED, CANCELLED
    val totalSteps: Int = 0,
    val currentStep: Int = 0,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val finalResult: String? = null,
    val errorMessage: String? = null,
    val pendingAction: String? = null,
    val requiresApproval: Boolean = false
)

@Entity(tableName = "task_steps")
data class TaskStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: String,
    val stepNumber: Int,
    val toolName: String,
    val toolInput: String,
    val toolOutput: String? = null,
    val status: String = "RUNNING", // RUNNING, SUCCESS, FAILED, PENDING_APPROVAL
    val timestamp: Long = System.currentTimeMillis(),
    val screenshotUrl: String? = null
)
