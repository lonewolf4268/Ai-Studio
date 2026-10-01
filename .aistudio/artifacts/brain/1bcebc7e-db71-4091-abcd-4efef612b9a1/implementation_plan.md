# Kotlin Android Jetpack Compose AI Assistant

Rewrite the AI Studio assistant from a hybrid web/Capacitor app into a high-performance native Android application using Kotlin, Jetpack Compose Material 3, Room Database for persistent chat sessions, and the Google GenAI SDK.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were selected during Phase 1 clarification:

- **UI Framework**: Native Jetpack Compose with Material 3 design system, supporting system dark/light dynamic theming, smooth message animations, and responsive layout.
- **AI Model Client**: Google GenAI Android SDK directly calling Gemini Flash models with streaming response support (`generateContentStream`), multimodal attachments (images/text), and prompt follow-up suggestions.
- **Data Persistence**: Room SQLite database with Kotlin Coroutines and Flows for persistent multi-session chat histories, categories, tags, pinned sessions, and message reactions.

---

## 1. Overview & Core Concept

- **What It Does**: A native Android chat client for Gemini with real-time text streaming, multimodal image analysis, OCR/text extraction handling, customizable prompt templates, conversation categories/tagging, session pinning, search filtering, and JSON workspace import/export.
- **Target Audience**: Android users who want a responsive, offline-first Gemini AI assistant with session history and multimodal capabilities.
- **Key Value**: Native Android performance, zero webview overhead, smooth 60fps streaming text rendering, Material 3 theming, and local Room persistence.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Chat Conversation & Streaming**: User enters a prompt or picks from prompt templates (Code Refactoring, Bug Fixer, Summarizer, etc.), attaches photos/files, and observes real-time typewriter streaming responses from Gemini.
2. **Session Management & Organization**: Navigation drawer / side sheet to create new chats, switch sessions, pin favorites, filter by category (`General`, `Work`, `Coding`, `Personal`), search chat history, or rename/tag sessions.
3. **Multimodal & Voice Input**: Attach camera/gallery images, extract OCR text, or use native Android Speech Recognizer for hands-free voice transcription into the chat prompt.
4. **Message Actions**: Copy markdown code blocks, thumbs-up / thumbs-down reactions, edit prior user queries, retry/regenerate AI responses, and tap follow-up quick-reply suggestion chips.
5. **Settings & Workspace Tools**: Configure Gemini API key, model parameters (Gemini 2.5 Flash / Pro), toggle dark/light theme, clear history, and export transcripts (Markdown / Plaintext / Workspace JSON).

### Visual Identity & Theme
- **Theme**: Material 3 with Dynamic Color support (`MaterialTheme`), adaptive dark/light surfaces (`surface`, `surfaceVariant`, `primaryContainer`, `onSurface`).
- **Typography**: Clean Android system typography (`titleLarge`, `bodyMedium`, `labelSmall`, `headlineSmall`) with monospace rendering for code blocks.
- **Rhythm & Components**: Floating prompt bar, rounded message bubbles (`RoundedCornerShape(16.dp)`), status badges for model state, and animated drawer transitions.

---

## 3. Key Product Decisions & Trade-Offs

- **Kotlin + Jetpack Compose**:
  - *Chosen Approach*: 100% declarative Compose UI using `Scaffold`, `ModalNavigationDrawer`, `LazyColumn`, and `TopAppBar`.
  - *Why*: Modern Android standard, seamless state hoisting, reactive UI updates with Kotlin Coroutine `Flow`.
- **Google GenAI SDK Integration**:
  - *Chosen Approach*: Official Google GenAI / Gemini client on Android for direct streaming without middleware latency.
  - *Why*: Direct response streaming and native multipart content builder for bitmaps.
- **Room SQLite for Storage**:
  - *Chosen Approach*: Room `@Database` with `SessionEntity` and `MessageEntity` tables linked via foreign key and indexed queries.
  - *Why*: Reliable schema migrations, fast querying, and reactive `Flow<List<SessionWithMessages>>` observation.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                    │
│   ┌──────────────────────┐        ┌─────────────────────┐   │
│   │   Navigation Drawer  │        │   Main Chat Screen  │   │
│   │ (Sessions / Filters) │        │ (Messages / Stream) │   │
│   └──────────┬───────────┘        └──────────┬──────────┘   │
│              │                               │              │
│              ▼                               ▼              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                    ChatViewModel                    │   │
│   │   - StateFlow<ChatUiState>                          │   │
│   │   - Active streaming job & cancellation             │   │
│   └──────────────┬───────────────────────────┬──────────┘   │
└──────────────────┼───────────────────────────┼──────────────┘
                   │                           │
                   ▼                           ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│      ChatRepository / Room   │ │     GeminiApiService       │
│  ┌────────────────────────┐  │ │  ┌──────────────────────┐  │
│  │ SessionDao, MessageDao │  │ │  │ Google GenAI Client  │  │
│  │ SQLite Database (Room) │  │ │  │ Streaming & Suggest  │  │
│  └────────────────────────┘  │ │  └──────────────────────┘  │
└──────────────────────────────┘ └────────────────────────────┘
```

### Data Models & State
- **`ChatSessionEntity`**: `id`, `title`, `category`, `tagsJson`, `isPinned`, `createdAt`, `updatedAt`.
- **`ChatMessageEntity`**: `id`, `sessionId`, `sender` (`USER`, `AI`, `APP`), `message`, `timestamp`, `reaction`, `attachmentsJson`, `suggestionsJson`.
- **`ChatUiState`**: `currentSessionId`, `sessions`, `messages`, `isLoading`, `isStreaming`, `selectedAttachments`, `searchQuery`, `categoryFilter`.

### Interactive Handlers & Transitions
- **`onSendMessage(text, attachments)`**: Inserts user message to Room, initiates `generateContentStream`, updates streaming message buffer in ViewModel state, commits final AI response to Room.
- **`onRegenerateMessage(id)`**: Rolls back history up to selected turn and restarts generation.
- **`onEditMessage(id, newText)`**: Updates user turn and re-queries AI model.
- **`onExport(type)`**: Exports chat history as `.md`, `.txt`, or `.json` to Android Documents via Storage Access Framework / FileProvider.

---

## Verification Plan

### Build Verification
- Compile and build the native Android application using standard Gradle Android workflow (`./gradlew assembleDebug` / `compile_applet`).
- Verify zero compiler warnings and errors across all Kotlin and Compose components.
- Verify Room database initialization, DAO queries, and Gemini streaming state handling.
