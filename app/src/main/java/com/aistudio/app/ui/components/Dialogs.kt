package com.aistudio.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.app.data.model.ChatSession
import com.aistudio.app.data.model.PromptTemplate

import androidx.compose.ui.window.DialogProperties

data class PersonaPreset(
    val name: String,
    val icon: String,
    val instruction: String
)

val personaPresets = listOf(
    PersonaPreset("General AI", "🤖", "You are a helpful, versatile, and accurate AI assistant."),
    PersonaPreset("Software Engineer", "💻", "You are an expert Senior Software Engineer. Provide clean, production-ready code with concise explanations, edge cases, and best architectural practices."),
    PersonaPreset("Technical Writer", "✍️", "You are a skilled technical writer. Format information clearly using bold headings, structured bullet points, clean markdown, and concise prose."),
    PersonaPreset("Patient Tutor", "🎓", "You are a patient, encouraging tutor. Explain complex concepts step-by-step using clear analogies, and ask engaging follow-up questions."),
    PersonaPreset("Direct & Concise", "⚡", "You are a direct, zero-fluff AI assistant. Provide short, precise answers without preamble, disclaimers, or repetitive filler."),
    PersonaPreset("Creative Writer", "🎨", "You are an imaginative storyteller and creative writer. Craft rich, engaging, vivid descriptions and creative prose.")
)

@Composable
fun ServerSettingsDialog(
    currentApiKey: String,
    currentModel: String,
    currentInstruction: String,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, model: String, instruction: String) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(currentModel) }
    var instructionInput by remember { mutableStateOf(currentInstruction) }
    var isModelMenuOpen by remember { mutableStateOf(false) }

    val models = listOf(
        Pair("gemini-3.5-flash", "Recommended • Fast & Multimodal"),
        Pair("gemini-3.1-pro-preview", "Advanced Reasoning & Coding"),
        Pair("gemini-3.1-flash-lite-preview", "Ultra-fast & Lightweight")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("AI Persona & Settings", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Persona & System Instruction
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "System Persona & Instructions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Choose a preset persona or define custom instructions for Gemini to follow in all conversations:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Persona Presets Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(personaPresets) { preset ->
                            val isSelected = instructionInput.trim() == preset.instruction.trim()
                            FilterChip(
                                selected = isSelected,
                                onClick = { instructionInput = preset.instruction },
                                label = {
                                    Text("${preset.icon} ${preset.name}", fontSize = 12.sp)
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    // System Instruction Text Field
                    OutlinedTextField(
                        value = instructionInput,
                        onValueChange = { instructionInput = it },
                        label = { Text("System Instruction / Persona Prompt") },
                        placeholder = { Text("Define how the AI should answer, its tone, or expertise...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("system_instruction_input"),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Text(
                                text = "${instructionInput.length} characters",
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    )
                }

                HorizontalDivider()

                // Section 2: AI Model Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AI Model Selection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = models.find { it.first == selectedModel }?.let { "${it.first} (${it.second})" } ?: selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model Architecture") },
                            trailingIcon = {
                                IconButton(onClick = { isModelMenuOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Model")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = isModelMenuOpen,
                            onDismissRequest = { isModelMenuOpen = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            models.forEach { (mId, mDesc) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(mId, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(mDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        selectedModel = mId
                                        isModelMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Section 3: Gemini API Key
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "API Configuration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKeyInput.trim(), selectedModel, instructionInput.trim())
                    onDismiss()
                },
                modifier = Modifier.testTag("save_settings_button")
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
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
