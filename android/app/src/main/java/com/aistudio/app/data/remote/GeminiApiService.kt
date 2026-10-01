package com.aistudio.app.data.remote

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
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
        val model = getModelName().ifBlank { "gemini-2.5-flash" }

        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"
        val url = URL(endpointUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "text/event-stream")
            connectTimeout = 30000
            readTimeout = 60000
        }

        val requestBody = JSONObject()

        // System instruction
        val sysInstruction = getSystemInstruction()
        if (sysInstruction.isNotBlank()) {
            val sysObj = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", sysInstruction) })
                }
                put("parts", parts)
            }
            requestBody.put("systemInstruction", sysObj)
        }

        // Contents
        val contentsArr = JSONArray()

        // Add history turns (excluding system error messages)
        val conversationHistory = history.filter { it.sender == MessageSender.YOU || it.sender == MessageSender.AI }
        for (msg in conversationHistory) {
            val role = if (msg.sender == MessageSender.YOU) "user" else "model"
            val partsArr = JSONArray()

            // Message attachments
            if (msg.sender == MessageSender.YOU && msg.attachments.isNotEmpty()) {
                for (att in msg.attachments) {
                    if (!att.base64Data.isNullOrBlank()) {
                        val inlineData = JSONObject().apply {
                            put("mimeType", att.mimeType)
                            put("data", att.base64Data.substringAfter("base64,"))
                        }
                        partsArr.put(JSONObject().apply { put("inlineData", inlineData) })
                    }
                    if (!att.extractedText.isNullOrBlank()) {
                        partsArr.put(JSONObject().apply { put("text", "[Extracted Text from ${att.name}]:\n${att.extractedText}") })
                    }
                }
            }

            if (msg.message.isNotBlank()) {
                partsArr.put(JSONObject().apply { put("text", msg.message) })
            }

            if (partsArr.length() > 0) {
                contentsArr.put(JSONObject().apply {
                    put("role", role)
                    put("parts", partsArr)
                })
            }
        }

        // Current turn if not already in history
        if (attachments.isNotEmpty() && (history.isEmpty() || history.last().sender != MessageSender.YOU)) {
            val currentTurnParts = JSONArray()
            for (att in attachments) {
                if (!att.base64Data.isNullOrBlank()) {
                    val inlineData = JSONObject().apply {
                        put("mimeType", att.mimeType)
                        put("data", att.base64Data.substringAfter("base64,"))
                    }
                    currentTurnParts.put(JSONObject().apply { put("inlineData", inlineData) })
                }
                if (!att.extractedText.isNullOrBlank()) {
                    currentTurnParts.put(JSONObject().apply { put("text", "[Extracted OCR Text from ${att.name}]:\n${att.extractedText}") })
                }
            }
            if (userPrompt.isNotBlank()) {
                currentTurnParts.put(JSONObject().apply { put("text", userPrompt) })
            }
            contentsArr.put(JSONObject().apply {
                put("role", "user")
                put("parts", currentTurnParts)
            })
        }

        requestBody.put("contents", contentsArr)

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
                    // Skip malformed chunk
                }
            }
        }
        conn.disconnect()
    }.flowOn(Dispatchers.IO)

    /**
     * Generate quick reply suggestions based on the last conversation turn.
     */
    suspend fun generateSuggestions(history: List<ChatMessage>): List<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isBlank() || history.isEmpty()) return@withContext emptyList()

        try {
            val model = "gemini-2.5-flash"
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
                    val text = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    // Parse JSON array from model output
                    val cleaned = text.substringAfter("[").substringBeforeLast("]")
                    if (cleaned.isNotBlank()) {
                        val arr = JSONArray("[$cleaned]")
                        val results = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            results.add(arr.getString(i))
                        }
                        return@withContext results.take(3)
                    }
                }
            }
        } catch (_: Exception) {}
        return@withContext emptyList()
    }
}
