package com.example.core.config

import androidx.compose.ui.graphics.Color

/**
 * Centralized Application Configuration & Branding.
 * All application identity, naming, metadata, brand styling, and feature flags
 * are centralized here so branding can be seamlessly modified in one place.
 */
object AppConfig {
    // Application Identity & Branding
    const val APP_NAME = "Just Anime"
    const val APP_TAGLINE = "Your gateway to anime discovery and streaming"
    const val APP_VERSION = "1.0.0"
    const val APP_BUILD_NAME = "Foundation Release"
    const val APPLICATION_ID = "com.aistudio.justanime.wqvz"

    // Feature Flags
    /**
     * Set to false for the Foundation phase.
     * When false, the UI displays the structured streaming player staging screen,
     * awaiting the dedicated streaming implementation.
     */
    const val IS_STREAMING_ENABLED = false
    const val IS_ANILIST_SYNC_ENABLED = true
    const val IS_EXTENSIONS_ENABLED = true
    const val IS_OFFLINE_CACHE_ENABLED = true

    // Remote Endpoints
    const val ANILIST_GRAPHQL_ENDPOINT = "https://graphql.anilist.co"
    const val DEFAULT_USER_AGENT = "Mozilla/5.0 JustAnime/1.0 (Linux; Android)"
    const val NETWORK_TIMEOUT_SECONDS = 30L

    // Brand Palette & Styling
    val BRAND_PRIMARY_COLOR = Color(0xFF8F82FE)
    val BRAND_SECONDARY_COLOR = Color(0xFF6C5CE7)
    val BRAND_ACCENT_COLOR = Color(0xFF54A0FF)
    val BRAND_SURFACE_DARK = Color(0xFF13141F)
    val BRAND_BACKGROUND_DARK = Color(0xFF090A10)

    // Community & Support Links
    const val GITHUB_REPO_URL = "https://github.com/aistudio/just-anime"
    const val DISCORD_COMMUNITY_URL = "https://discord.gg/anime"
    const val SUPPORT_EMAIL = "support@justanime.app"
    const val PRIVACY_POLICY_URL = "https://justanime.app/privacy"
    const val TERMS_OF_SERVICE_URL = "https://justanime.app/terms"
}
