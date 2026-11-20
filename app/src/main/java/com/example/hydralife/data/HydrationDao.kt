package com.example.hydralife.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HydrationEntryEntity)

    @Query("SELECT SUM(amountMl) FROM hydration_entries WHERE dateString = :dateStr")
    suspend fun getTotalConsumedForDate(dateStr: String): Int?

    @Query("SELECT SUM(amountMl) FROM hydration_entries WHERE dateString = :dateStr AND session = :session")
    suspend fun getConsumedForSession(dateStr: String, session: String): Int?

    @Query("SELECT * FROM hydration_entries WHERE dateString = :dateStr ORDER BY timestamp DESC")
    fun getEntriesForDateFlow(dateStr: String): Flow<List<HydrationEntryEntity>>

    @Query("SELECT dateString, SUM(amountMl) as total FROM hydration_entries WHERE timestamp >= :sinceTimestamp GROUP BY dateString")
    suspend fun getWeeklyConsumption(sinceTimestamp: Long): List<DailyConsumptionResult>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: HydrationGoalEntity)

    @Query("SELECT * FROM hydration_goals WHERE dateString = :dateStr")
    suspend fun getGoalForDate(dateStr: String): HydrationGoalEntity?

    @Query("SELECT * FROM hydration_goals ORDER BY dateString DESC")
    fun getAllGoalsHistoryFlow(): Flow<List<HydrationGoalEntity>>

    // Conversations & Chat Messages
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Long)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateConversationTitle(conversationId: Long, title: String, updatedAt: Long)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateConversationTimestamp(conversationId: Long, updatedAt: Long)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesListForConversation(conversationId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("SELECT text FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageText(conversationId: Long): String?
}

data class DailyConsumptionResult(
    val dateString: String,
    val total: Int
)
