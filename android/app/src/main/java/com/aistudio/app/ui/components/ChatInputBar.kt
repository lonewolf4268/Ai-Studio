package com.aistudio.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.app.data.model.Attachment

@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    stagedAttachments: List<Attachment>,
    onRemoveAttachment: (Int) -> Unit,
    onPickImage: () -> Unit,
    onVoiceInputClick: () -> Unit,
    onOpenTemplates: () -> Unit,
    onSend: () -> Unit,
    onStopStreaming: () -> Unit,
    isStreaming: Boolean,
    isLoading: Boolean
) {
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Staged Attachments Preview Row
            AnimatedVisibility(visible = stagedAttachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    itemsIndexed(stagedAttachments) { index, att ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (att.mimeType.startsWith("image/")) Icons.Default.Image else Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = att.name,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onRemoveAttachment(index) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Row
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Attach file button
                IconButton(
                    onClick = onPickImage,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Templates button
                IconButton(
                    onClick = onOpenTemplates,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Templates",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Voice Dictation Button
                IconButton(
                    onClick = onVoiceInputClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Text Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    placeholder = {
                        Text(
                            text = if (stagedAttachments.isNotEmpty()) "Ask about attachments..." else "Ask Gemini...",
                            fontSize = 13.sp
                        )
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )

                // Send or Stop button
                if (isStreaming || isLoading) {
                    IconButton(
                        onClick = onStopStreaming,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    val canSend = inputText.isNotBlank() || stagedAttachments.isNotEmpty()
                    IconButton(
                        onClick = onSend,
                        enabled = canSend,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Send",
                            tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
