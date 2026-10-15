package com.aistudio.app.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.aistudio.app.data.model.Attachment
import com.aistudio.app.data.model.ChatMessage
import com.aistudio.app.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service class handling direct REST & SSE streaming communication with Google Gemini API.
 * Ensures robust chunk streaming, prevents premature token truncation, and sanitizes multiturn history.
 */
class GeminiApiService(
    private val getApiKey: () -> String,
    private val getModelName: () -> String,
    private val getSystemInstruction: () -> String
) {

    /**
     * Stream Gemini response chunks via SSE / REST streamGenerateContent API.
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

        val rawModel = getModelName().trim()
        val model = if (rawModel.isBlank()) "gemini-2.5-flash" else rawModel

        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"
        val url = URL(endpointUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "text/event-stream")
            connectTimeout = 60000
            readTimeout = 60000
        }

        val requestBody = JSONObject()

        // 1. System instruction
        val sysInstruction = getSystemInstruction().trim()
        if (sysInstruction.isNotBlank()) {
            val sysObj = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", sysInstruction) })
                }
                put("parts", parts)
            }
            requestBody.put("systemInstruction", sysObj)
        }

        // 2. Generation Config - ensure maxOutputTokens is large enough (8192) to avoid truncation
        val genConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("topP", 0.95)
            put("topK", 40)
            put("maxOutputTokens", 8192)
        }
        requestBody.put("generationConfig", genConfig)

        // 3. Safety Settings - prevent false positive cuts on code/analysis queries
        val safetySettings = JSONArray().apply {
            val categories = listOf(
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT",
                "HARM_CATEGORY_CIVIC_INTEGRITY"
            )
            for (cat in categories) {
                put(JSONObject().apply {
                    put("category", cat)
                    put("threshold", "BLOCK_NONE")
                })
            }
        }
        requestBody.put("safetySettings", safetySettings)

        // 4. Build sanitized, strictly alternating multiturn conversation history
        val contentsArr = buildSanitizedContents(history, attachments, userPrompt)
        requestBody.put("contents", contentsArr)

        try {
            // Write request payload
            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream ?: conn.inputStream
                val errorText = BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                var message = "API request failed with code $responseCode"
                try {
                    val errObj = JSONObject(errorText)
                    if (errObj.has("error")) {
                        message = errObj.getJSONObject("error").optString("message", message)
                    }
                } catch (_: Exception) {}
                throw Exception(message)
            }

            // Read SSE stream
            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.startsWith("data:")) {
                    val dataPayload = trimmed.substring(5).trim()
                    if (dataPayload == "[DONE]") {
                        break
                    }
                    try {
                        val chunkJson = JSONObject(dataPayload)
                        val candidates = chunkJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            if (content != null) {
                                val parts = content.optJSONArray("parts")
                                if (parts != null) {
                                    for (i in 0 until parts.length()) {
                                        val part = parts.getJSONObject(i)
                                        val text = part.optString("text", "")
                                        if (text.isNotEmpty()) {
                                            emit(text)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Skip unparseable non-content chunks
                    }
                }
            }
        } finally {
            try {
                conn.disconnect()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Sanitizes conversation history turns to strictly obey Gemini API rules:
     * - Must start with 'user' role (drops any initial local UI greetings)
     * - Strict alternation: user -> model -> user -> model -> user
     * - Merges consecutive turns of the same role
     * - Strips empty messages and 'Thinking...' placeholders
     * - Ends with the current user prompt
     */
    private fun buildSanitizedContents(
        history: List<ChatMessage>,
        currentAttachments: List<Attachment>,
        currentPrompt: String
    ): JSONArray {
        // Collect relevant historical turns
        data class RawTurn(val role: String, val parts: MutableList<JSONObject>)

        val rawTurns = mutableListOf<RawTurn>()

        // Filter valid user and AI messages from history
        val filtered = history.filter {
            (it.sender == MessageSender.YOU || it.sender == MessageSender.AI) &&
                    it.message.trim() != "Thinking..." &&
                    !(it.message.isBlank() && it.attachments.isEmpty())
        }

        // Find the index of the first user message (drop any leading AI greetings)
        val firstUserIndex = filtered.indexOfFirst { it.sender == MessageSender.YOU }
        val usableHistory = if (firstUserIndex != -1) filtered.subList(firstUserIndex, filtered.size) else emptyList()

        for (msg in usableHistory) {
            val role = if (msg.sender == MessageSender.YOU) "user" else "model"
            val parts = mutableListOf<JSONObject>()

            if (msg.sender == MessageSender.YOU && msg.attachments.isNotEmpty()) {
                for (att in msg.attachments) {
                    val inlineData = createInlineDataObject(att)
                    if (inlineData != null) {
                        parts.add(JSONObject().apply { put("inlineData", inlineData) })
                    }
                    if (!att.extractedText.isNullOrBlank()) {
                        parts.add(JSONObject().apply {
                            put("text", "[Extracted OCR Text from ${att.name}]:\n${att.extractedText}")
                        })
                    }
                }
            }

            if (msg.message.isNotBlank()) {
                parts.add(JSONObject().apply { put("text", msg.message) })
            }

            if (parts.isNotEmpty()) {
                // If previous turn has the same role, merge parts to preserve strict alternation
                if (rawTurns.isNotEmpty() && rawTurns.last().role == role) {
                    rawTurns.last().parts.addAll(parts)
                } else {
                    rawTurns.add(RawTurn(role, parts))
                }
            }
        }

        // Check if the current prompt turn is already represented at the end
        val currentParts = mutableListOf<JSONObject>()
        for (att in currentAttachments) {
            val inlineData = createInlineDataObject(att)
            if (inlineData != null) {
                currentParts.add(JSONObject().apply { put("inlineData", inlineData) })
            }
            if (!att.extractedText.isNullOrBlank()) {
                currentParts.add(JSONObject().apply {
                    put("text", "[Extracted OCR Text from ${att.name}]:\n${att.extractedText}")
                })
            }
        }
        if (currentPrompt.isNotBlank()) {
            currentParts.add(JSONObject().apply { put("text", currentPrompt) })
        }

        // Determine if current turn is already the last turn in rawTurns
        val needsCurrentTurn = currentParts.isNotEmpty() && (
                rawTurns.isEmpty() ||
                        rawTurns.last().role != "user" ||
                        rawTurns.last().parts.none { it.optString("text") == currentPrompt }
                )

        if (needsCurrentTurn) {
            if (rawTurns.isNotEmpty() && rawTurns.last().role == "user") {
                rawTurns.last().parts.addAll(currentParts)
            } else {
                rawTurns.add(RawTurn("user", currentParts))
            }
        }

        // If history was completely empty, ensure at least one user prompt turn exists
        if (rawTurns.isEmpty()) {
            val defaultParts = mutableListOf<JSONObject>()
            if (currentPrompt.isNotBlank()) {
                defaultParts.add(JSONObject().apply { put("text", currentPrompt) })
            } else {
                defaultParts.add(JSONObject().apply { put("text", "Hello") })
            }
            rawTurns.add(RawTurn("user", defaultParts))
        }

        // Ensure the last turn is from user
        if (rawTurns.last().role != "user") {
            val finalUserParts = mutableListOf<JSONObject>().apply {
                if (currentPrompt.isNotBlank()) {
                    add(JSONObject().apply { put("text", currentPrompt) })
                } else {
                    add(JSONObject().apply { put("text", "Please continue.") })
                }
            }
            rawTurns.add(RawTurn("user", finalUserParts))
        }

        // Convert to JSONArray
        val result = JSONArray()
        for (turn in rawTurns) {
            val turnObj = JSONObject().apply {
                put("role", turn.role)
                val partsArr = JSONArray()
                turn.parts.forEach { partsArr.put(it) }
                put("parts", partsArr)
            }
            result.put(turnObj)
        }

        return result
    }

    /**
     * Create inlineData JSON object from an attachment.
     */
    private fun createInlineDataObject(attachment: Attachment): JSONObject? {
        val rawBase64 = attachment.base64Data ?: return null
        val cleanBase64 = if (rawBase64.contains(",")) {
            rawBase64.substringAfter(",")
        } else {
            rawBase64
        }.trim()

        if (cleanBase64.isBlank()) return null

        val mimeType = when {
            attachment.mimeType.isNotBlank() -> attachment.mimeType
            attachment.name.endsWith(".png", ignoreCase = true) -> "image/png"
            attachment.name.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/jpeg"
        }

        return JSONObject().apply {
            put("mimeType", mimeType)
            put("data", cleanBase64)
        }
    }

    /**
     * Generate quick reply suggestions based on the last conversation turn.
     */
    suspend fun generateSuggestions(history: List<ChatMessage>): List<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isBlank() || history.isEmpty()) return@withContext emptyList()

        try {
            val rawModel = getModelName().trim()
            val model = if (rawModel.isBlank()) "gemini-2.5-flash" else rawModel
            val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val url = URL(endpointUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val requestBody = JSONObject()
            val promptText = "Based on the following conversation, provide 3 short, relevant, diverse follow-up questions or prompt ideas the user might want to ask next. Format strictly as a JSON array of 3 strings (e.g. [\"Option 1\", \"Option 2\", \"Option 3\"]). Keep each suggestion under 8 words.\n\nConversation context:\n" +
                    history.takeLast(4).joinToString("\n") { "${it.sender.name}: ${it.message}" }

            val contentsArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", promptText) })
                    })
                })
            }
            requestBody.put("contents", contentsArr)

            OutputStreamWriter(conn.outputStream, "UTF-8").use {
                it.write(requestBody.toString())
                it.flush()
            }

            if (conn.responseCode in 200..299) {
                val resp = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                val json = JSONObject(resp)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

                    val jsonStart = text.indexOf('[')
                    val jsonEnd = text.lastIndexOf(']')
                    if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                        val arrayStr = text.substring(jsonStart, jsonEnd + 1)
                        val arr = JSONArray(arrayStr)
                        val results = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            val item = arr.optString(i, "").trim()
                            if (item.isNotBlank()) {
                                results.add(item)
                            }
                        }
                        return@withContext results.take(3)
                    }
                }
            }
        } catch (_: Exception) {}
        return@withContext emptyList()
    }
}
