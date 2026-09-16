package com.example.core.config

/**
 * AniList OAuth2 Configuration & Placeholders.
 *
 * Documentation for connecting your AniList API Application:
 * 1. Visit AniList Developer settings: https://anilist.co/settings/developer
 * 2. Click "Create New Client"
 * 3. Set the Name to "JustAnime" (or your custom client name)
 * 4. Set the Redirect URI to:
 *      - Primary: `animex://anilist-auth`
 *      - Alternative: `justanime://anilist-auth`
 * 5. Provide your Client ID and Client Secret in your build configuration or `.env`.
 *
 * Note: Never hardcode sensitive production secrets into source code.
 */
object AniListAuthConfig {
    /**
     * Configuration placeholder for the AniList OAuth Client ID.
     * Developers can replace this placeholder with their registered AniList Client ID
     * or inject it at build time via BuildConfig / .env.
     */
    const val CLIENT_ID = "YOUR_ANILIST_CLIENT_ID" // Replace with real Client ID
    const val CLIENT_SECRET = "YOUR_ANILIST_CLIENT_SECRET" // Replace with real Client Secret
    
    // Check if the app is running in AI Studio Preview environment
    // In a real app, this might be handled via build flavors
    const val IS_AI_STUDIO_PREVIEW = true

    /**
     * Primary registered deep link callback scheme & host for OAuth redirection.
     * 
     * In the AI Studio Preview environment, deep links might not function properly 
     * because the app runs inside a browser emulator which cannot redirect to app schemas.
     */
    const val REDIRECT_URI: String = "animex://anilist-auth"

    /**
     * AniList OAuth Endpoints
     */
    const val OAUTH_AUTHORIZE_URL: String = "https://anilist.co/api/v2/oauth/authorize"
    const val OAUTH_TOKEN_URL: String = "https://anilist.co/api/v2/oauth/token"
    const val DEVELOPER_SETTINGS_URL: String = "https://anilist.co/settings/developer"

    /**
     * Constructs the Authorization Code Grant authorization URL.
     * When completed, AniList redirects to [redirectUri] with `?code=...`.
     */
    fun buildCodeAuthUrl(
        clientId: String = CLIENT_ID,
        redirectUri: String = REDIRECT_URI
    ): String {
        return "$OAUTH_AUTHORIZE_URL?client_id=$clientId&redirect_uri=$redirectUri&response_type=code"
    }

    /**
     * Checks if the configured Client ID is still the default placeholder.
     */
    fun isPlaceholderClientId(clientId: String = CLIENT_ID): Boolean {
        return clientId.isBlank() || clientId == "YOUR_ANILIST_CLIENT_ID" || clientId == "CLIENT_ID_PLACEHOLDER"
    }
}
