package com.example.data.repository

import com.example.domain.model.AppSettings
import com.example.domain.repository.SettingsRepository
import com.example.data.local.preferences.UserPreferencesDataStore
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val preferencesDataStore: UserPreferencesDataStore
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> = preferencesDataStore.settingsFlow

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        preferencesDataStore.updateSettings(transform)
    }

    override suspend fun setAutoSkipIntro(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(autoSkipIntro = enabled) }
    }

    override suspend fun setAutoSkipOutro(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(autoSkipOutro = enabled) }
    }

    override suspend fun setAutoSkipRecap(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(autoSkipRecap = enabled) }
    }

    override suspend fun setDefaultQuality(quality: String) {
        preferencesDataStore.updateSettings { it.copy(defaultQuality = quality) }
    }

    override suspend fun setAudioSubPreference(pref: String) {
        preferencesDataStore.updateSettings { it.copy(audioSubPreference = pref) }
    }

    override suspend fun setSkipDurationSeconds(seconds: Int) {
        preferencesDataStore.updateSettings { it.copy(skipDurationSeconds = seconds) }
    }

    override suspend fun setPlayerGestures(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(playerGestures = enabled) }
    }

    override suspend fun setPlayerTheme(theme: String) {
        preferencesDataStore.updateSettings { it.copy(playerTheme = theme) }
    }

    override suspend fun setDefaultPlaybackSpeed(speed: Float) {
        preferencesDataStore.updateSettings { it.copy(defaultPlaybackSpeed = speed) }
    }

    override suspend fun setAutoPlayNext(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(autoPlayNext = enabled) }
    }

    override suspend fun setBackgroundPlayback(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(backgroundPlayback = enabled) }
    }

    override suspend fun setPipEnabled(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(pipEnabled = enabled) }
    }

    override suspend fun setBufferCacheMb(mb: Int) {
        preferencesDataStore.updateSettings { it.copy(bufferCacheMb = mb) }
    }

    override suspend fun setHardwareAcceleration(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(hardwareAcceleration = enabled) }
    }

    override suspend fun setHideAdultContent(hide: Boolean) {
        preferencesDataStore.updateSettings { it.copy(hideAdultContent = hide) }
    }

    override suspend fun setUnifiedLibrary(unified: Boolean) {
        preferencesDataStore.updateSettings { it.copy(unifiedLibrary = unified) }
    }

    override suspend fun setCardStyle(style: String) {
        preferencesDataStore.updateSettings { it.copy(cardStyle = style) }
    }

    override suspend fun setHistoryCardStyle(style: String) {
        preferencesDataStore.updateSettings { it.copy(historyCardStyle = style) }
    }

    override suspend fun setCarouselStyle(style: String) {
        preferencesDataStore.updateSettings { it.copy(carouselStyle = style) }
    }

    override suspend fun setNavBarStyle(style: String) {
        preferencesDataStore.updateSettings { it.copy(navBarStyle = style) }
    }

    override suspend fun setNavBarMargin(margin: Int) {
        preferencesDataStore.updateSettings { it.copy(navBarMargin = margin) }
    }

    override suspend fun setPreferredProvider(providerId: String) {
        preferencesDataStore.updateSettings { it.copy(preferredProviderId = providerId) }
    }

    override suspend fun setSyncConflictStrategy(strategy: String) {
        preferencesDataStore.updateSettings { it.copy(syncConflictStrategy = strategy) }
    }

    override suspend fun setAutoSyncAniList(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(autoSyncAniList = enabled) }
    }

    override suspend fun resetSettings() {
        preferencesDataStore.resetSettings()
    }
}

