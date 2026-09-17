package com.example.ui.screens.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.AppConfig
import com.example.core.interfaces.AnimeRepository
import com.example.core.interfaces.PlaybackRepository
import com.example.core.interfaces.SettingsRepository
import com.example.core.interfaces.SourceResolver
import com.example.domain.model.AppSettings
import com.example.domain.model.Anime
import com.example.domain.model.AudioTrack
import com.example.domain.model.Episode
import com.example.domain.model.PlaybackProgress
import com.example.domain.model.SubtitleTrack
import com.example.domain.model.VideoSource
import com.example.domain.model.LanguagePreference
import com.example.player.Media3PlayerManager
import com.example.player.PlayerController
import com.example.player.PlayerUiState
import com.example.player.ResizeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerScreenState(
    val anime: Anime? = null,
    val episode: Episode? = null,
    val episodes: List<Episode> = emptyList(),
    val isResolvingSource: Boolean = true,
    val playerUiState: PlayerUiState = PlayerUiState(),
    val showServerSheet: Boolean = false,
    val showQualitySheet: Boolean = false,
    val showSubtitleSheet: Boolean = false,
    val showSyncSubtitlesSheet: Boolean = false,
    val showAudioSheet: Boolean = false,
    val showSpeedSheet: Boolean = false,
    val showEpisodesSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showShadersSheet: Boolean = false,
    val showDiagnosticsSheet: Boolean = false,
    val autoSkipIntro: Boolean = true,
    val autoSkipOutro: Boolean = true,
    val autoSkipRecap: Boolean = true,
    val backgroundPlayback: Boolean = false,
    val settings: AppSettings? = null,
    val playerController: PlayerController? = null
)

class PlayerViewModel(
    private val context: Context,
    private val animeId: String,
    private val episodeNumber: Int,
    private val animeRepository: AnimeRepository,
    private val sourceResolver: SourceResolver,
    private val playbackRepository: PlaybackRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerScreenState())
    val state: StateFlow<PlayerScreenState> = _state.asStateFlow()

    private var playerController: PlayerController? = null

    init {
        if (AppConfig.IS_STREAMING_ENABLED) {
            val manager = Media3PlayerManager(
                context = context,
                onProgressUpdate = { posMs, durMs ->
                    saveProgress(posMs, durMs)
                },
                isAutoSkipIntroEnabled = { _state.value.autoSkipIntro },
                isAutoSkipOutroEnabled = { _state.value.autoSkipOutro },
                isAutoSkipRecapEnabled = { _state.value.autoSkipRecap },
                onErrorCallback = { _, is403, is410 ->
                    handlePlaybackError(is403, is410)
                }
            )
            playerController = manager
            _state.update { it.copy(playerController = manager) }

            viewModelScope.launch {
                manager.uiState.collect { pState ->
                    _state.update { it.copy(playerUiState = pState) }
                }
            }
        }
        initializeSettingsAndData()
    }

    private fun initializeSettingsAndData() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.firstOrNull()
            val autoIntro = settings?.autoSkipIntro ?: true
            val autoOutro = settings?.autoSkipOutro ?: true
            val autoRecap = settings?.autoSkipRecap ?: true
            val bgPlay = settings?.backgroundPlayback ?: false

            _state.update {
                it.copy(
                    autoSkipIntro = autoIntro,
                    autoSkipOutro = autoOutro,
                    autoSkipRecap = autoRecap,
                    backgroundPlayback = bgPlay,
                    settings = settings
                )
            }

            // Continuously observe settings for subtitle styling updates
            launch {
                settingsRepository.settingsFlow.collect { updatedSettings ->
                    _state.update { it.copy(settings = updatedSettings) }
                }
            }

            loadAnimeAndEpisode(episodeNumber)
        }
    }

    fun getPlayerController(): PlayerController? = playerController

    fun loadAnimeAndEpisode(epNum: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isResolvingSource = true) }
            val animeRes = animeRepository.getAnimeDetails(animeId)
            val anime = animeRes.getOrNull()
            val episodesRes = animeRepository.getEpisodes(animeId)
            val episodes = episodesRes.getOrDefault(emptyList())
            val episode = episodes.find { it.number == epNum } ?: episodes.firstOrNull()

            _state.update {
                it.copy(
                    anime = anime,
                    episode = episode,
                    episodes = episodes,
                    isResolvingSource = false
                )
            }

            if (anime == null || episode == null) {
                _state.update { it.copy(playerUiState = it.playerUiState.copy(error = "Failed to load anime or episode details.")) }
                return@launch
            }

            if (AppConfig.IS_STREAMING_ENABLED) {
                val preferredProvider = _state.value.settings?.preferredProviderId
                val langPref = when (_state.value.settings?.audioSubPreference?.uppercase()) {
                    "DUB" -> LanguagePreference.DUB
                    "AUTO" -> LanguagePreference.AUTO
                    else -> LanguagePreference.SUB
                }
                val sourcesRes = sourceResolver.resolveSourcesForEpisode(
                    animeTitle = anime.title,
                    episode = episode,
                    preferredProviderId = preferredProvider,
                    languagePreference = langPref
                )
                val initialSources = sourcesRes.getOrDefault(emptyList())

                val savedProgress = playbackRepository.getEpisodeProgress(animeId, epNum)
                val startPos = if (savedProgress != null && !savedProgress.completed) {
                    savedProgress.currentPositionMs
                } else {
                    0L
                }

                val chosenSource = initialSources.firstOrNull()
                if (chosenSource != null) {
                    playerController?.prepareSource(chosenSource, startPositionMs = startPos)
                } else {
                    _state.update { it.copy(playerUiState = it.playerUiState.copy(error = sourcesRes.exceptionOrNull()?.message ?: "Searching authorized providers for streams...")) }
                }

                _state.update {
                    it.copy(
                        isResolvingSource = false,
                        playerUiState = it.playerUiState.copy(availableSources = initialSources)
                    )
                }
            }
        }
    }

    private var reResolutionAttempts = 0
    private val failedSourceIds = mutableSetOf<String>()

    fun retryPlayback() {
        failedSourceIds.clear()
        reResolutionAttempts = 0
        val anime = _state.value.anime
        val ep = _state.value.episode
        if (anime != null && ep != null && _state.value.playerUiState.error != null) {
            loadAnimeAndEpisode(ep.number)
        } else {
            playerController?.retry()
        }
    }

    fun tryNextServer() {
        val available = _state.value.playerUiState.availableSources.filter { it.id !in failedSourceIds }
        val current = _state.value.playerUiState.selectedSource
        val nextSource = if (current != null) {
            val idx = available.indexOfFirst { it.id == current.id }
            available.getOrNull(idx + 1) ?: available.firstOrNull { it.id != current.id }
        } else {
            available.firstOrNull()
        }

        if (nextSource != null) {
            val currentPos = _state.value.playerUiState.currentPositionMs
            _state.update { it.copy(playerUiState = it.playerUiState.copy(selectedSource = nextSource, error = null)) }
            playerController?.prepareSource(nextSource, startPositionMs = currentPos, autoPlay = true)
        }
    }

    private fun handlePlaybackError(is403: Boolean, is410: Boolean) {
        val current = _state.value.playerUiState.selectedSource
        if (current != null) {
            failedSourceIds.add(current.id)
        }

        if (is410) {
            viewModelScope.launch {
                com.example.core.diagnostics.PlaybackDiagnosticsManager.updatePlaybackState("HTTP 410 PlaybackSourceGone - Attempting server failover", isError = true)
                val anime = _state.value.anime
                val ep = _state.value.episode

                // 1. If we haven't re-resolved fresh for this episode yet, resolve the source again fresh from extension
                if (reResolutionAttempts < 1 && anime != null && ep != null) {
                    reResolutionAttempts++
                    val preferredProvider = _state.value.settings?.preferredProviderId
                    val freshRes = sourceResolver.resolveSourcesForEpisode(
                        animeTitle = anime.title,
                        episode = ep,
                        preferredProviderId = preferredProvider
                    )
                    val freshSources = freshRes.getOrNull()?.filter { it.id !in failedSourceIds }
                    if (!freshSources.isNullOrEmpty()) {
                        val firstSource = freshSources.first()
                        _state.update {
                            it.copy(
                                playerUiState = it.playerUiState.copy(
                                    availableSources = freshSources,
                                    selectedSource = firstSource,
                                    error = null
                                )
                            )
                        }
                        val currentPos = _state.value.playerUiState.currentPositionMs
                        playerController?.prepareSource(firstSource, startPositionMs = currentPos, autoPlay = true)
                        return@launch
                    }
                }

                // 2. Try the next available server returned by the provider
                val available = _state.value.playerUiState.availableSources.filter { it.id !in failedSourceIds }
                val nextSource = available.firstOrNull()
                if (nextSource != null) {
                    val currentPos = _state.value.playerUiState.currentPositionMs
                    _state.update {
                        it.copy(playerUiState = it.playerUiState.copy(selectedSource = nextSource, error = null))
                    }
                    playerController?.prepareSource(nextSource, startPositionMs = currentPos, autoPlay = true)
                } else {
                    // 3. If all servers fail, show "Playback source unavailable"
                    _state.update {
                        it.copy(
                            playerUiState = it.playerUiState.copy(
                                isBuffering = false,
                                error = "Playback source unavailable"
                            )
                        )
                    }
                }
            }
        } else if (is403) {
            if (current?.hlsProxyUrl != null && current.streamUrl != current.hlsProxyUrl) {
                val proxySource = current.copy(streamUrl = current.hlsProxyUrl)
                val currentPos = _state.value.playerUiState.currentPositionMs
                playerController?.prepareSource(proxySource, startPositionMs = currentPos, autoPlay = true)
            } else {
                tryNextServer()
            }
        }
    }

    fun selectSource(source: VideoSource) {
        val currentPos = _state.value.playerUiState.currentPositionMs
        playerController?.prepareSource(source, startPositionMs = currentPos)
    }

    fun selectQuality(quality: String) {
        playerController?.selectQuality(quality)
    }

    fun selectSubtitle(track: SubtitleTrack?) {
        playerController?.selectSubtitle(track)
    }

    fun selectAudioTrack(track: AudioTrack?) {
        playerController?.selectAudioTrack(track)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerController?.setPlaybackSpeed(speed)
    }

    fun setResizeMode(mode: ResizeMode) {
        playerController?.setResizeMode(mode)
    }

    fun cycleResizeMode() {
        playerController?.cycleResizeMode()
    }

    fun toggleLock() {
        playerController?.toggleLock()
    }

    fun toggleControls() {
        val current = _state.value.playerUiState.isControlsVisible
        playerController?.setControlsVisible(!current)
    }

    fun hideControls() {
        playerController?.setControlsVisible(false)
    }

    fun showControls() {
        playerController?.setControlsVisible(true)
    }

    fun skipActiveSegment() {
        playerController?.skipActiveSegment()
    }

    fun megaSkip() {
        playerController?.megaSkip()
    }

    fun cancelAutoSkip() {
        playerController?.cancelAutoSkip()
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        playerController?.adjustSubtitleDelay(deltaMs)
    }

    fun resetSubtitleDelay() {
        playerController?.resetSubtitleDelay()
    }

    fun setAudioChannels(channels: String) {
        playerController?.setAudioChannels(channels)
    }

    fun toggleHardwareDecoder() {
        playerController?.toggleHardwareDecoder()
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        playerController?.setDoubleTapSeekSeconds(seconds)
    }

    fun setMegaSkipDurationSeconds(seconds: Int) {
        playerController?.setMegaSkipDurationSeconds(seconds)
    }

    fun setShadersEnabled(enabled: Boolean) {
        playerController?.setShadersEnabled(enabled)
    }

    fun setShaderProfile(profile: String) {
        playerController?.setShaderProfile(profile)
    }

    fun setExperimentalSettings(
        enabled: Boolean? = null,
        frameInterpolation: Boolean? = null,
        pitchCorrection: Boolean? = null,
        cacheMinutes: Int? = null,
        demuxerBufferMb: Int? = null
    ) {
        playerController?.setExperimentalSettings(
            enabled,
            frameInterpolation,
            pitchCorrection,
            cacheMinutes,
            demuxerBufferMb
        )
    }

    // Sheet visibility toggles
    fun closeAllSheets() {
        _state.update {
            it.copy(
                showServerSheet = false,
                showQualitySheet = false,
                showSubtitleSheet = false,
                showSyncSubtitlesSheet = false,
                showAudioSheet = false,
                showSpeedSheet = false,
                showEpisodesSheet = false,
                showSettingsSheet = false,
                showShadersSheet = false,
                showDiagnosticsSheet = false
            )
        }
    }

    fun setServerSheetVisible(visible: Boolean) {
        closeAllSheets()
        _state.update { it.copy(showServerSheet = visible) }
    }

    fun setDiagnosticsSheetVisible(visible: Boolean) {
        val next = if (visible) true else false
        closeAllSheets()
        _state.update { it.copy(showDiagnosticsSheet = next) }
    }

    fun setQualitySheetVisible(visible: Boolean) {
        closeAllSheets()
        _state.update { it.copy(showQualitySheet = visible) }
    }

    fun setSubtitleSheetVisible(visible: Boolean) {
        closeAllSheets()
        _state.update { it.copy(showSubtitleSheet = visible) }
    }

    fun setSyncSubtitlesSheetVisible(visible: Boolean) {
        closeAllSheets()
        _state.update { it.copy(showSyncSubtitlesSheet = visible) }
    }

    fun setAudioSheetVisible(visible: Boolean) {
        closeAllSheets()
        _state.update { it.copy(showAudioSheet = visible) }
    }

    fun setSpeedSheetVisible(visible: Boolean) {
        closeAllSheets()
        _state.update { it.copy(showSpeedSheet = visible) }
    }

    fun setEpisodesSheetVisible(visible: Boolean) {
        closeAllSheets()
        _state.update { it.copy(showEpisodesSheet = visible) }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        closeAllSheets()
        _state.update { it.copy(showSettingsSheet = visible) }
    }

    fun setShadersSheetVisible(visible: Boolean) {
        closeAllSheets()
        _state.update { it.copy(showShadersSheet = visible) }
    }

    // Subtitle Appearance Customization (saved in SettingsRepository)
    fun updateSubtitleFontFamily(font: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(subFontFamily = font) }
        }
    }

    fun updateSubtitleFontSize(size: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(subFontSize = size) }
        }
    }

    fun updateSubtitleOutlineStyle(style: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(subOutlineStyle = style) }
        }
    }

    fun updateSubtitleTextColor(color: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(subTextColor = color) }
        }
    }

    fun updateSubtitleBackgroundStyle(style: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(subBackgroundStyle = style) }
        }
    }

    fun updateSubtitleBackgroundOpacity(opacity: Float) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(subBackgroundOpacity = opacity) }
        }
    }

    fun updateSubtitlePosition(pos: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(subPosition = pos) }
        }
    }

    fun resetSubtitleStyling() {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    subFontFamily = "Sans Serif",
                    subFontSize = "Normal",
                    subFontWeight = "Normal",
                    subTextColor = "White",
                    subOutlineStyle = "Shadow",
                    subBackgroundStyle = "Semi-transparent",
                    subBackgroundOpacity = 0.5f,
                    subPosition = "Bottom"
                )
            }
        }
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        _state.update { it.copy(autoSkipIntro = enabled) }
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(autoSkipIntro = enabled) }
        }
    }

    fun setAutoSkipOutro(enabled: Boolean) {
        _state.update { it.copy(autoSkipOutro = enabled) }
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(autoSkipOutro = enabled) }
        }
    }

    fun setAutoSkipRecap(enabled: Boolean) {
        _state.update { it.copy(autoSkipRecap = enabled) }
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(autoSkipRecap = enabled) }
        }
    }

    private var lastSavedPosMs: Long = 0L
    private var lastSaveTimeMs: Long = 0L

    private fun saveProgress(posMs: Long, durMs: Long, force: Boolean = false) {
        val anime = _state.value.anime ?: return
        val episode = _state.value.episode ?: return
        if (durMs <= 0) return

        val now = System.currentTimeMillis()
        val posDiff = kotlin.math.abs(posMs - lastSavedPosMs)
        val timeDiff = now - lastSaveTimeMs

        // Update if forced, or position changed by more than 5s, or 10s elapsed
        if (!force && posDiff < 5000L && timeDiff < 10000L) {
            return
        }

        lastSavedPosMs = posMs
        lastSaveTimeMs = now

        val completed = posMs >= (durMs * 0.90)
        val prog = PlaybackProgress(
            animeId = anime.id,
            animeTitle = anime.title,
            coverUrl = anime.coverUrl,
            episodeNumber = episode.number,
            episodeTitle = episode.title,
            currentPositionMs = posMs,
            durationMs = durMs,
            lastWatchedTimestamp = System.currentTimeMillis(),
            completed = completed
        )
        viewModelScope.launch {
            playbackRepository.savePlaybackProgress(prog)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Save final progress when leaving
        _state.value.playerUiState.let {
            saveProgress(it.currentPositionMs, it.durationMs, force = true)
        }
        playerController?.release()
    }

    class Factory(
        private val context: Context,
        private val animeId: String,
        private val episodeNumber: Int,
        private val animeRepository: AnimeRepository,
        private val sourceResolver: SourceResolver,
        private val playbackRepository: PlaybackRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(
                context,
                animeId,
                episodeNumber,
                animeRepository,
                sourceResolver,
                playbackRepository,
                settingsRepository
            ) as T
        }
    }
}
