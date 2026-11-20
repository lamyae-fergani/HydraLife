package com.example.hydralife.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HydrationEntryEntity::class, HydrationGoalEntity::class, ConversationEntity::class, ChatMessageEntity::class], version = 2, exportSchema = false)
abstract class HydrationDatabase : RoomDatabase() {

    abstract fun hydrationDao(): HydrationDao

    companion object {
        @Volatile
        private var INSTANCE: HydrationDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `conversations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `conversationId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `isUser` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): HydrationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HydrationDatabase::class.java,
                    "hydralife_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
