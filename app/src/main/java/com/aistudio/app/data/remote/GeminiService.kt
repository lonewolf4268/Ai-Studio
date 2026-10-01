package com.aistudio.app.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.aistudio.app.data.model.Attachment
import com.aistudio.app.data.model.ChatMessage
import com.aistudio.app.data.model.MessageSender
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Service class handling communication with Google Gemini API using Google GenAI SDK.
 */
class GeminiService(
    private val getApiKey: () -> String,
    private val getModelName: () -> String,
    private val getSystemInstruction: () -> String
) {

    /**
     * Initializes and returns a GenerativeModel instance with current settings.
     */
    private fun createGenerativeModel(systemInstructionText: String? = null): GenerativeModel {
        val apiKey = getApiKey().trim()
        val modelName = getModelName().ifBlank { "gemini-3.5-flash" }
        val sysInstruction = systemInstructionText ?: getSystemInstruction().trim()

        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
            },
            systemInstruction = if (sysInstruction.isNotBlank()) {
                content { text(sysInstruction) }
            } else null
        )
    }

    /**
     * Stream responses from Gemini for the current conversation history and prompt.
     */
    fun streamChatResponse(
        history: List<ChatMessage>,
        attachments: List<Attachment>,
        userPrompt: String
    ): Flow<String> = flow {
        val apiKey = getApiKey().trim()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API Key is missing. Please open Settings (gear icon in the top right bar) to enter your API Key.")
        }
        val generativeModel = createGenerativeModel()

        // Build conversation history content turns
        val contentTurns = mutableListOf<Content>()
        val filteredHistory = history.filter { it.sender == MessageSender.YOU || it.sender == MessageSender.AI }

        for (msg in filteredHistory) {
            val role = if (msg.sender == MessageSender.YOU) "user" else "model"
            val turn = content(role = role) {
                // Attachments if any
                if (msg.sender == MessageSender.YOU && msg.attachments.isNotEmpty()) {
                    for (att in msg.attachments) {
                        val bitmap = decodeAttachmentBitmap(att)
                        if (bitmap != null) {
                            image(bitmap)
                        }
                        if (!att.extractedText.isNullOrBlank()) {
                            text("[Extracted text from ${att.name}]:\n${att.extractedText}")
                        }
                    }
                }
                if (msg.message.isNotBlank()) {
                    text(msg.message)
                }
            }
            contentTurns.add(turn)
        }

        // Add staged attachments/current prompt if not already in history
        val hasStagedTurn = attachments.isNotEmpty() && (history.isEmpty() || history.last().sender != MessageSender.YOU)
        if (hasStagedTurn) {
            val currentTurn = content(role = "user") {
                for (att in attachments) {
                    val bitmap = decodeAttachmentBitmap(att)
                    if (bitmap != null) {
                        image(bitmap)
                    }
                    if (!att.extractedText.isNullOrBlank()) {
                        text("[Extracted OCR text from ${att.name}]:\n${att.extractedText}")
                    }
                }
                if (userPrompt.isNotBlank()) {
                    text(userPrompt)
                }
            }
            contentTurns.add(currentTurn)
        }

        if (contentTurns.isEmpty()) {
            val singlePromptTurn = content(role = "user") { text(userPrompt) }
            contentTurns.add(singlePromptTurn)
        }

        // Stream generated response chunks
        val responseStream = generativeModel.generateContentStream(*contentTurns.toTypedArray())
        responseStream.collect { responseChunk ->
            val chunkText = responseChunk.text
            if (!chunkText.isNullOrEmpty()) {
                emit(chunkText)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Generate 3 follow-up quick suggestion prompts.
     */
    suspend fun generateFollowUpSuggestions(history: List<ChatMessage>): List<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isBlank() || history.isEmpty()) return@withContext emptyList()

        try {
            val suggestionModel = GenerativeModel(
                modelName = "gemini-3.5-flash",
                apiKey = apiKey
            )
            val contextHistory = history.takeLast(4).joinToString("\n") { "${it.sender.name}: ${it.message}" }
            val prompt = "Based on this conversation context, generate exactly 3 short follow-up questions or next actions the user might want to ask. Output strictly a JSON array of 3 strings (e.g. [\"Explain more\", \"Give examples\", \"How does this work?\"]). Keep each suggestion under 7 words.\n\nContext:\n$contextHistory"

            val response = suggestionModel.generateContent(prompt)
            val text = response.text ?: ""
            val jsonStart = text.indexOf('[')
            val jsonEnd = text.lastIndexOf(']')

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                val arrayStr = text.substring(jsonStart + 1, jsonEnd)
                val items = arrayStr.split(",")
                    .map { it.trim().trim('"', '\'', '\n', '\r') }
                    .filter { it.isNotBlank() }
                return@withContext items.take(3)
            }
        } catch (_: Exception) {}

        return@withContext emptyList()
    }

    /**
     * Helper to decode an attachment's base64 image data into a Bitmap.
     */
    private fun decodeAttachmentBitmap(attachment: Attachment): Bitmap? {
        val rawBase64 = attachment.base64Data ?: return null
        return try {
            val cleanBase64 = if (rawBase64.contains(",")) {
                rawBase64.substringAfter(",")
            } else {
                rawBase64
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
