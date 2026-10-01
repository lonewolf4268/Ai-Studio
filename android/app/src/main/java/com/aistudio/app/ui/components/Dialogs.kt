package com.aistudio.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.app.data.model.ChatSession
import com.aistudio.app.data.model.PromptTemplate

@Composable
fun ServerSettingsDialog(
    currentApiKey: String,
    currentModel: String,
    currentInstruction: String,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, model: String, instruction: String) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var selectedModel by remember { mutableStateOf(currentModel) }
    var instructionInput by remember { mutableStateOf(currentInstruction) }
    var isModelMenuOpen by remember { mutableStateOf(false) }

    val models = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Settings & API Key", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Gemini API Key
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Model Selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("AI Model") },
                        trailingIcon = {
                            IconButton(onClick = { isModelMenuOpen = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Model")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    DropdownMenu(
                        expanded = isModelMenuOpen,
                        onDismissRequest = { isModelMenuOpen = false }
                    ) {
                        models.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    selectedModel = m
                                    isModelMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // System Instruction
                OutlinedTextField(
                    value = instructionInput,
                    onValueChange = { instructionInput = it },
                    label = { Text("System Instruction") },
                    placeholder = { Text("You are a helpful AI...") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKeyInput.trim(), selectedModel, instructionInput.trim())
                    onDismiss()
                }
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditSessionDialog(
    session: ChatSession,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, tags: List<String>) -> Unit
) {
    var titleInput by remember { mutableStateOf(session.title) }
    var selectedCategory by remember { mutableStateOf(session.category) }
    var tagsInput by remember { mutableStateOf(session.tags.joinToString(", ")) }
    var isCategoryMenuOpen by remember { mutableStateOf(false) }

    val categories = listOf("General", "Work", "Coding", "Personal")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Chat Session", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            IconButton(onClick = { isCategoryMenuOpen = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    DropdownMenu(
                        expanded = isCategoryMenuOpen,
                        onDismissRequest = { isCategoryMenuOpen = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    isCategoryMenuOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma-separated)") },
                    placeholder = { Text("kotlin, ai, refactor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(titleInput.trim(), selectedCategory, parsedTags)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PromptTemplatesDialog(
    templates: List<PromptTemplate>,
    onSelectTemplate: (String) -> Unit,
    onSaveTemplates: (List<PromptTemplate>) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddCustom by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newPrompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Prompt Templates", fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddCustom = !showAddCustom }) {
                    Icon(
                        imageVector = if (showAddCustom) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Add Template"
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (showAddCustom) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text("Create New Template", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            placeholder = { Text("Template Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = newPrompt,
                            onValueChange = { newPrompt = it },
                            placeholder = { Text("Prompt text...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank() && newPrompt.isNotBlank()) {
                                    val newT = PromptTemplate(
                                        id = "custom-${System.currentTimeMillis()}",
                                        title = newTitle.trim(),
                                        prompt = newPrompt.trim()
                                    )
                                    onSaveTemplates(templates + newT)
                                    newTitle = ""
                                    newPrompt = ""
                                    showAddCustom = false
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Add")
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(templates) { template ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectTemplate(template.prompt)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(template.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        template.prompt,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onSaveTemplates(templates.filter { it.id != template.id })
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
