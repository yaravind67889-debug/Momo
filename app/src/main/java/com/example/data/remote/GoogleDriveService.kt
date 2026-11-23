package com.example.data.remote

import android.content.Context
import com.example.data.local.entity.DriveFileEntity
import com.example.data.repository.MomoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

class GoogleDriveService(
    private val context: Context,
    private val repository: MomoRepository
) {
    suspend fun isDriveConnected(): Boolean {
        return repository.getSettingDirect("google_drive_connected") == "true"
    }

    suspend fun connectDrive(accountEmail: String): Boolean = withContext(Dispatchers.IO) {
        delay(600) // Simulated OAuth handshake
        repository.setSetting("google_drive_connected", "true")
        repository.setSetting("google_drive_account", accountEmail)
        repository.setSetting("google_drive_folder", "Momo AI / Automations")

        // Populate initial demo artifacts so the user immediately sees working files
        if (repository.getSettingDirect("drive_seeded") != "true") {
            repository.setSetting("drive_seeded", "true")
            repository.insertDriveFile(
                DriveFileEntity(
                    id = UUID.randomUUID().toString(),
                    fileName = "momo_welcome_guide.pdf",
                    fileType = "application/pdf",
                    fileSize = 1024 * 342,
                    driveFileId = "drive_doc_001",
                    taskId = null,
                    uploadedAt = System.currentTimeMillis() - 86400000,
                    downloadUrl = "https://drive.google.com/file/d/momo_welcome_guide",
                    isSynced = true
                )
            )
            repository.insertDriveFile(
                DriveFileEntity(
                    id = UUID.randomUUID().toString(),
                    fileName = "browser_research_snapshot.png",
                    fileType = "image/png",
                    fileSize = 1024 * 820,
                    driveFileId = "drive_img_002",
                    taskId = null,
                    uploadedAt = System.currentTimeMillis() - 3600000,
                    downloadUrl = "https://drive.google.com/file/d/browser_snapshot",
                    isSynced = true
                )
            )
        }
        true
    }

    suspend fun disconnectDrive() = withContext(Dispatchers.IO) {
        repository.setSetting("google_drive_connected", "false")
        repository.setSetting("google_drive_account", "")
    }

    suspend fun uploadTaskArtifact(
        fileName: String,
        fileType: String,
        fileSizeBytes: Long,
        taskId: String?
    ): DriveFileEntity = withContext(Dispatchers.IO) {
        val fileId = UUID.randomUUID().toString()
        val isConnected = isDriveConnected()
        val entity = DriveFileEntity(
            id = fileId,
            fileName = fileName,
            fileType = fileType,
            fileSize = fileSizeBytes,
            driveFileId = if (isConnected) "drive_${UUID.randomUUID().toString().take(8)}" else null,
            taskId = taskId,
            uploadedAt = System.currentTimeMillis(),
            downloadUrl = if (isConnected) "https://drive.google.com/file/d/$fileId" else null,
            isSynced = isConnected
        )
        repository.insertDriveFile(entity)
        entity
    }
}
