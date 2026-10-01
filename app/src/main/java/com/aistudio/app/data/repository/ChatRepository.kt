package com.aistudio.app.data.repository

import android.content.Context
import com.aistudio.app.data.local.AppDatabase
import com.aistudio.app.data.local.MessageEntity
import com.aistudio.app.data.local.PreferencesManager
import com.aistudio.app.data.local.SessionEntity
import com.aistudio.app.data.model.Attachment
import com.aistudio.app.data.model.ChatMessage
import com.aistudio.app.data.model.ChatSession
import com.aistudio.app.data.model.MessageSender
import com.aistudio.app.data.remote.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatRepository(
    context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val database = AppDatabase.getInstance(context)
    private val chatDao = database.chatDao()

    private val geminiService = GeminiService(
        getApiKey = { preferencesManager.apiKey },
        getModelName = { preferencesManager.modelName },
        getSystemInstruction = { preferencesManager.systemInstruction }
    )

    fun getAllSessions(): Flow<List<ChatSession>> {
        return chatDao.getAllSessionsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSessionFlow(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun createNewSession(category: String = "General"): ChatSession = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val session = ChatSession(
            id = "session-$now",
            title = "New Chat",
            category = category,
            tags = emptyList(),
            isPinned = false,
            createdAt = now,
            updatedAt = now
        )
        chatDao.insertSession(SessionEntity.fromDomain(session))
        preferencesManager.activeSessionId = session.id
        return@withContext session
    }

    suspend fun saveSession(session: ChatSession) = withContext(Dispatchers.IO) {
        chatDao.insertSession(SessionEntity.fromDomain(session))
    }

    suspend fun updateSessionTitleAndCategory(
        sessionId: String,
        title: String,
        category: String,
        tags: List<String>
    ) = withContext(Dispatchers.IO) {
        val existing = chatDao.getSessionById(sessionId) ?: return@withContext
        val updated = existing.toDomain().copy(
            title = title.trim(),
            category = category,
            tags = tags,
            updatedAt = System.currentTimeMillis()
        )
        chatDao.updateSession(SessionEntity.fromDomain(updated))
    }

    suspend fun togglePinSession(sessionId: String) = withContext(Dispatchers.IO) {
        val existing = chatDao.getSessionById(sessionId) ?: return@withContext
        val updated = existing.toDomain().copy(
            isPinned = !existing.isPinned,
            updatedAt = System.currentTimeMillis()
        )
        chatDao.updateSession(SessionEntity.fromDomain(updated))
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteSessionById(sessionId)
    }

    suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(MessageEntity.fromDomain(message))
        // Auto update session title based on first user prompt if still default
        val existingSession = chatDao.getSessionById(message.sessionId)
        if (existingSession != null) {
            var updatedTitle = existingSession.title
            if (updatedTitle == "New Chat" && message.sender == MessageSender.YOU) {
                updatedTitle = message.message.take(32) + if (message.message.length > 32) "..." else ""
            }
            chatDao.updateSession(
                existingSession.copy(
                    title = updatedTitle,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.updateMessage(MessageEntity.fromDomain(message))
    }

    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessageById(messageId)
    }

    suspend fun clearSession(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.clearSessionMessages(sessionId)
    }

    suspend fun updateReaction(messageId: String, reaction: String?) = withContext(Dispatchers.IO) {
        chatDao.updateReaction(messageId, reaction)
    }

    fun streamAiResponse(
        history: List<ChatMessage>,
        attachments: List<Attachment>,
        prompt: String
    ): Flow<String> {
        return geminiService.streamChatResponse(history, attachments, prompt)
    }

    suspend fun generateFollowUpSuggestions(history: List<ChatMessage>): List<String> {
        return geminiService.generateFollowUpSuggestions(history)
    }

    suspend fun exportTranscript(sessionId: String, format: String): String = withContext(Dispatchers.IO) {
        val messages = chatDao.getMessagesForSession(sessionId).map { it.toDomain() }
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        if (format.equals("md", ignoreCase = true)) {
            val sb = StringBuilder()
            sb.append("# AI Studio Chat Transcript\n")
            sb.append("Exported on: $dateStr\n\n---\n\n")
            messages.forEach { m ->
                if (m.sender == MessageSender.APP) {
                    sb.append("> **System**: ${m.message}\n\n")
                } else {
                    sb.append("### ${m.sender.name} (${m.timestamp})\n\n${m.message}\n\n")
                }
            }
            return@withContext sb.toString()
        } else {
            val sb = StringBuilder()
            sb.append("AI STUDIO CHAT TRANSCRIPT\n")
            sb.append("Exported on: $dateStr\n")
            sb.append("====================================\n\n")
            messages.forEach { m ->
                sb.append("[${m.timestamp}] ${m.sender.name}: ${m.message}\n\n")
            }
            return@withContext sb.toString()
        }
    }
}
