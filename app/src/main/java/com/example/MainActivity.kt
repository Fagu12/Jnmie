package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.core.interfaces.AppSettings
import com.example.di.ProvideAppContainer
import com.example.ui.screens.MainScreen
import com.example.ui.theme.JustAnimeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = applicationContext as JustAnimeApp
        handleOAuthIntent(intent)

        setContent {
            ProvideAppContainer(container = app.container) {
                val settings by app.container.settingsRepository.settingsFlow.collectAsState(
                    initial = AppSettings()
                )
                JustAnimeTheme(
                    darkTheme = settings.darkTheme,
                    isAmoled = settings.amoledPureBlack
                ) {
                    MainScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val app = applicationContext as? JustAnimeApp ?: return
        lifecycleScope.launch {
            try {
                val result = app.container.handleAniListOAuthCallbackUseCase(uri.toString())
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    Toast.makeText(
                        this@MainActivity,
                        "Connected to AniList as ${user?.name ?: "User"}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = when {
                        error?.message?.contains("invalid_client") == true -> "AniList OAuth client configuration is invalid. Check the configured Client ID and redirect URI."
                        error?.message?.contains("access_denied") == true -> "Login cancelled."
                        error is java.net.UnknownHostException || error is java.net.ConnectException -> "Unable to connect to AniList. Check your internet connection."
                        else -> "AniList login could not be completed."
                    }
                    Toast.makeText(
                        this@MainActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "OAuth callback handling failed: ${e.message}", e)
                Toast.makeText(
                    this@MainActivity,
                    "AniList login could not be completed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
