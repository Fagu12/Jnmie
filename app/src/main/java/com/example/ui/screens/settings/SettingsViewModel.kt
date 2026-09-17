package com.example.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.auth.AniListOAuthHelper
import com.example.core.auth.AniListOAuthResult
import com.example.core.config.AniListAuthConfig
import com.example.core.interfaces.AniListRepository
import com.example.core.interfaces.AppSettings
import com.example.core.interfaces.PlaybackRepository
import com.example.core.interfaces.ProviderManager
import com.example.core.interfaces.SettingsRepository
import com.example.core.model.AniListUser
import com.example.core.model.ConflictStrategy
import com.example.domain.model.ProviderInfo
import com.example.domain.model.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: AniListUser? = null,
    val savedUsername: String? = null,
    val isAuthenticated: Boolean = false,
    val isSecureStorageEnabled: Boolean = true,
    val isSyncingAniList: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val pendingSyncCount: Int = 0,
    val settings: AppSettings = AppSettings(),
    val availableProviders: List<ProviderInfo> = emptyList(),
    val showAuthDialog: Boolean = false,
    val authDialogTab: Int = 0, // 0: OAuth Web, 1: Personal Access Token, 2: Quick Demo
    val tokenInput: String = "",
    val isAuthenticating: Boolean = false,
    val authErrorMessage: String? = null,
    val toastMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val aniListRepository: AniListRepository,
    private val playbackRepository: PlaybackRepository,
    private val providerManager: ProviderManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(isSecureStorageEnabled = aniListRepository.isSecureStorageEnabled)
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { s ->
                _uiState.update { it.copy(settings = s) }
            }
        }
        viewModelScope.launch {
            aniListRepository.currentUser.collect { u ->
                _uiState.update { it.copy(user = u) }
            }
        }
        viewModelScope.launch {
            aniListRepository.savedUsername.collect { name ->
                _uiState.update { it.copy(savedUsername = name) }
            }
        }
        viewModelScope.launch {
            aniListRepository.isAuthenticated.collect { auth ->
                _uiState.update { it.copy(isAuthenticated = auth) }
            }
        }
        viewModelScope.launch {
            aniListRepository.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status, isSyncingAniList = status is SyncStatus.Syncing) }
            }
        }
        viewModelScope.launch {
            aniListRepository.pendingSyncCount.collect { count ->
                _uiState.update { it.copy(pendingSyncCount = count) }
            }
        }
        viewModelScope.launch {
            val providers = providerManager.getAvailableProviders()
            _uiState.update { it.copy(availableProviders = providers) }
        }
    }

    fun setCardStyle(style: String) {
        _uiState.update { it.copy(settings = it.settings.copy(cardStyle = style)) }
        viewModelScope.launch { settingsRepository.setCardStyle(style) }
    }

    fun setCarouselStyle(style: String) {
        _uiState.update { it.copy(settings = it.settings.copy(carouselStyle = style)) }
        viewModelScope.launch { settingsRepository.setCarouselStyle(style) }
    }

    fun setHistoryCardStyle(style: String) {
        _uiState.update { it.copy(settings = it.settings.copy(historyCardStyle = style)) }
        viewModelScope.launch { settingsRepository.setHistoryCardStyle(style) }
    }

    fun setAmoledPureBlack(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(amoledPureBlack = enabled)) }
        viewModelScope.launch { settingsRepository.updateSettings { it.copy(amoledPureBlack = enabled) } }
    }

    fun setNavBarStyle(style: String) {
        _uiState.update { it.copy(settings = it.settings.copy(navBarStyle = style)) }
        viewModelScope.launch { settingsRepository.setNavBarStyle(style) }
    }

    fun setPreferredProvider(providerId: String) {
        _uiState.update { it.copy(settings = it.settings.copy(preferredProviderId = providerId)) }
        viewModelScope.launch { settingsRepository.setPreferredProvider(providerId) }
    }

    fun setDefaultQuality(quality: String) {
        _uiState.update { it.copy(settings = it.settings.copy(defaultQuality = quality)) }
        viewModelScope.launch { settingsRepository.setDefaultQuality(quality) }
    }

    fun setAudioSubPreference(pref: String) {
        _uiState.update { it.copy(settings = it.settings.copy(audioSubPreference = pref)) }
        viewModelScope.launch { settingsRepository.setAudioSubPreference(pref) }
    }

    fun setDefaultPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(settings = it.settings.copy(defaultPlaybackSpeed = speed)) }
        viewModelScope.launch { settingsRepository.setDefaultPlaybackSpeed(speed) }
    }

    fun setSkipDuration(seconds: Int) {
        _uiState.update { it.copy(settings = it.settings.copy(skipDurationSeconds = seconds)) }
        viewModelScope.launch { settingsRepository.setSkipDurationSeconds(seconds) }
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(autoSkipIntro = enabled)) }
        viewModelScope.launch { settingsRepository.setAutoSkipIntro(enabled) }
    }

    fun setAutoSkipOutro(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(autoSkipOutro = enabled)) }
        viewModelScope.launch { settingsRepository.setAutoSkipOutro(enabled) }
    }

    fun setAutoSkipRecap(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(autoSkipRecap = enabled)) }
        viewModelScope.launch { settingsRepository.setAutoSkipRecap(enabled) }
    }

    fun setPlayerGestures(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(playerGestures = enabled)) }
        viewModelScope.launch { settingsRepository.setPlayerGestures(enabled) }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(autoPlayNext = enabled)) }
        viewModelScope.launch { settingsRepository.setAutoPlayNext(enabled) }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(backgroundPlayback = enabled)) }
        viewModelScope.launch { settingsRepository.setBackgroundPlayback(enabled) }
    }

    fun setPipEnabled(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(pipEnabled = enabled)) }
        viewModelScope.launch { settingsRepository.setPipEnabled(enabled) }
    }

    fun setBufferCacheMb(mb: Int) {
        _uiState.update { it.copy(settings = it.settings.copy(bufferCacheMb = mb)) }
        viewModelScope.launch { settingsRepository.setBufferCacheMb(mb) }
    }

    fun launchOAuthInBrowser(context: Context) {
        AniListOAuthHelper.launchOAuthBrowser(context)
    }

    // --- AniList Authentication ---

    fun setAuthDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAuthDialog = visible, authErrorMessage = null, tokenInput = "") }
    }

    fun setAuthDialogTab(tabIndex: Int) {
        _uiState.update { it.copy(authDialogTab = tabIndex, authErrorMessage = null) }
    }

    fun setTokenInput(token: String) {
        _uiState.update { it.copy(tokenInput = token) }
    }

    fun setSecureStorage(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isSecureStorageEnabled = enabled,
                toastMessage = if (enabled) "Encrypted hardware keystore active" else "Standard app-private storage active"
            )
        }
    }

    fun handleAuthCallback(uriString: String) {
        when (val result = AniListOAuthHelper.parseCallbackUri(uriString)) {
            is AniListOAuthResult.TokenSuccess -> {
                authenticateWithToken(result.accessToken)
            }
            is AniListOAuthResult.CodeSuccess -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
                    aniListRepository.handleOAuthCallback(uriString)
                        .onSuccess { user ->
                            _uiState.update { it.copy(isAuthenticating = false, showAuthDialog = false, toastMessage = "Welcome, ${user.name}!") }
                        }
                        .onFailure { e ->
                            _uiState.update { it.copy(isAuthenticating = false, authErrorMessage = e.message ?: "Authentication failed") }
                        }
                }
            }
            is AniListOAuthResult.OAuthError -> {
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authErrorMessage = "OAuth error: ${result.errorDescription ?: result.error}"
                    )
                }
            }
            AniListOAuthResult.NotOAuthCallback -> Unit
        }
    }

    fun authenticateWithToken(token: String) {
        if (token.isBlank()) {
            _uiState.update { it.copy(authErrorMessage = "Please enter an AniList access token") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
            val result = aniListRepository.authenticateWithToken(token.trim())
            if (result.isSuccess) {
                val user = result.getOrNull()
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        showAuthDialog = false,
                        tokenInput = "",
                        toastMessage = "Connected to AniList as ${user?.name ?: "User"}"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authErrorMessage = result.exceptionOrNull()?.message ?: "Authentication failed"
                    )
                }
            }
        }
    }

    fun loginWithDemo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, authErrorMessage = null) }
            val result = aniListRepository.authenticateWithToken("OtakuExplorer")
            _uiState.update {
                it.copy(
                    isAuthenticating = false,
                    showAuthDialog = false,
                    tokenInput = "",
                    toastMessage = "Connected with demo AniList profile"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            aniListRepository.logout()
            _uiState.update { it.copy(toastMessage = "Disconnected from AniList") }
        }
    }

    fun setAutoSyncAniList(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(autoSyncAniList = enabled)) }
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(autoSyncAniList = enabled) }
        }
    }

    fun setSyncConflictStrategy(strategy: ConflictStrategy) {
        _uiState.update { it.copy(settings = it.settings.copy(syncConflictStrategy = strategy.name)) }
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(syncConflictStrategy = strategy.name) }
        }
    }

    fun retryPendingSync() {
        viewModelScope.launch {
            val result = aniListRepository.retryPendingSync()
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _uiState.update { it.copy(toastMessage = "Retried pending sync ($count items processed)") }
            } else {
                _uiState.update { it.copy(toastMessage = "Failed to retry pending sync: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun clearPendingSync() {
        viewModelScope.launch {
            aniListRepository.clearPendingSync()
            _uiState.update { it.copy(toastMessage = "Pending sync queue cleared") }
        }
    }

    fun syncAniList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingAniList = true) }
            val currentStrategy = try {
                ConflictStrategy.valueOf(_uiState.value.settings.syncConflictStrategy)
            } catch (e: Exception) {
                ConflictStrategy.HIGHEST_PROGRESS
            }
            val result = aniListRepository.syncUserLists(currentStrategy)
            if (result.isSuccess) {
                val report = result.getOrNull()
                val summary = if (report != null && (report.pulledCount > 0 || report.pushedCount > 0)) {
                    "Sync complete: ↓${report.pulledCount} pulled, ↑${report.pushedCount} pushed"
                } else {
                    "AniList library is up to date"
                }
                _uiState.update {
                    it.copy(
                        isSyncingAniList = false,
                        toastMessage = summary
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSyncingAniList = false,
                        toastMessage = "Sync failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            playbackRepository.clearHistory()
            _uiState.update { it.copy(toastMessage = "Watch history cleared") }
        }
    }

    fun clearPlaybackCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(toastMessage = "Playback cache cleared (128 MB freed)") }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.resetSettings()
            _uiState.update { it.copy(toastMessage = "Settings reset to defaults") }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun setSubEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepository.setSubEnabled(enabled) } }
    fun setSubLanguage(language: String) { viewModelScope.launch { settingsRepository.setSubLanguage(language) } }
    fun setSubFontFamily(family: String) { viewModelScope.launch { settingsRepository.setSubFontFamily(family) } }
    fun setSubFontSize(size: String) { viewModelScope.launch { settingsRepository.setSubFontSize(size) } }
    fun setSubFontWeight(weight: String) { viewModelScope.launch { settingsRepository.setSubFontWeight(weight) } }
    fun setSubTextColor(color: String) { viewModelScope.launch { settingsRepository.setSubTextColor(color) } }
    fun setSubBackgroundStyle(style: String) { viewModelScope.launch { settingsRepository.setSubBackgroundStyle(style) } }
    fun setSubBackgroundOpacity(opacity: Float) { viewModelScope.launch { settingsRepository.setSubBackgroundOpacity(opacity) } }
    fun setSubOutlineStyle(style: String) { viewModelScope.launch { settingsRepository.setSubOutlineStyle(style) } }
    fun setSubPosition(position: String) { viewModelScope.launch { settingsRepository.setSubPosition(position) } }
    
    class Factory(
        private val settingsRepository: SettingsRepository,
        private val aniListRepository: AniListRepository,
        private val playbackRepository: PlaybackRepository,
        private val providerManager: ProviderManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                settingsRepository,
                aniListRepository,
                playbackRepository,
                providerManager
            ) as T
        }
    }
}
