# AI Studio (Android)

Native Android AI Assistant application built with Kotlin, Jetpack Compose, Material 3, Room SQLite persistence, and direct Google Gemini API integration.

## Features

- **Interactive Gemini Chat**: Real-time conversational AI streaming using Google Gemini (`gemini-3.5-flash`, `gemini-3.1-pro-preview`, `gemini-3.1-flash-lite-preview`).
- **Multimodal Image Support**: Attach and send images to Gemini with automatic base64 encoding and inline analysis.
- **Voice Dictation**: Speech-to-text input with native Android speech recognizer.
- **Local Room Database**: Complete local persistence of chat sessions, message histories, reactions, categories, and tags.
- **Session Management**: Create, organize, pin, rename, categorize (General, Work, Coding, Personal), and delete chat sessions.
- **Prompt Templates**: Quick prompt shortcuts (Code Refactoring, Bug Fixer, Summarizer, ELI5) with custom template creation.
- **Rich Message Controls**: Edit sent messages with conversation branch rewind, retry/regenerate responses, copy to clipboard, and give thumbs up/down reactions.
- **Transcript Export**: Share or save chat transcripts as Markdown (`.md`) or plain text.
- **Theming**: Full Dark Mode and Light Mode support adhering to Material Design 3 guidelines.

## Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Local Database**: Room SQLite (with KSP)
- **Networking**: OkHttp & HttpURLConnection SSE streaming
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)
