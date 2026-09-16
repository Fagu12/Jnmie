package com.example.ui.util

import com.example.core.model.AniListUser

/**
 * Resolves the display name for greetings and user-facing account labels.
 * Strictly adheres to the priority:
 * 1. AniList username/handle if logged in (non-blank)
 * 2. Locally saved AniList username if available (non-blank)
 * 3. "User" fallback
 *
 * Never uses personal/real name, profile full name, email address, Android account name,
 * Google account name, or device owner name.
 */
fun getDisplayUsername(user: AniListUser?, savedUsername: String? = null): String {
    val liveName = user?.name?.trim()
    if (!liveName.isNullOrBlank() && !liveName.equals("Guest", ignoreCase = true)) {
        return liveName
    }
    val saved = savedUsername?.trim()
    if (!saved.isNullOrBlank() && !saved.equals("Guest", ignoreCase = true)) {
        return saved
    }
    return "User"
}

/**
 * Returns the Home screen greeting based strictly on the authenticated AniList handle.
 * - "Welcome back, {AniList username}" when connected
 * - "Welcome back, User" when no AniList account is connected
 */
fun getGreetingText(user: AniListUser?, savedUsername: String? = null): String {
    val username = getDisplayUsername(user, savedUsername)
    return "Welcome back, $username"
}
