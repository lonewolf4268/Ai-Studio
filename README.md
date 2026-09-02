# AI Studio

An AI-powered chatbot application ported to a modern React + TypeScript web application with Express backend and Google Gemini API integration.

## Features

- **Interactive Gemini Chat**: Conversational AI chatbot with context memory and streaming/formatted Markdown output.
- **Image OCR & Vision Processing**: Client-side optical character recognition (OCR) and multimodal vision processing to recognize text from uploaded images and answer questions about them.
- **Markdown & Code Highlighting**: Rich rendering of code snippets, tables, blockquotes, and formatting.
- **Responsive Bubble Layout**: Faithful representation of the original AI Studio messenger interface.
- **Generation Controls**: Stop streaming responses, edit prompts, resend them, and regenerate assistant replies from the original user turn.
- **Workspace Backup & Restore**: Export and restore sessions, prompt snippets, settings, and attachments as a portable JSON backup.

## Tech Stack

- **Frontend**: React 18, TypeScript, Tailwind CSS, Lucide Icons, Motion, React Markdown, Tesseract.js
- **Backend**: Node.js, Express, `@google/genai` (Gemini 3.7 Flash)
- **Mobile**: Capacitor 8 (Android)
- **Bundler**: Vite

## Android App Development

AI Studio includes full native Android support via Capacitor:

- **Build Web & Sync to Android**:
  ```bash
  npm run android:build
  ```
- **Open in Android Studio**:
  ```bash
  npm run android:open
  ```
  *(Or open the `android/` directory directly in Android Studio)*
- **Run Directly on Device/Emulator**:
  ```bash
  npm run android:run
  ```
- **Build APK via Gradle**:
  ```bash
  cd android
  .\gradlew.bat assembleDebug
  ```
  The resulting APK is located at `android/app/build/outputs/apk/debug/app-debug.apk`.

### Mobile Backend Configuration
When running on an Android Emulator or physical device:
1. Tap the **Server Settings** (server icon) in the top app bar.
2. For the **Android Emulator**, use the default: `http://10.0.2.2:3000`.
3. For a **Physical Android Device**, enter your host machine's Wi-Fi IP address (e.g., `http://192.168.x.x:3000`).
4. Click **Test Connection** to verify connectivity with the Express backend, then click **Save Configuration**.
