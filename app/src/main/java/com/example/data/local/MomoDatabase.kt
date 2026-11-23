package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BrowserDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.DriveFileDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.SettingDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.BrowserSessionEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.DriveFileEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskStepEntity

@Database(
    entities = [
        TaskEntity::class,
        TaskStepEntity::class,
        ConversationEntity::class,
        MemoryEntity::class,
        BrowserSessionEntity::class,
        DriveFileEntity::class,
        SettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MomoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun browserDao(): BrowserDao
    abstract fun driveFileDao(): DriveFileDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: MomoDatabase? = null

        fun getDatabase(context: Context): MomoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MomoDatabase::class.java,
                    "momo_ai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
