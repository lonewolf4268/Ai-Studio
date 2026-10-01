package com.aistudio.app.data.local

import androidx.room.*
import com.aistudio.app.data.model.Attachment
import com.aistudio.app.data.model.ChatMessage
import com.aistudio.app.data.model.ChatSession
import com.aistudio.app.data.model.MessageSender
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "chat_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val tagsJson: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): ChatSession {
        val tagsList = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(tagsJson)
            for (i in 0 until jsonArr.length()) {
                tagsList.add(jsonArr.getString(i))
            }
        } catch (_: Exception) {}
        return ChatSession(
            id = id,
            title = title,
            category = category,
            tags = tagsList,
            isPinned = isPinned,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(session: ChatSession): SessionEntity {
            val jsonArr = JSONArray()
            session.tags.forEach { jsonArr.put(it) }
            return SessionEntity(
                id = session.id,
                title = session.title,
                category = session.category,
                tagsJson = jsonArr.toString(),
                isPinned = session.isPinned,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt
            )
        }
    }
}

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sender: String, // "YOU", "AI", "APP"
    val message: String,
    val timestamp: String,
    val attachmentsJson: String,
    val reaction: String?,
    val suggestionsJson: String
) {
    fun toDomain(): ChatMessage {
        val senderEnum = when (sender) {
            "YOU" -> MessageSender.YOU
            "AI" -> MessageSender.AI
            else -> MessageSender.APP
        }
        val attachmentList = mutableListOf<Attachment>()
        try {
            val jsonArr = JSONArray(attachmentsJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                attachmentList.add(
                    Attachment(
                        uri = obj.optString("uri"),
                        mimeType = obj.optString("mimeType"),
                        name = obj.optString("name"),
                        extractedText = if (obj.has("extractedText")) obj.getString("extractedText") else null,
                        base64Data = if (obj.has("base64Data")) obj.getString("base64Data") else null
                    )
                )
            }
        } catch (_: Exception) {}

        val suggestionList = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(suggestionsJson)
            for (i in 0 until jsonArr.length()) {
                suggestionList.add(jsonArr.getString(i))
            }
        } catch (_: Exception) {}

        return ChatMessage(
            id = id,
            sessionId = sessionId,
            sender = senderEnum,
            message = message,
            timestamp = timestamp,
            attachments = attachmentList,
            reaction = reaction,
            suggestions = suggestionList
        )
    }

    companion object {
        fun fromDomain(msg: ChatMessage): MessageEntity {
            val attachArr = JSONArray()
            msg.attachments.forEach { att ->
                val obj = JSONObject().apply {
                    put("uri", att.uri)
                    put("mimeType", att.mimeType)
                    put("name", att.name)
                    att.extractedText?.let { put("extractedText", it) }
                    att.base64Data?.let { put("base64Data", it) }
                }
                attachArr.put(obj)
            }

            val suggArr = JSONArray()
            msg.suggestions.forEach { suggArr.put(it) }

            return MessageEntity(
                id = msg.id,
                sessionId = msg.sessionId,
                sender = msg.sender.name,
                message = msg.message,
                timestamp = msg.timestamp,
                attachmentsJson = attachArr.toString(),
                reaction = msg.reaction,
                suggestionsJson = suggArr.toString()
            )
        }
    }
}
