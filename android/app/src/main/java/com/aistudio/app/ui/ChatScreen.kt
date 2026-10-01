package com.aistudio.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.app.data.model.Attachment
import com.aistudio.app.data.model.ChatMessage
import com.aistudio.app.data.model.ChatSession
import com.aistudio.app.data.model.MessageSender
import com.aistudio.app.ui.components.*
import com.aistudio.app.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isTemplatesOpen by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<ChatSession?>(null) }

    val currentSession = remember(uiState.sessions, uiState.currentSessionId) {
        uiState.sessions.find { it.id == uiState.currentSessionId }
    }

    // Auto scroll to bottom when new messages arrive or when streaming
    LaunchedEffect(uiState.messages.size, uiState.streamingText) {
        if (uiState.autoScroll && (uiState.messages.isNotEmpty() || uiState.streamingText.isNotEmpty())) {
            val targetIndex = (uiState.messages.size + (if (uiState.isStreaming) 1 else 0)) - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        for (uri in uris) {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val base64DataUrl = "data:$mimeType;base64,$base64"
                    val filename = "Image_${System.currentTimeMillis()}.${mimeType.substringAfter("/")}"
                    val attachment = Attachment(
                        uri = uri.toString(),
                        mimeType = mimeType,
                        name = filename,
                        base64Data = base64DataUrl
                    )
                    viewModel.addAttachment(attachment)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Speech Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                inputText = if (inputText.isBlank()) spokenText else "$inputText $spokenText"
            }
        }
    }

    val startVoiceInput: () -> Unit = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini...")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // Export Transcript Action
    val handleExportTranscript: (String) -> Unit = { format ->
        coroutineScope.launch {
            try {
                val content = viewModel.exportTranscript(format)
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, content)
                    putExtra(Intent.EXTRA_TITLE, "AI Studio Transcript")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Export Chat Transcript")
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavDrawerContent(
                sessions = uiState.sessions,
                currentSessionId = uiState.currentSessionId,
                selectedCategory = uiState.categoryFilter,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onCategorySelect = { viewModel.setCategoryFilter(it) },
                onSessionSelect = { sessionId ->
                    viewModel.selectSession(sessionId)
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChatClick = {
                    viewModel.createNewSession(uiState.categoryFilter.takeIf { it != "All" } ?: "General")
                    coroutineScope.launch { drawerState.close() }
                },
                onTogglePin = { viewModel.togglePinSession(it) },
                onEditSession = { sessionToEdit = it },
                onDeleteSession = { viewModel.deleteSession(it) }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    title = currentSession?.title ?: "AI Studio",
                    category = currentSession?.category ?: "General",
                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                    onOpenSettings = { isSettingsOpen = true },
                    onOpenTemplates = { isTemplatesOpen = true },
                    onClearChat = { viewModel.clearChat() },
                    onExportTranscript = handleExportTranscript,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    isDarkMode = uiState.isDarkMode ?: false
                )
            },
            bottomBar = {
                ChatInputBar(
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    stagedAttachments = uiState.stagedAttachments,
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                    onPickImage = { imagePickerLauncher.launch("image/*") },
                    onVoiceInputClick = startVoiceInput,
                    onOpenTemplates = { isTemplatesOpen = true },
                    onSend = {
                        val text = inputText
                        inputText = ""
                        viewModel.sendMessage(text)
                    },
                    onStopStreaming = { viewModel.stopStreaming() },
                    isStreaming = uiState.isStreaming,
                    isLoading = uiState.isLoading
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    // Render historical messages
                    items(uiState.messages, key = { it.id }) { msg ->
                        MessageItemView(
                            message = msg,
                            onRetry = { viewModel.retryMessage(msg.id) },
                            onEdit = { newText -> viewModel.editMessage(msg.id, newText) },
                            onDelete = { viewModel.deleteMessage(msg.id) },
                            onReaction = { reaction -> viewModel.setReaction(msg.id, reaction) },
                            onSuggestionClick = { suggestion ->
                                viewModel.sendMessage(suggestion)
                            }
                        )
                    }

                    // Render active streaming message item
                    if (uiState.isStreaming && uiState.streamingMessageId != null) {
                        item(key = uiState.streamingMessageId) {
                            val tempMsg = ChatMessage(
                                id = uiState.streamingMessageId!!,
                                sessionId = uiState.currentSessionId,
                                sender = MessageSender.AI,
                                message = if (uiState.streamingText.isNotBlank()) uiState.streamingText else "Thinking...",
                                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            )
                            MessageItemView(
                                message = tempMsg,
                                onRetry = {},
                                onEdit = {},
                                onDelete = {},
                                onReaction = {},
                                onSuggestionClick = {}
                            )
                        }
                    }
                }

                // Scroll to bottom floating button if scrolled up
                val showScrollButton by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 2
                    }
                }

                AnimatedVisibility(
                    visible = showScrollButton,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                val total = uiState.messages.size + (if (uiState.isStreaming) 1 else 0)
                                if (total > 0) listState.animateScrollToItem(total - 1)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll to Bottom")
                    }
                }
            }
        }
    }

    // Settings Dialog
    if (isSettingsOpen) {
        ServerSettingsDialog(
            currentApiKey = uiState.apiKey,
            currentModel = uiState.modelName,
            currentInstruction = uiState.systemInstruction,
            onDismiss = { isSettingsOpen = false },
            onSave = { apiKey, model, instruction ->
                viewModel.updateApiKey(apiKey)
                viewModel.updateModel(model)
                viewModel.updateSystemInstruction(instruction)
            }
        )
    }

    // Session Edit Dialog
    sessionToEdit?.let { session ->
        EditSessionDialog(
            session = session,
            onDismiss = { sessionToEdit = null },
            onSave = { title, category, tags ->
                viewModel.renameSession(session.id, title, category, tags)
                sessionToEdit = null
            }
        )
    }

    // Prompt Templates Dialog
    if (isTemplatesOpen) {
        PromptTemplatesDialog(
            templates = uiState.templates,
            onSelectTemplate = { prompt ->
                inputText = prompt
            },
            onSaveTemplates = { newTemplates ->
                viewModel.saveTemplates(newTemplates)
            },
            onDismiss = { isTemplatesOpen = false }
        )
    }
}
