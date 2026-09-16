package com.example.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.example.JustAnimeApp

/**
 * CompositionLocal provider for AppContainer to make dependency injection
 * seamlessly accessible anywhere in the Compose hierarchy.
 */
val LocalAppContainer: ProvidableCompositionLocal<AppContainer> = staticCompositionLocalOf {
    error("AppContainer not provided. Ensure Application is configured properly.")
}

@Composable
fun ProvideAppContainer(
    container: AppContainer,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppContainer provides container,
        content = content
    )
}

/**
 * Convenience accessor for the current AppContainer from Compose context.
 */
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current
    val app = context.applicationContext as? JustAnimeApp
    return app?.container ?: LocalAppContainer.current
}
