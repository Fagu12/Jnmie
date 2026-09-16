package com.example.core.config

/**
 * Centralized Application Constants.
 * Holds immutable architectural keys, database names, preferences identifiers,
 * navigation routes, and playback timing defaults.
 */
object AppConstants {
    // Database & Storage
    const val DATABASE_NAME = "just_anime.db"
    const val DATASTORE_SETTINGS_NAME = "user_settings"

    // Navigation Routes
    const val ROUTE_HOME = "home"
    const val ROUTE_EXPLORE = "explore"
    const val ROUTE_LIBRARY = "library"
    const val ROUTE_EXTENSIONS = "extensions"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_SEARCH = "search"
    const val ROUTE_DETAILS = "details/{animeId}"
    const val ROUTE_PLAYER = "player/{animeId}/{episodeNumber}"

    // Route Argument Keys
    const val ARG_ANIME_ID = "animeId"
    const val ARG_EPISODE_NUMBER = "episodeNumber"

    // Default Playback & Preferences Values
    const val DEFAULT_AUTO_SKIP_INTRO_SECONDS = 85L
    const val DEFAULT_AUTO_SKIP_OUTRO_SECONDS = 90L
    const val DEFAULT_SEEK_INCREMENT_MS = 10_000L
    const val DEFAULT_BUFFER_CACHE_MB = 128
    const val DEFAULT_PLAYBACK_SPEED = 1.0f

    // UI & Grid Standards
    const val TOUCH_TARGET_MIN_DP = 48
    const val CAROUSEL_CYCLE_DELAY_MS = 6_000L
    const val SNACKBAR_DURATION_MS = 3_000L
}
