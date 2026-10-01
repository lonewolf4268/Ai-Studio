package com.aistudio.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    category: String,
    activePersona: String? = null,
    onMenuClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTemplates: () -> Unit,
    onClearChat: () -> Unit,
    onExportTranscript: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    isDarkMode: Boolean
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title.ifBlank { "AI Studio" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (!activePersona.isNullOrBlank()) "$category • Persona: $activePersona" else category,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Drawer"
                )
            }
        },
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "AI Persona & Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onOpenTemplates) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Prompt Templates"
                )
            }
            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme"
                )
            }
            IconButton(onClick = { showOptionsMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options"
                )
            }

            DropdownMenu(
                expanded = showOptionsMenu,
                onDismissRequest = { showOptionsMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("AI Persona & Settings") },
                    leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    onClick = {
                        showOptionsMenu = false
                        onOpenSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export as PDF (.pdf)") },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showOptionsMenu = false
                        onExportTranscript("pdf")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export as Text (.txt)") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    onClick = {
                        showOptionsMenu = false
                        onExportTranscript("txt")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export as Markdown (.md)") },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                    onClick = {
                        showOptionsMenu = false
                        onExportTranscript("md")
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Clear Chat History", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showOptionsMenu = false
                        onClearChat()
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
