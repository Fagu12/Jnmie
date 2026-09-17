package com.example.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesDataStore(private val context: Context) {

    private object PreferencesKeys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val AUTO_SKIP_INTRO = booleanPreferencesKey("auto_skip_intro")
        val AUTO_SKIP_OUTRO = booleanPreferencesKey("auto_skip_outro")
        val AUTO_SKIP_RECAP = booleanPreferencesKey("auto_skip_recap")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val AUDIO_SUB_PREF = stringPreferencesKey("audio_sub_pref")
        val SKIP_DURATION_SEC = intPreferencesKey("skip_duration_sec")
        val PLAYER_GESTURES = booleanPreferencesKey("player_gestures")
        val PLAYER_THEME = stringPreferencesKey("player_theme")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val BUFFER_CACHE_MB = intPreferencesKey("buffer_cache_mb")
        val AUTO_UPDATE_EXT = booleanPreferencesKey("auto_update_ext")
        val HW_ACCEL = booleanPreferencesKey("hw_accel")
        val HIDE_ADULT = booleanPreferencesKey("hide_adult")
        val UNIFIED_LIBRARY = booleanPreferencesKey("unified_library")
        val ASK_TRACKING = booleanPreferencesKey("ask_tracking")
        val SHOW_COMMUNITY_REC = booleanPreferencesKey("show_community_rec")
        val CARD_STYLE = stringPreferencesKey("card_style")
        val HISTORY_CARD_STYLE = stringPreferencesKey("history_card_style")
        val CAROUSEL_STYLE = stringPreferencesKey("carousel_style")
        val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
        val NAV_BAR_MARGIN = intPreferencesKey("nav_bar_margin")
        val PREFERRED_PROVIDER = stringPreferencesKey("preferred_provider")
        val SYNC_CONFLICT_STRATEGY = stringPreferencesKey("sync_conflict_strategy")
        val AUTO_SYNC_ANILIST = booleanPreferencesKey("auto_sync_anilist")
        val LAST_SYNCED_TIMESTAMP = longPreferencesKey("last_synced_timestamp")
        val ANILIST_AUTH_TOKEN = stringPreferencesKey("anilist_auth_token")
        val ANILIST_USERNAME = stringPreferencesKey("anilist_username")
        
        val SUB_ENABLED = booleanPreferencesKey("sub_enabled")
        val SUB_LANGUAGE = stringPreferencesKey("sub_language")
        val SUB_FONT_FAMILY = stringPreferencesKey("sub_font_family")
        val SUB_FONT_SIZE = stringPreferencesKey("sub_font_size")
        val SUB_FONT_WEIGHT = stringPreferencesKey("sub_font_weight")
        val SUB_TEXT_COLOR = stringPreferencesKey("sub_text_color")
        val SUB_BACKGROUND_STYLE = stringPreferencesKey("sub_background_style")
        val SUB_BACKGROUND_OPACITY = floatPreferencesKey("sub_background_opacity")
        val SUB_OUTLINE_STYLE = stringPreferencesKey("sub_outline_style")
        val SUB_POSITION = stringPreferencesKey("sub_position")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            darkTheme = preferences[PreferencesKeys.DARK_THEME] ?: true,
            amoledPureBlack = preferences[PreferencesKeys.AMOLED_BLACK] ?: true,
            autoSkipIntro = preferences[PreferencesKeys.AUTO_SKIP_INTRO] ?: true,
            autoSkipOutro = preferences[PreferencesKeys.AUTO_SKIP_OUTRO] ?: true,
            autoSkipRecap = preferences[PreferencesKeys.AUTO_SKIP_RECAP] ?: true,
            defaultQuality = preferences[PreferencesKeys.DEFAULT_QUALITY] ?: "1080p",
            audioSubPreference = preferences[PreferencesKeys.AUDIO_SUB_PREF] ?: "Sub",
            skipDurationSeconds = preferences[PreferencesKeys.SKIP_DURATION_SEC] ?: 85,
            playerGestures = preferences[PreferencesKeys.PLAYER_GESTURES] ?: true,
            playerTheme = preferences[PreferencesKeys.PLAYER_THEME] ?: "Dark OLED",
            defaultPlaybackSpeed = preferences[PreferencesKeys.DEFAULT_SPEED] ?: 1.0f,
            autoPlayNext = preferences[PreferencesKeys.AUTO_PLAY_NEXT] ?: true,
            backgroundPlayback = preferences[PreferencesKeys.BACKGROUND_PLAYBACK] ?: false,
            pipEnabled = preferences[PreferencesKeys.PIP_ENABLED] ?: true,
            bufferCacheMb = preferences[PreferencesKeys.BUFFER_CACHE_MB] ?: 128,
            autoUpdateExtensions = preferences[PreferencesKeys.AUTO_UPDATE_EXT] ?: true,
            hardwareAcceleration = preferences[PreferencesKeys.HW_ACCEL] ?: true,
            hideAdultContent = preferences[PreferencesKeys.HIDE_ADULT] ?: true,
            unifiedLibrary = preferences[PreferencesKeys.UNIFIED_LIBRARY] ?: true,
            askForTrackingPermission = preferences[PreferencesKeys.ASK_TRACKING] ?: true,
            showCommunityRecommendations = preferences[PreferencesKeys.SHOW_COMMUNITY_REC] ?: true,
            cardStyle = preferences[PreferencesKeys.CARD_STYLE] ?: "Saikou",
            historyCardStyle = preferences[PreferencesKeys.HISTORY_CARD_STYLE] ?: "Frosted Glass",
            carouselStyle = preferences[PreferencesKeys.CAROUSEL_STYLE] ?: "Classic",
            navBarStyle = preferences[PreferencesKeys.NAV_BAR_STYLE] ?: "Dynamic Pill",
            navBarMargin = preferences[PreferencesKeys.NAV_BAR_MARGIN] ?: 32,
            preferredProviderId = preferences[PreferencesKeys.PREFERRED_PROVIDER] ?: "core_anilist",
            syncConflictStrategy = preferences[PreferencesKeys.SYNC_CONFLICT_STRATEGY] ?: "HIGHEST_PROGRESS",
            autoSyncAniList = preferences[PreferencesKeys.AUTO_SYNC_ANILIST] ?: true,
            lastSyncedTimestamp = preferences[PreferencesKeys.LAST_SYNCED_TIMESTAMP] ?: 0L,
            subEnabled = preferences[PreferencesKeys.SUB_ENABLED] ?: true,
            subLanguage = preferences[PreferencesKeys.SUB_LANGUAGE] ?: "English",
            subFontFamily = preferences[PreferencesKeys.SUB_FONT_FAMILY] ?: "Default",
            subFontSize = preferences[PreferencesKeys.SUB_FONT_SIZE] ?: "Medium",
            subFontWeight = preferences[PreferencesKeys.SUB_FONT_WEIGHT] ?: "Normal",
            subTextColor = preferences[PreferencesKeys.SUB_TEXT_COLOR] ?: "White",
            subBackgroundStyle = preferences[PreferencesKeys.SUB_BACKGROUND_STYLE] ?: "None",
            subBackgroundOpacity = preferences[PreferencesKeys.SUB_BACKGROUND_OPACITY] ?: 0.5f,
            subOutlineStyle = preferences[PreferencesKeys.SUB_OUTLINE_STYLE] ?: "Outline",
            subPosition = preferences[PreferencesKeys.SUB_POSITION] ?: "Bottom"
        )
    }

    val aniListTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ANILIST_AUTH_TOKEN]
    }

    val aniListUsernameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ANILIST_USERNAME]
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { preferences ->
            val current = AppSettings(
                darkTheme = preferences[PreferencesKeys.DARK_THEME] ?: true,
                amoledPureBlack = preferences[PreferencesKeys.AMOLED_BLACK] ?: true,
                autoSkipIntro = preferences[PreferencesKeys.AUTO_SKIP_INTRO] ?: true,
                autoSkipOutro = preferences[PreferencesKeys.AUTO_SKIP_OUTRO] ?: true,
                autoSkipRecap = preferences[PreferencesKeys.AUTO_SKIP_RECAP] ?: true,
                defaultQuality = preferences[PreferencesKeys.DEFAULT_QUALITY] ?: "1080p",
                audioSubPreference = preferences[PreferencesKeys.AUDIO_SUB_PREF] ?: "Sub",
                skipDurationSeconds = preferences[PreferencesKeys.SKIP_DURATION_SEC] ?: 85,
                playerGestures = preferences[PreferencesKeys.PLAYER_GESTURES] ?: true,
                playerTheme = preferences[PreferencesKeys.PLAYER_THEME] ?: "Dark OLED",
                defaultPlaybackSpeed = preferences[PreferencesKeys.DEFAULT_SPEED] ?: 1.0f,
                autoPlayNext = preferences[PreferencesKeys.AUTO_PLAY_NEXT] ?: true,
                backgroundPlayback = preferences[PreferencesKeys.BACKGROUND_PLAYBACK] ?: false,
                pipEnabled = preferences[PreferencesKeys.PIP_ENABLED] ?: true,
                bufferCacheMb = preferences[PreferencesKeys.BUFFER_CACHE_MB] ?: 128,
                autoUpdateExtensions = preferences[PreferencesKeys.AUTO_UPDATE_EXT] ?: true,
                hardwareAcceleration = preferences[PreferencesKeys.HW_ACCEL] ?: true,
                hideAdultContent = preferences[PreferencesKeys.HIDE_ADULT] ?: true,
                unifiedLibrary = preferences[PreferencesKeys.UNIFIED_LIBRARY] ?: true,
                askForTrackingPermission = preferences[PreferencesKeys.ASK_TRACKING] ?: true,
                showCommunityRecommendations = preferences[PreferencesKeys.SHOW_COMMUNITY_REC] ?: true,
                cardStyle = preferences[PreferencesKeys.CARD_STYLE] ?: "Saikou",
                historyCardStyle = preferences[PreferencesKeys.HISTORY_CARD_STYLE] ?: "Frosted Glass",
                carouselStyle = preferences[PreferencesKeys.CAROUSEL_STYLE] ?: "Classic",
                navBarStyle = preferences[PreferencesKeys.NAV_BAR_STYLE] ?: "Dynamic Pill",
                navBarMargin = preferences[PreferencesKeys.NAV_BAR_MARGIN] ?: 32,
                preferredProviderId = preferences[PreferencesKeys.PREFERRED_PROVIDER] ?: "core_anilist",
                syncConflictStrategy = preferences[PreferencesKeys.SYNC_CONFLICT_STRATEGY] ?: "HIGHEST_PROGRESS",
                autoSyncAniList = preferences[PreferencesKeys.AUTO_SYNC_ANILIST] ?: true,
                lastSyncedTimestamp = preferences[PreferencesKeys.LAST_SYNCED_TIMESTAMP] ?: 0L
            )
            val updated = transform(current)
            preferences[PreferencesKeys.DARK_THEME] = updated.darkTheme
            preferences[PreferencesKeys.AMOLED_BLACK] = updated.amoledPureBlack
            preferences[PreferencesKeys.AUTO_SKIP_INTRO] = updated.autoSkipIntro
            preferences[PreferencesKeys.AUTO_SKIP_OUTRO] = updated.autoSkipOutro
            preferences[PreferencesKeys.AUTO_SKIP_RECAP] = updated.autoSkipRecap
            preferences[PreferencesKeys.DEFAULT_QUALITY] = updated.defaultQuality
            preferences[PreferencesKeys.AUDIO_SUB_PREF] = updated.audioSubPreference
            preferences[PreferencesKeys.SKIP_DURATION_SEC] = updated.skipDurationSeconds
            preferences[PreferencesKeys.PLAYER_GESTURES] = updated.playerGestures
            preferences[PreferencesKeys.PLAYER_THEME] = updated.playerTheme
            preferences[PreferencesKeys.DEFAULT_SPEED] = updated.defaultPlaybackSpeed
            preferences[PreferencesKeys.AUTO_PLAY_NEXT] = updated.autoPlayNext
            preferences[PreferencesKeys.BACKGROUND_PLAYBACK] = updated.backgroundPlayback
            preferences[PreferencesKeys.PIP_ENABLED] = updated.pipEnabled
            preferences[PreferencesKeys.BUFFER_CACHE_MB] = updated.bufferCacheMb
            preferences[PreferencesKeys.AUTO_UPDATE_EXT] = updated.autoUpdateExtensions
            preferences[PreferencesKeys.HW_ACCEL] = updated.hardwareAcceleration
            preferences[PreferencesKeys.HIDE_ADULT] = updated.hideAdultContent
            preferences[PreferencesKeys.UNIFIED_LIBRARY] = updated.unifiedLibrary
            preferences[PreferencesKeys.ASK_TRACKING] = updated.askForTrackingPermission
            preferences[PreferencesKeys.SHOW_COMMUNITY_REC] = updated.showCommunityRecommendations
            preferences[PreferencesKeys.CARD_STYLE] = updated.cardStyle
            preferences[PreferencesKeys.HISTORY_CARD_STYLE] = updated.historyCardStyle
            preferences[PreferencesKeys.CAROUSEL_STYLE] = updated.carouselStyle
            preferences[PreferencesKeys.NAV_BAR_STYLE] = updated.navBarStyle
            preferences[PreferencesKeys.NAV_BAR_MARGIN] = updated.navBarMargin
            preferences[PreferencesKeys.PREFERRED_PROVIDER] = updated.preferredProviderId
            preferences[PreferencesKeys.SYNC_CONFLICT_STRATEGY] = updated.syncConflictStrategy
            preferences[PreferencesKeys.AUTO_SYNC_ANILIST] = updated.autoSyncAniList
            preferences[PreferencesKeys.LAST_SYNCED_TIMESTAMP] = updated.lastSyncedTimestamp
        }
    }

    suspend fun resetSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.AUTO_SKIP_INTRO)
            preferences.remove(PreferencesKeys.AUTO_SKIP_OUTRO)
            preferences.remove(PreferencesKeys.AUTO_SKIP_RECAP)
            preferences.remove(PreferencesKeys.DEFAULT_QUALITY)
            preferences.remove(PreferencesKeys.AUDIO_SUB_PREF)
            preferences.remove(PreferencesKeys.SKIP_DURATION_SEC)
            preferences.remove(PreferencesKeys.PLAYER_GESTURES)
            preferences.remove(PreferencesKeys.PLAYER_THEME)
            preferences.remove(PreferencesKeys.DEFAULT_SPEED)
            preferences.remove(PreferencesKeys.AUTO_PLAY_NEXT)
            preferences.remove(PreferencesKeys.BACKGROUND_PLAYBACK)
            preferences.remove(PreferencesKeys.PIP_ENABLED)
            preferences.remove(PreferencesKeys.BUFFER_CACHE_MB)
            preferences.remove(PreferencesKeys.AUTO_UPDATE_EXT)
            preferences.remove(PreferencesKeys.CARD_STYLE)
            preferences.remove(PreferencesKeys.HISTORY_CARD_STYLE)
            preferences.remove(PreferencesKeys.CAROUSEL_STYLE)
            preferences.remove(PreferencesKeys.NAV_BAR_STYLE)
        }
    }

    suspend fun saveAniListToken(token: String?) {
        context.dataStore.edit { preferences ->
            if (token == null) {
                preferences.remove(PreferencesKeys.ANILIST_AUTH_TOKEN)
            } else {
                preferences[PreferencesKeys.ANILIST_AUTH_TOKEN] = token
            }
        }
    }

    suspend fun saveAniListUser(token: String?, username: String?) {
        context.dataStore.edit { preferences ->
            // Always remove sensitive token from plain preferences if legacy key existed
            preferences.remove(PreferencesKeys.ANILIST_AUTH_TOKEN)
            if (username == null) {
                preferences.remove(PreferencesKeys.ANILIST_USERNAME)
            } else {
                preferences[PreferencesKeys.ANILIST_USERNAME] = username
            }
        }
    }
}
