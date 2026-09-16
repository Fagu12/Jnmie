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
import com.example.core.model.Anime
import com.example.core.model.AudioTrack
import com.example.core.model.Episode
import com.example.core.model.PlaybackProgress
import com.example.core.model.SubtitleTrack
import com.example.core.model.VideoSource
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
    val showAudioSheet: Boolean = false,
    val showSpeedSheet: Boolean = false,
    val showEpisodesSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val autoSkipIntro: Boolean = true,
    val autoSkipOutro: Boolean = true
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
        initializeSettingsAndData()
    }

    private fun initializeSettingsAndData() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.firstOrNull()
            val autoIntro = settings?.autoSkipIntro ?: true
            val autoOutro = settings?.autoSkipOutro ?: true

            _state.update {
                it.copy(
                    autoSkipIntro = autoIntro,
                    autoSkipOutro = autoOutro
                )
            }

            if (AppConfig.IS_STREAMING_ENABLED) {
                val manager = Media3PlayerManager(
                    context = context,
                    onProgressUpdate = { posMs, durMs ->
                        saveProgress(posMs, durMs)
                    },
                    isAutoSkipIntroEnabled = { _state.value.autoSkipIntro },
                    isAutoSkipOutroEnabled = { _state.value.autoSkipOutro },
                    onErrorCallback = { _, is403 ->
                        handlePlaybackError(is403)
                    }
                )
                playerController = manager

                viewModelScope.launch {
                    manager.uiState.collect { pState ->
                        _state.update { it.copy(playerUiState = pState) }
                    }
                }
            }

            loadAnimeAndEpisode(episodeNumber)
        }
    }

    fun getPlayerController(): PlayerController? = playerController

    fun loadAnimeAndEpisode(epNum: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isResolvingSource = true) }
            val anime = animeRepository.getAnimeDetails(animeId).getOrNull()
            val episodes = animeRepository.getEpisodes(animeId).getOrDefault(emptyList())
            val episode = episodes.find { it.number == epNum } ?: episodes.firstOrNull()

            _state.update {
                it.copy(
                    anime = anime,
                    episode = episode,
                    episodes = episodes,
                    isResolvingSource = false
                )
            }

            if (AppConfig.IS_STREAMING_ENABLED && anime != null && episode != null) {
                val sourcesRes = sourceResolver.resolveSources(anime.title, epNum)
                val sources = sourcesRes.getOrDefault(emptyList())

                val savedProgress = playbackRepository.getEpisodeProgress(animeId, epNum)
                val startPos = savedProgress?.currentPositionMs ?: 0L

                val chosenSource = sources.firstOrNull()
                if (chosenSource != null) {
                    playerController?.prepareSource(chosenSource, startPositionMs = startPos)
                }

                _state.update {
                    it.copy(
                        isResolvingSource = false,
                        playerUiState = it.playerUiState.copy(availableSources = sources)
                    )
                }
            }
        }
    }

    fun retryPlayback() {
        playerController?.retry()
    }

    fun tryNextServer() {
        val available = _state.value.playerUiState.availableSources
        val current = _state.value.playerUiState.selectedSource
        val nextSource = if (current != null) {
            val idx = available.indexOfFirst { it.id == current.id }
            available.getOrNull(idx + 1) ?: available.firstOrNull { it.id != current.id }
        } else {
            available.firstOrNull()
        }

        if (nextSource != null) {
            val currentPos = _state.value.playerUiState.currentPositionMs
            playerController?.prepareSource(nextSource, startPositionMs = currentPos, autoPlay = true)
        }
    }

    private fun handlePlaybackError(is403: Boolean) {
        if (!is403) return
        val available = _state.value.playerUiState.availableSources
        val current = _state.value.playerUiState.selectedSource ?: return
        val nextSource = available.firstOrNull { it.id != current.id }
        if (nextSource != null) {
            viewModelScope.launch {
                val currentPos = _state.value.playerUiState.currentPositionMs
                playerController?.prepareSource(nextSource, startPositionMs = currentPos, autoPlay = true)
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

    fun skipActiveSegment() {
        playerController?.skipActiveSegment()
    }

    fun setServerSheetVisible(visible: Boolean) {
        _state.update { it.copy(showServerSheet = visible) }
    }

    fun setQualitySheetVisible(visible: Boolean) {
        _state.update { it.copy(showQualitySheet = visible) }
    }

    fun setSubtitleSheetVisible(visible: Boolean) {
        _state.update { it.copy(showSubtitleSheet = visible) }
    }

    fun setAudioSheetVisible(visible: Boolean) {
        _state.update { it.copy(showAudioSheet = visible) }
    }

    fun setSpeedSheetVisible(visible: Boolean) {
        _state.update { it.copy(showSpeedSheet = visible) }
    }

    fun setEpisodesSheetVisible(visible: Boolean) {
        _state.update { it.copy(showEpisodesSheet = visible) }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _state.update { it.copy(showSettingsSheet = visible) }
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

    private fun saveProgress(posMs: Long, durMs: Long) {
        val anime = _state.value.anime ?: return
        val episode = _state.value.episode ?: return
        if (durMs <= 0) return

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
