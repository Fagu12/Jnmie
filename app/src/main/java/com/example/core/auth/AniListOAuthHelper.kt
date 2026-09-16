package com.example.core.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.core.config.AniListAuthConfig

/**
 * Result of parsing an AniList OAuth redirect URI.
 */
sealed interface AniListOAuthResult {
    data class TokenSuccess(
        val accessToken: String,
        val tokenType: String = "Bearer",
        val expiresInSeconds: Long? = null
    ) : AniListOAuthResult

    data class CodeSuccess(
        val authCode: String
    ) : AniListOAuthResult

    data class OAuthError(
        val error: String,
        val errorDescription: String? = null
    ) : AniListOAuthResult

    object NotOAuthCallback : AniListOAuthResult
}

/**
 * Helper utility for building AniList OAuth intents and parsing deep link callback URIs.
 */
object AniListOAuthHelper {
    private const val TAG = "AniListOAuthHelper"

    /**
     * Launches the system browser or custom tab with the AniList OAuth authorization page.
     */
    fun launchOAuthBrowser(
        context: Context,
        clientId: String = AniListAuthConfig.CLIENT_ID,
        redirectUri: String = AniListAuthConfig.REDIRECT_URI
    ) {
        // First log only sanitized configuration before launching
        Log.i(TAG, "Preparing AniList OAuth Authorization Request")
        Log.i(TAG, "Authorization Endpoint: ${AniListAuthConfig.OAUTH_AUTHORIZE_URL}")
        Log.i(TAG, "Client ID: ${if (clientId.length > 4) clientId.take(4) + "..." else "INVALID"}")
        Log.i(TAG, "Redirect URI: $redirectUri")
        Log.i(TAG, "Response Type: code")
        
        val authUrl = AniListAuthConfig.buildCodeAuthUrl(clientId, redirectUri)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch browser for AniList OAuth: ${e.message}", e)
        }
    }

    /**
     * Parses an incoming intent or Deep Link URI for AniList OAuth responses.
     */
    fun parseCallbackUri(uriString: String?): AniListOAuthResult {
        if (uriString.isNullOrBlank()) return AniListOAuthResult.NotOAuthCallback
        return try {
            parseCallbackUri(Uri.parse(uriString))
        } catch (e: Exception) {
            AniListOAuthResult.NotOAuthCallback
        }
    }

    /**
     * Parses an incoming intent or Deep Link URI for AniList OAuth responses.
     */
    fun parseCallbackUri(uri: Uri?): AniListOAuthResult {
        if (uri == null) return AniListOAuthResult.NotOAuthCallback

        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()

        // Check if URI matches registered redirect URI schemas (e.g. animex://anilist-auth or justanime://anilist-auth)
        val isAniListScheme = (scheme == "animex" || scheme == "justanime" || scheme == "https") &&
                (host == "anilist-auth" || host == "anilist.co" || uri.path?.contains("anilist-auth") == true)

        if (!isAniListScheme) {
            return AniListOAuthResult.NotOAuthCallback
        }

        // 1. Check for error parameters (either in query or fragment)
        val queryError = uri.getQueryParameter("error")
        if (!queryError.isNullOrBlank()) {
            val errorDesc = uri.getQueryParameter("error_description")
            return AniListOAuthResult.OAuthError(queryError, errorDesc)
        }

        // 2. Check for Authorization Code Grant (in query parameter `code`)
        val code = uri.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
            return AniListOAuthResult.CodeSuccess(code)
        }

        // 3. Check for Implicit Grant Token in Fragment (e.g., `#access_token=...&token_type=Bearer&expires_in=...`)
        val fragment = uri.fragment
        if (!fragment.isNullOrBlank()) {
            val params = parseFragmentParams(fragment)
            val errorInFragment = params["error"]
            if (!errorInFragment.isNullOrBlank()) {
                val errorDesc = params["error_description"]
                return AniListOAuthResult.OAuthError(errorInFragment, errorDesc)
            }

            val token = params["access_token"]
            if (!token.isNullOrBlank()) {
                val tokenType = params["token_type"] ?: "Bearer"
                val expiresIn = params["expires_in"]?.toLongOrNull()
                return AniListOAuthResult.TokenSuccess(
                    accessToken = token,
                    tokenType = tokenType,
                    expiresInSeconds = expiresIn
                )
            }
        }

        return AniListOAuthResult.NotOAuthCallback
    }

    /**
     * Parses key=value pairs inside a URI fragment (#key1=val1&key2=val2).
     */
    private fun parseFragmentParams(fragment: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val pairs = fragment.split("&")
        for (pair in pairs) {
            val parts = pair.split("=", limit = 2)
            if (parts.size == 2) {
                val key = Uri.decode(parts[0].trim())
                val value = Uri.decode(parts[1].trim())
                map[key] = value
            }
        }
        return map
    }
}
