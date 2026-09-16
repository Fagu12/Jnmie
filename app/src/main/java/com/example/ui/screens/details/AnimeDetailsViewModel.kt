package com.example.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.PlaybackProgress
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.ExtensionManager
import com.example.domain.repository.PlaybackRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnimeDetailsUiState(
    val animeId: String,
    val isLoading: Boolean = true,
    val anime: Anime? = null,
    val episodes: List<Episode> = emptyList(),
    val continueWatching: PlaybackProgress? = null,
    val isFavorite: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userListStatus: String? = null, // CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED
    val userScore: Double? = null,
    val selectedTab: Int = 0, // 0 = Overview, 1 = Episodes, 2 = Characters & Staff, 3 = Relations, 4 = Recommendations, 5 = Links
    val isDescriptionExpanded: Boolean = false,
    val episodeSortAscending: Boolean = true,
    val availableExtensions: List<ExtensionManifest> = emptyList(),
    val selectedExtension: ExtensionManifest? = null,
    val error: String? = null
) {
    val displayedEpisodes: List<Episode>
        get() = if (episodeSortAscending) episodes else episodes.reversed()

    val nextEpisodeToWatch: Int
        get() = continueWatching?.episodeNumber ?: 1
}

class AnimeDetailsViewModel(
    private val animeId: String,
    private val animeRepository: AnimeRepository,
    private val playbackRepository: PlaybackRepository,
    private val extensionManager: ExtensionManager,
    private val settingsRepository: SettingsRepository,
    private val aniListRepository: AniListRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeDetailsUiState(animeId = animeId))
    val uiState: StateFlow<AnimeDetailsUiState> = _uiState.asStateFlow()

    init {
        loadAnime()
        loadExtensions()
        observeAuth()
    }

    private fun observeAuth() {
        if (aniListRepository != null) {
            viewModelScope.launch {
                aniListRepository.isAuthenticated.collect { authed ->
                    _uiState.update { it.copy(isAuthenticated = authed) }
                }
            }
        }
    }

    fun loadAnime() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val animeRes = animeRepository.getAnimeDetails(animeId)
            val episodesRes = animeRepository.getEpisodes(animeId)
            val anime = animeRes.getOrNull()
            val episodes = episodesRes.getOrDefault(emptyList())

            val history = playbackRepository.getWatchHistory().firstOrNull()?.find { it.animeId == animeId }
            val isFav = animeRepository.getFavoriteAnimeList().firstOrNull()?.any { it.id == animeId } ?: anime?.isFavorite ?: false

            _uiState.update {
                it.copy(
                    isLoading = false,
                    anime = anime,
                    episodes = episodes,
                    continueWatching = history,
                    isFavorite = isFav,
                    userListStatus = anime?.userListStatus,
                    userScore = anime?.userScore
                )
            }
        }
    }

    fun setAniListStatus(status: String, score: Double? = null) {
        _uiState.update { it.copy(userListStatus = status, userScore = score ?: it.userScore) }
        viewModelScope.launch {
            aniListRepository?.setAnimeListStatus(animeId, status, score, progress = _uiState.value.continueWatching?.episodeNumber)
        }
    }

    fun setUserScore(score: Double) {
        _uiState.update { it.copy(userScore = score) }
        val currentStatus = _uiState.value.userListStatus ?: "CURRENT"
        viewModelScope.launch {
            aniListRepository?.setAnimeListStatus(animeId, currentStatus, score, progress = _uiState.value.continueWatching?.episodeNumber)
        }
    }

    private fun loadExtensions() {
        viewModelScope.launch {
            val enabled = extensionManager.getEnabledExtensions()
            val settings = settingsRepository.settingsFlow.firstOrNull()
            val preferredId = settings?.preferredProviderId
            val selected = enabled.find { it.id == preferredId } ?: enabled.firstOrNull()
            _uiState.update {
                it.copy(
                    availableExtensions = enabled,
                    selectedExtension = selected
                )
            }
        }
    }

    fun selectExtension(extension: ExtensionManifest) {
        _uiState.update { it.copy(selectedExtension = extension) }
        viewModelScope.launch {
            settingsRepository.setPreferredProvider(extension.id)
        }
    }

    fun toggleFavorite() {
        val currentAnime = _uiState.value.anime ?: return
        viewModelScope.launch {
            val newFavState = animeRepository.toggleFavorite(currentAnime)
            _uiState.update { it.copy(isFavorite = newFavState) }
            aniListRepository?.toggleFavorite(animeId)
        }
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleDescriptionExpanded() {
        _uiState.update { it.copy(isDescriptionExpanded = !it.isDescriptionExpanded) }
    }

    fun toggleEpisodeSort() {
        _uiState.update { it.copy(episodeSortAscending = !it.episodeSortAscending) }
    }

    class Factory(
        private val animeId: String,
        private val animeRepository: AnimeRepository,
        private val playbackRepository: PlaybackRepository,
        private val extensionManager: ExtensionManager,
        private val settingsRepository: SettingsRepository,
        private val aniListRepository: AniListRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnimeDetailsViewModel(
                animeId,
                animeRepository,
                playbackRepository,
                extensionManager,
                settingsRepository,
                aniListRepository
            ) as T
        }
    }
}
