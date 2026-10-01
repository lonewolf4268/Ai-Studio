package com.aistudio.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.app.data.model.ChatMessage
import com.aistudio.app.data.model.MessageSender
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItemView(
    message: ChatMessage,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onReaction: (String?) -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var editInputText by remember { mutableStateOf(message.message) }
    var isCopied by remember { mutableStateOf(false) }

    val isUser = message.sender == MessageSender.YOU
    val isAppError = message.sender == MessageSender.APP

    // Reset copy state after 2 seconds
    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    val copyToClipboard = {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val label = if (isUser) "User Message" else "AI Response"
            clipboard.setPrimaryClip(ClipData.newPlainText(label, message.message))
            isCopied = true
            Toast.makeText(context, if (isUser) "Message copied to clipboard" else "AI response copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Copy failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender Name & Timestamp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isUser) "You" else if (isAppError) "System Error" else "Gemini AI",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isAppError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = message.timestamp,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Attachments if any
        if (message.attachments.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .widthIn(max = 320.dp)
            ) {
                items(message.attachments) { att ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (att.mimeType.startsWith("image/")) Icons.Default.Image else Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = att.name,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Message Bubble with Long-Press to Copy
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = when {
                isAppError -> MaterialTheme.colorScheme.errorContainer
                isUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .widthIn(max = 340.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { copyToClipboard() }
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editInputText,
                        onValueChange = { editInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        TextButton(onClick = { isEditing = false }) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                isEditing = false
                                onEdit(editInputText)
                            }
                        ) {
                            Text("Save & Resend", fontSize = 12.sp)
                        }
                    }
                } else {
                    RenderMarkdownContent(
                        markdown = message.message,
                        isUser = isUser,
                        isError = isAppError
                    )
                }
            }
        }

        // Action Toolbar (Copy, Reactions, Retry, Edit)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            // Enhanced Copy Button with Visual Feedback
            IconButton(
                onClick = copyToClipboard,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("copy_response_button")
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy Response",
                    tint = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }

            if (!isUser && !isAppError) {
                // Thumbs up
                IconButton(
                    onClick = {
                        val next = if (message.reaction == "thumbs-up") null else "thumbs-up"
                        onReaction(next)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Like",
                        tint = if (message.reaction == "thumbs-up") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Thumbs down
                IconButton(
                    onClick = {
                        val next = if (message.reaction == "thumbs-down") null else "thumbs-down"
                        onReaction(next)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (message.reaction == "thumbs-down") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Retry
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            if (isUser) {
                // Edit user message
                IconButton(
                    onClick = {
                        editInputText = message.message
                        isEditing = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Prompt",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Delete message
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Quick suggestions chips
        if (message.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            ) {
                items(message.suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = { Text(suggestion, fontSize = 11.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderMarkdownContent(
    markdown: String,
    isUser: Boolean,
    isError: Boolean
) {
    val textColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    if (markdown.contains("```")) {
        // Code blocks are present: render Markdown sections and code blocks with syntax styling
        val segments = markdown.split("```")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            segments.forEachIndexed { index, segment ->
                if (index % 2 == 0) {
                    if (segment.isNotBlank()) {
                        MarkdownText(
                            markdown = segment.trim(),
                            textColor = textColor,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val lines = segment.trim().lines()
                    val lang = if (lines.isNotEmpty() && lines[0].all { it.isLetterOrDigit() }) lines[0] else "code"
                    val codeContent = if (lang != "code" && lines.size > 1) lines.drop(1).joinToString("\n") else segment

                    CodeBlockView(language = lang, code = codeContent)
                }
            }
        }
    } else {
        // Render full markdown text including bold, lists, links, headers
        MarkdownText(
            markdown = markdown,
            textColor = textColor,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun CodeBlockView(language: String, code: String) {
    val context = LocalContext.current
    var isCodeCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCodeCopied) {
        if (isCodeCopied) {
            delay(2000)
            isCodeCopied = false
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E1E),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = language.uppercase(),
                    color = Color(0xFF9CDCFE),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                        isCodeCopied = true
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(
                        imageVector = if (isCodeCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isCodeCopied) Color(0xFF81C784) else Color.LightGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCodeCopied) "Copied" else "Copy Code",
                        color = if (isCodeCopied) Color(0xFF81C784) else Color.LightGray,
                        fontSize = 10.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = code,
                    color = Color(0xFFD4D4D4),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
