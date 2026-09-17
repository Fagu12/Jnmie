package com.example.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.PlaybackProgress
import com.example.domain.model.ProviderInfo
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.PlaybackRepository
import com.example.domain.repository.ProviderManager
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SourceResolver
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
    val userListStatus: String? = null,
    val userScore: Double? = null,
    val selectedTab: Int = 0,
    val isDescriptionExpanded: Boolean = false,
    val episodeSortAscending: Boolean = true,
    val availableProviders: List<ProviderInfo> = emptyList(),
    val selectedProvider: ProviderInfo? = null,
    val isResolvingProvider: Boolean = false,
    val providerResolverStatus: String? = null,
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
    private val providerManager: ProviderManager,
    private val settingsRepository: SettingsRepository,
    private val aniListRepository: AniListRepository? = null,
    private val sourceResolver: SourceResolver? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeDetailsUiState(animeId = animeId))
    val uiState: StateFlow<AnimeDetailsUiState> = _uiState.asStateFlow()

    init {
        loadAnime()
        loadProviders()
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
            if (animeRes.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = animeRes.exceptionOrNull()?.message ?: "Failed to load details") }
                return@launch
            }
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

            if (sourceResolver != null && anime != null) {
                val selected = _uiState.value.selectedProvider
                if (selected != null) {
                    resolveFromProvider(selected, anime)
                }
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

    private fun loadProviders() {
        viewModelScope.launch {
            val providers = providerManager.getAvailableProviders()
            val settings = settingsRepository.settingsFlow.firstOrNull()
            val preferredId = settings?.preferredProviderId
            val selected = providers.find { it.id == preferredId } ?: providers.firstOrNull()
            _uiState.update {
                it.copy(
                    availableProviders = providers,
                    selectedProvider = selected
                )
            }

            val currentAnime = _uiState.value.anime
            if (selected != null && currentAnime != null && sourceResolver != null) {
                resolveFromProvider(selected, currentAnime)
            }
        }
    }

    fun selectProvider(provider: ProviderInfo) {
        _uiState.update { it.copy(selectedProvider = provider) }
        viewModelScope.launch {
            settingsRepository.setPreferredProvider(provider.id)
            val currentAnime = _uiState.value.anime
            if (currentAnime != null && sourceResolver != null) {
                resolveFromProvider(provider, currentAnime)
            }
        }
    }

    fun refreshEpisodesFromProvider() {
        val selected = _uiState.value.selectedProvider ?: return
        val currentAnime = _uiState.value.anime ?: return
        if (sourceResolver != null) {
            resolveFromProvider(selected, currentAnime)
        }
    }

    private fun resolveFromProvider(provider: ProviderInfo, anime: Anime) {
        if (sourceResolver == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isResolvingProvider = true,
                    providerResolverStatus = "Connecting to ${provider.name}..."
                )
            }

            val epResult = sourceResolver.resolveEpisodes(
                animeTitle = anime.title,
                animeId = anime.id,
                preferredProviderId = provider.id
            )

            if (epResult.isSuccess && epResult.getOrNull()?.isNotEmpty() == true) {
                val extEpisodes = epResult.getOrThrow()
                _uiState.update {
                    it.copy(
                        episodes = extEpisodes,
                        isResolvingProvider = false,
                        providerResolverStatus = "Resolved ${extEpisodes.size} episodes from ${provider.name}"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isResolvingProvider = false,
                        providerResolverStatus = "Using metadata catalogue (${provider.name} ready for playback)"
                    )
                }
            }
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
        private val providerManager: ProviderManager,
        private val settingsRepository: SettingsRepository,
        private val aniListRepository: AniListRepository? = null,
        private val sourceResolver: SourceResolver? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnimeDetailsViewModel(
                animeId,
                animeRepository,
                playbackRepository,
                providerManager,
                settingsRepository,
                aniListRepository,
                sourceResolver
            ) as T
        }
    }
}
