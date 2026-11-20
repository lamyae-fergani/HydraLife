package com.example.hydralife.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HydrationRepository(private val dao: HydrationDao) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getCurrentDateString(): String {
        return dateFormat.format(Date())
    }

    suspend fun addWaterEntry(amountMl: Int) {
        val now = System.currentTimeMillis()
        val dateStr = getCurrentDateString()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val session = when {
            hour < 12 -> "Morning"
            hour < 18 -> "Afternoon"
            else -> "Evening"
        }

        val entry = HydrationEntryEntity(
            timestamp = now,
            amountMl = amountMl,
            session = session,
            dateString = dateStr
        )
        dao.insertEntry(entry)
        updateGoalCompletion(dateStr)
    }

    suspend fun getTotalConsumedToday(): Int {
        val dateStr = getCurrentDateString()
        return dao.getTotalConsumedForDate(dateStr) ?: 0
    }

    suspend fun getConsumedForSessionToday(session: String): Int {
        val dateStr = getCurrentDateString()
        return dao.getConsumedForSession(dateStr, session) ?: 0
    }

    suspend fun getOrCreateGoalToday(): HydrationGoalEntity {
        val dateStr = getCurrentDateString()
        val existing = dao.getGoalForDate(dateStr)
        if (existing != null) {
            return existing
        } else {
            val newGoal = HydrationGoalEntity(dateString = dateStr, goalMl = 2000, completed = false)
            dao.insertGoal(newGoal)
            return newGoal
        }
    }

    suspend fun updateGoal(goalMl: Int) {
        val dateStr = getCurrentDateString()
        val consumed = getTotalConsumedToday()
        val completed = consumed >= goalMl
        val goalEntity = HydrationGoalEntity(dateString = dateStr, goalMl = goalMl, completed = completed)
        dao.insertGoal(goalEntity)
    }

    private suspend fun updateGoalCompletion(dateStr: String) {
        val goal = dao.getGoalForDate(dateStr) ?: HydrationGoalEntity(dateString = dateStr, goalMl = 2000, completed = false)
        val consumed = dao.getTotalConsumedForDate(dateStr) ?: 0
        val completed = consumed >= goal.goalMl
        if (goal.completed != completed) {
            dao.insertGoal(goal.copy(completed = completed))
        }
    }

    suspend fun getWeeklyConsumptionMap(): Map<String, Int> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val sinceTimestamp = calendar.timeInMillis

        val results = dao.getWeeklyConsumption(sinceTimestamp)
        return results.associate { it.dateString to it.total }
    }

    fun getAllGoalsHistory(): Flow<List<HydrationGoalEntity>> {
        return dao.getAllGoalsHistoryFlow()
    }

    // Conversation Management
    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return dao.getAllConversations()
    }

    suspend fun createNewConversation(title: String = "New Conversation"): Long {
        val conv = ConversationEntity(title = title, updatedAt = System.currentTimeMillis())
        return dao.insertConversation(conv)
    }

    suspend fun saveChatMessage(conversationId: Long, text: String, isUser: Boolean): Long {
        val msg = ChatMessageEntity(
            conversationId = conversationId,
            text = text,
            isUser = isUser,
            timestamp = System.currentTimeMillis()
        )
        val msgId = dao.insertMessage(msg)
        dao.updateConversationTimestamp(conversationId, System.currentTimeMillis())
        return msgId
    }

    suspend fun getMessagesForConversationList(conversationId: Long): List<ChatMessageEntity> {
        return dao.getMessagesListForConversation(conversationId)
    }

    suspend fun updateConversationTitle(conversationId: Long, title: String) {
        dao.updateConversationTitle(conversationId, title, System.currentTimeMillis())
    }

    suspend fun deleteConversation(conversationId: Long) {
        dao.deleteMessagesForConversation(conversationId)
        dao.deleteConversation(conversationId)
    }

    suspend fun updateConversationTitleFromFirstMessage(conversationId: Long, firstUserMessage: String) {
        val truncatedTitle = if (firstUserMessage.length > 30) firstUserMessage.take(30) + "..." else firstUserMessage
        dao.updateConversationTitle(conversationId, truncatedTitle, System.currentTimeMillis())
    }
}
