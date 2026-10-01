package com.aistudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.app.ui.ChatScreen
import com.aistudio.app.ui.theme.AIStudioTheme
import com.aistudio.app.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val chatViewModel: ChatViewModel = viewModel()
            val uiState by chatViewModel.uiState.collectAsState()
            val darkTheme = uiState.isDarkMode ?: isSystemInDarkTheme()

            AIStudioTheme(darkTheme = darkTheme) {
                ChatScreen(viewModel = chatViewModel)
            }
        }
    }
}
