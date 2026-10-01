package com.aistudio.app.data.model

import java.io.Serializable

enum class MessageSender {
    YOU,
    AI,
    APP
}

data class Attachment(
    val uri: String,
    val mimeType: String,
    val name: String,
    val extractedText: String? = null,
    val base64Data: String? = null
) : Serializable

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val sender: MessageSender,
    val message: String,
    val timestamp: String,
    val attachments: List<Attachment> = emptyList(),
    val reaction: String? = null, // "thumbs-up" | "thumbs-down"
    val suggestions: List<String> = emptyList()
) : Serializable

data class ChatSession(
    val id: String,
    val title: String,
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable

data class PromptTemplate(
    val id: String,
    val title: String,
    val prompt: String
) : Serializable
