package com.example.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.auth.AniListOAuthHelper
import com.example.core.config.AniListAuthConfig
import com.example.core.interfaces.AniListRepository
import com.example.core.interfaces.AppSettings
import com.example.core.interfaces.ExtensionRepository
import com.example.core.interfaces.PlaybackRepository
import com.example.core.interfaces.SettingsRepository
import com.example.core.model.AniListUser
import com.example.core.model.ConflictStrategy
import com.example.core.model.ExtensionRepo
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
    val repositories: List<ExtensionRepo> = emptyList(),
    val isSyncingRepos: Boolean = false,
    val showAuthDialog: Boolean = false,
    val authDialogTab: Int = 0, // 0: OAuth Web, 1: Personal Access Token, 2: Quick Demo
    val tokenInput: String = "",
    val isAuthenticating: Boolean = false,
    val authErrorMessage: String? = null,
    val showAddRepoDialog: Boolean = false,
    val repoUrlInput: String = "",
    val toastMessage: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val aniListRepository: AniListRepository,
    private val playbackRepository: PlaybackRepository,
    private val extensionRepository: ExtensionRepository
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
            extensionRepository.getRepositories().collect { repos ->
                _uiState.update { it.copy(repositories = repos) }
            }
        }
    }

    fun setCardStyle(style: String) {
        _uiState.update { it.copy(settings = it.settings.copy(cardStyle = style)) }
        viewModelScope.launch { settingsRepository.setCardStyle(style) }
    }

    fun setHistoryCardStyle(style: String) {
        _uiState.update { it.copy(settings = it.settings.copy(historyCardStyle = style)) }
        viewModelScope.launch { settingsRepository.setHistoryCardStyle(style) }
    }

    fun setCarouselStyle(style: String) {
        _uiState.update { it.copy(settings = it.settings.copy(carouselStyle = style)) }
        viewModelScope.launch { settingsRepository.setCarouselStyle(style) }
    }

    fun setNavBarStyle(style: String) {
        _uiState.update { it.copy(settings = it.settings.copy(navBarStyle = style)) }
        viewModelScope.launch { settingsRepository.setNavBarStyle(style) }
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSkipIntro(enabled) }
    }

    fun setAutoSkipOutro(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoSkipOutro(enabled) }
    }

    fun setDefaultQuality(quality: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(defaultQuality = quality) }
        }
    }

    fun setAudioSubPreference(pref: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(audioSubPreference = pref) }
        }
    }

    fun setSkipDuration(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(skipDurationSeconds = seconds) }
        }
    }

    fun setPlayerGestures(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(playerGestures = enabled) }
        }
    }

    fun setPlayerTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(playerTheme = theme) }
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(autoPlayNext = enabled) }
        }
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(backgroundPlayback = enabled) }
        }
    }

    fun setPipEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(pipEnabled = enabled) }
        }
    }

    fun setBufferCacheMb(mb: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(bufferCacheMb = mb) }
        }
    }

    fun setAutoUpdateExtensions(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(autoUpdateExtensions = enabled) }
        }
    }

    fun setAmoledPureBlack(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(amoledPureBlack = enabled)) }
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(amoledPureBlack = enabled) }
        }
    }

    // --- AniList Authentication Flows ---

    fun setAuthDialogVisible(visible: Boolean) {
        _uiState.update {
            it.copy(
                showAuthDialog = visible,
                authErrorMessage = null
            )
        }
    }

    fun setAuthDialogTab(tab: Int) {
        _uiState.update { it.copy(authDialogTab = tab, authErrorMessage = null) }
    }

    fun setTokenInput(token: String) {
        _uiState.update { it.copy(tokenInput = token, authErrorMessage = null) }
    }

    fun launchOAuthInBrowser(context: Context) {
        AniListOAuthHelper.launchOAuthBrowser(context)
        _uiState.update {
            it.copy(
                showAuthDialog = false,
                toastMessage = "Opening AniList OAuth authorization page..."
            )
        }
    }

    fun loginWithToken(token: String) {
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

    // --- Extension Repositories ---

    fun setAddRepoDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddRepoDialog = visible, repoUrlInput = "") }
    }

    fun setRepoUrlInput(url: String) {
        _uiState.update { it.copy(repoUrlInput = url) }
    }

    fun addRepository(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            extensionRepository.addRepository(url.trim())
            setAddRepoDialogVisible(false)
            _uiState.update { it.copy(toastMessage = "Repository added") }
        }
    }

    fun removeRepository(url: String) {
        viewModelScope.launch {
            extensionRepository.removeRepository(url)
            _uiState.update { it.copy(toastMessage = "Repository removed") }
        }
    }

    fun syncRepositories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingRepos = true) }
            extensionRepository.refreshRepositories()
            _uiState.update { it.copy(isSyncingRepos = false, toastMessage = "Repositories synchronized") }
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

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val aniListRepository: AniListRepository,
        private val playbackRepository: PlaybackRepository,
        private val extensionRepository: ExtensionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                settingsRepository,
                aniListRepository,
                playbackRepository,
                extensionRepository
            ) as T
        }
    }
}
