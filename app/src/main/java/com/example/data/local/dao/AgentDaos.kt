package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BrowserSessionEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.DriveFileEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.SettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE `key` LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE `key` LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchMemoriesDirect(query: String): List<MemoryEntity>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC LIMIT 20")
    suspend fun getRecentMemoriesDirect(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)
}

@Dao
interface BrowserDao {
    @Query("SELECT * FROM browser_sessions WHERE id = :id LIMIT 1")
    fun getSession(id: String = "active_session"): Flow<BrowserSessionEntity?>

    @Query("SELECT * FROM browser_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionDirect(id: String = "active_session"): BrowserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: BrowserSessionEntity)
}

@Dao
interface DriveFileDao {
    @Query("SELECT * FROM drive_files ORDER BY uploadedAt DESC")
    fun getAllFiles(): Flow<List<DriveFileEntity>>

    @Query("SELECT * FROM drive_files WHERE taskId = :taskId ORDER BY uploadedAt DESC")
    fun getFilesForTask(taskId: String): Flow<List<DriveFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: DriveFileEntity)

    @Update
    suspend fun updateFile(file: DriveFileEntity)

    @Delete
    suspend fun deleteFile(file: DriveFileEntity)

    @Query("DELETE FROM drive_files WHERE id = :id")
    suspend fun deleteFileById(id: String)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM agent_settings")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM agent_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<SettingEntity?>

    @Query("SELECT * FROM agent_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingDirect(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: SettingEntity)
}
