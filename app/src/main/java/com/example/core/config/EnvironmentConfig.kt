package com.example.core.config

import com.example.BuildConfig

/**
 * Environment & Configuration Handler.
 * Supports switching between development, staging, and production environments,
 * and reading environment properties or fallback values safely.
 */
enum class AppEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

data class EnvironmentConfig(
    val environment: AppEnvironment = if (BuildConfig.DEBUG) AppEnvironment.DEVELOPMENT else AppEnvironment.PRODUCTION,
    val apiBaseUrl: String = AppConfig.ANILIST_GRAPHQL_ENDPOINT,
    val isLoggingEnabled: Boolean = BuildConfig.DEBUG,
    val connectionTimeoutMs: Long = AppConfig.NETWORK_TIMEOUT_SECONDS * 1000L,
    val readTimeoutMs: Long = AppConfig.NETWORK_TIMEOUT_SECONDS * 1000L
) {
    companion object {
        fun default(): EnvironmentConfig = EnvironmentConfig()
    }
}
