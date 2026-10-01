package com.aistudio.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.app.data.local.PreferencesManager
import com.aistudio.app.data.model.Attachment
import com.aistudio.app.data.model.ChatMessage
import com.aistudio.app.data.model.ChatSession
import com.aistudio.app.data.model.MessageSender
import com.aistudio.app.data.model.PromptTemplate
import com.aistudio.app.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingMessageId: String? = null,
    val streamingText: String = "",
    val searchQuery: String = "",
    val categoryFilter: String = "All",
    val isDarkMode: Boolean? = null,
    val autoScroll: Boolean = true,
    val templates: List<PromptTemplate> = emptyList(),
    val apiKey: String = "",
    val modelName: String = "gemini-3.5-flash",
    val systemInstruction: String = "",
    val stagedAttachments: List<Attachment> = emptyList()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val repository = ChatRepository(application, preferencesManager)

    private val _uiState = MutableStateFlow(
        ChatUiState(
            apiKey = preferencesManager.apiKey,
            modelName = preferencesManager.modelName,
            systemInstruction = preferencesManager.systemInstruction,
            isDarkMode = preferencesManager.isDarkMode,
            autoScroll = preferencesManager.autoScroll,
            templates = preferencesManager.getTemplates()
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeStreamJob: Job? = null
    private var messageObserveJob: Job? = null

    init {
        // Collect all sessions
        viewModelScope.launch {
            repository.getAllSessions().collect { sessionList ->
                val activeId = _uiState.value.currentSessionId.ifBlank {
                    preferencesManager.activeSessionId.ifBlank {
                        sessionList.firstOrNull()?.id ?: ""
                    }
                }

                if (sessionList.isEmpty()) {
                    // Create default initial session
                    val newSession = repository.createNewSession()
                    _uiState.update { it.copy(sessions = listOf(newSession), currentSessionId = newSession.id) }
                    observeMessagesForSession(newSession.id)
                } else {
                    val validActiveId = if (sessionList.any { it.id == activeId }) activeId else sessionList.first().id
                    _uiState.update { it.copy(sessions = sessionList, currentSessionId = validActiveId) }
                    observeMessagesForSession(validActiveId)
                }
            }
        }
    }

    private fun observeMessagesForSession(sessionId: String) {
        messageObserveJob?.cancel()
        messageObserveJob = viewModelScope.launch {
            repository.getMessagesForSession(sessionId).collect { messageList ->
                _uiState.update { it.copy(messages = messageList) }

                // Insert welcome greeting if session has no messages
                if (messageList.isEmpty()) {
                    val welcomeMessages = listOf(
                        "Hello! I am your Gemini AI assistant. Send me a message, code snippet, or photo to get started!",
                        "Hi there! How can I help you today? Feel free to ask questions or attach files.",
                        "Welcome! I'm ready to assist you with writing, coding, summaries, and creative tasks."
                    )
                    val randomGreeting = welcomeMessages.random()
                    val welcomeMsg = ChatMessage(
                        id = "welcome-${System.currentTimeMillis()}",
                        sessionId = sessionId,
                        sender = MessageSender.AI,
                        message = randomGreeting,
                        timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    )
                    repository.saveMessage(welcomeMsg)
                }
            }
        }
    }

    fun selectSession(sessionId: String) {
        if (_uiState.value.currentSessionId == sessionId) return
        activeStreamJob?.cancel()
        preferencesManager.activeSessionId = sessionId
        _uiState.update {
            it.copy(
                currentSessionId = sessionId,
                isLoading = false,
                isStreaming = false,
                streamingMessageId = null,
                streamingText = "",
                stagedAttachments = emptyList()
            )
        }
        observeMessagesForSession(sessionId)
    }

    fun createNewSession(category: String = "General") {
        viewModelScope.launch {
            val session = repository.createNewSession(category)
            selectSession(session.id)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun togglePinSession(sessionId: String) {
        viewModelScope.launch {
            repository.togglePinSession(sessionId)
        }
    }

    fun renameSession(sessionId: String, title: String, category: String, tags: List<String>) {
        viewModelScope.launch {
            repository.updateSessionTitleAndCategory(sessionId, title, category, tags)
        }
    }

    fun addAttachment(attachment: Attachment) {
        _uiState.update { it.copy(stagedAttachments = it.stagedAttachments + attachment) }
    }

    fun removeAttachment(index: Int) {
        _uiState.update {
            val updated = it.stagedAttachments.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            it.copy(stagedAttachments = updated)
        }
    }

    fun clearAttachments() {
        _uiState.update { it.copy(stagedAttachments = emptyList()) }
    }

    fun sendMessage(userPrompt: String) {
        val currentSessionId = _uiState.value.currentSessionId
        if (currentSessionId.isBlank()) return

        val attachmentsToSend = _uiState.value.stagedAttachments
        var finalPrompt = userPrompt.trim()
        if (finalPrompt.isBlank() && attachmentsToSend.isNotEmpty()) {
            val ocrParts = attachmentsToSend.mapNotNull { it.extractedText }.filter { it.isNotBlank() }
            finalPrompt = if (ocrParts.isNotEmpty()) {
                ocrParts.joinToString("\n\n")
            } else {
                "Please analyze these attached files and provide insights."
            }
        }
        if (finalPrompt.isBlank()) return

        val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val userMessage = ChatMessage(
            id = "user-${System.currentTimeMillis()}",
            sessionId = currentSessionId,
            sender = MessageSender.YOU,
            message = finalPrompt,
            timestamp = timestamp,
            attachments = attachmentsToSend
        )

        // Clear staged attachments
        _uiState.update { it.copy(stagedAttachments = emptyList(), isLoading = true) }

        viewModelScope.launch {
            repository.saveMessage(userMessage)
            startAiStream(currentSessionId, userMessage, _uiState.value.messages + userMessage, attachmentsToSend)
        }
    }

    private fun startAiStream(
        sessionId: String,
        userMsg: ChatMessage,
        fullHistory: List<ChatMessage>,
        attachments: List<Attachment>
    ) {
        activeStreamJob?.cancel()
        val aiMessageId = "ai-${System.currentTimeMillis()}"
        val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        _uiState.update {
            it.copy(
                isLoading = true,
                isStreaming = true,
                streamingMessageId = aiMessageId,
                streamingText = ""
            )
        }

        activeStreamJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                repository.streamAiResponse(
                    history = fullHistory,
                    attachments = attachments,
                    prompt = userMsg.message
                ).collect { chunk ->
                    accumulated.append(chunk)
                    _uiState.update { it.copy(streamingText = accumulated.toString(), isLoading = false) }
                }

                val finalAiMessageText = if (accumulated.isNotBlank()) accumulated.toString() else "No response generated."
                val aiMessage = ChatMessage(
                    id = aiMessageId,
                    sessionId = sessionId,
                    sender = MessageSender.AI,
                    message = finalAiMessageText,
                    timestamp = timestamp
                )
                repository.saveMessage(aiMessage)

                // Generate quick follow-up suggestions in background
                val suggestions = repository.generateFollowUpSuggestions(fullHistory + aiMessage)
                if (suggestions.isNotEmpty()) {
                    repository.updateMessage(aiMessage.copy(suggestions = suggestions))
                }

            } catch (e: Exception) {
                if (accumulated.isNotEmpty()) {
                    val partialAiMessage = ChatMessage(
                        id = aiMessageId,
                        sessionId = sessionId,
                        sender = MessageSender.AI,
                        message = accumulated.toString(),
                        timestamp = timestamp
                    )
                    repository.saveMessage(partialAiMessage)
                } else {
                    val errorMsg = ChatMessage(
                        id = "err-${System.currentTimeMillis()}",
                        sessionId = sessionId,
                        sender = MessageSender.APP,
                        message = e.message ?: "Failed to connect to AI Studio.",
                        timestamp = timestamp
                    )
                    repository.saveMessage(errorMsg)
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isStreaming = false,
                        streamingMessageId = null,
                        streamingText = ""
                    )
                }
            }
        }
    }

    fun stopStreaming() {
        activeStreamJob?.cancel()
        activeStreamJob = null
        val curId = _uiState.value.streamingMessageId
        val curText = _uiState.value.streamingText
        val sessionId = _uiState.value.currentSessionId
        if (curId != null && curText.isNotBlank()) {
            viewModelScope.launch {
                val stoppedMsg = ChatMessage(
                    id = curId,
                    sessionId = sessionId,
                    sender = MessageSender.AI,
                    message = curText,
                    timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                )
                repository.saveMessage(stoppedMsg)
            }
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                isStreaming = false,
                streamingMessageId = null,
                streamingText = ""
            )
        }
    }

    fun retryMessage(messageId: String) {
        val currentList = _uiState.value.messages
        val index = currentList.indexOfFirst { it.id == messageId }
        if (index == -1) return

        val msg = currentList[index]
        if (msg.sender == MessageSender.YOU) {
            editMessage(messageId, msg.message)
            return
        }

        val lastUserMsg = currentList.take(index).lastOrNull { it.sender == MessageSender.YOU } ?: return
        val rollbackHistory = currentList.take(index)
        viewModelScope.launch {
            // Delete messages from index forward in DB
            for (i in index until currentList.size) {
                repository.deleteMessage(currentList[i].id)
            }
            startAiStream(msg.sessionId, lastUserMsg, rollbackHistory, lastUserMsg.attachments)
        }
    }

    fun editMessage(messageId: String, newText: String) {
        val currentList = _uiState.value.messages
        val index = currentList.indexOfFirst { it.id == messageId }
        if (index == -1 || newText.isBlank()) return

        val originalMsg = currentList[index]
        val editedMsg = originalMsg.copy(
            message = newText.trim(),
            timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )

        viewModelScope.launch {
            // Remove following turns
            for (i in index until currentList.size) {
                repository.deleteMessage(currentList[i].id)
            }
            repository.saveMessage(editedMsg)
            startAiStream(editedMsg.sessionId, editedMsg, currentList.take(index) + editedMsg, editedMsg.attachments)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun clearChat() {
        val sessionId = _uiState.value.currentSessionId
        if (sessionId.isBlank()) return
        activeStreamJob?.cancel()
        viewModelScope.launch {
            repository.clearSession(sessionId)
        }
    }

    fun setReaction(messageId: String, reaction: String?) {
        viewModelScope.launch {
            repository.updateReaction(messageId, reaction)
        }
    }

    fun setCategoryFilter(category: String) {
        _uiState.update { it.copy(categoryFilter = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateApiKey(key: String) {
        preferencesManager.apiKey = key
        _uiState.update { it.copy(apiKey = key) }
    }

    fun updateModel(model: String) {
        preferencesManager.modelName = model
        _uiState.update { it.copy(modelName = model) }
    }

    fun updateSystemInstruction(instruction: String) {
        preferencesManager.systemInstruction = instruction
        _uiState.update { it.copy(systemInstruction = instruction) }
    }

    fun toggleDarkMode() {
        val current = _uiState.value.isDarkMode ?: false
        val next = !current
        preferencesManager.isDarkMode = next
        _uiState.update { it.copy(isDarkMode = next) }
    }

    fun saveTemplates(templates: List<PromptTemplate>) {
        preferencesManager.saveTemplates(templates)
        _uiState.update { it.copy(templates = templates) }
    }

    suspend fun exportTranscript(format: String): String {
        return repository.exportTranscript(_uiState.value.currentSessionId, format)
    }

    suspend fun exportTranscriptFile(context: android.content.Context, format: String): java.io.File {
        return repository.exportTranscriptFile(context, _uiState.value.currentSessionId, format)
    }
}
