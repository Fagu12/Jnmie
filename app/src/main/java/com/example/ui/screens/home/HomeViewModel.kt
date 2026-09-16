package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AniListMediaListEntry
import com.example.domain.model.AniListUser
import com.example.domain.model.Anime
import com.example.domain.model.PlaybackProgress
import com.example.domain.model.SyncStatus
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.PlaybackRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val user: AniListUser? = null,
    val savedUsername: String? = null,
    val isAuthenticated: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val featuredAnime: List<Anime> = emptyList(),
    val continueWatching: List<PlaybackProgress> = emptyList(),
    val recentlyWatched: List<PlaybackProgress> = emptyList(),
    val aniListWatchingList: List<AniListMediaListEntry> = emptyList(),
    val trendingAnime: List<Anime> = emptyList(),
    val popularSeasonAnime: List<Anime> = emptyList(),
    val upcomingAnime: List<Anime> = emptyList(),
    val topRatedAnime: List<Anime> = emptyList(),
    val airingSchedule: List<Anime> = emptyList(),
    val favorites: List<Anime> = emptyList(),
    val categories: List<String> = listOf("All", "Trending", "This Season", "Upcoming", "Top Rated", "Airing Schedule", "Action", "Romance", "Fantasy", "Sci-Fi"),
    val selectedCategory: String = "All",
    val cardStyle: String = "Saikou",
    val historyCardStyle: String = "Frosted Glass",
    val error: String? = null
) {
    val displayUsername: String?
        get() = user?.name ?: savedUsername
}

class HomeViewModel(
    private val animeRepository: AnimeRepository,
    private val playbackRepository: PlaybackRepository,
    private val aniListRepository: AniListRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            playbackRepository.getContinueWatching().collect { list ->
                _uiState.update { it.copy(continueWatching = list) }
            }
        }
        viewModelScope.launch {
            playbackRepository.getWatchHistory().collect { list ->
                _uiState.update { it.copy(recentlyWatched = list) }
            }
        }
        viewModelScope.launch {
            animeRepository.getFavoriteAnimeList().collect { list ->
                _uiState.update { it.copy(favorites = list) }
            }
        }
        viewModelScope.launch {
            aniListRepository.isAuthenticated.collect { authed ->
                _uiState.update { it.copy(isAuthenticated = authed) }
                if (authed) {
                    loadUserAniListData()
                }
            }
        }
        viewModelScope.launch {
            aniListRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            aniListRepository.savedUsername.collect { savedName ->
                _uiState.update { it.copy(savedUsername = savedName) }
            }
        }
        viewModelScope.launch {
            aniListRepository.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        viewModelScope.launch {
            aniListRepository.cachedUserList.collect { entries ->
                _uiState.update { current ->
                    current.copy(
                        aniListWatchingList = entries.filter { it.status.equals("CURRENT", ignoreCase = true) }
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        cardStyle = settings.cardStyle,
                        historyCardStyle = settings.historyCardStyle
                    )
                }
            }
        }
    }

    fun loadData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, error = null) }

            val trendingRes = animeRepository.getTrendingAnime(page = 1)
            val popularRes = animeRepository.getPopularThisSeason(page = 1)
            val upcomingRes = animeRepository.getUpcomingAnime(page = 1)
            val topRatedRes = animeRepository.getTopRated(page = 1)
            val airingRes = animeRepository.getAiringSchedule(page = 1)

            val trendingList = trendingRes.getOrDefault(emptyList())
            val popularList = popularRes.getOrDefault(emptyList())
            val upcomingList = upcomingRes.getOrDefault(emptyList())
            val topRatedList = topRatedRes.getOrDefault(emptyList())
            val airingList = airingRes.getOrDefault(emptyList())

            val featuredList = if (trendingList.isNotEmpty()) trendingList.take(6) else popularList.take(6)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    featuredAnime = featuredList,
                    trendingAnime = trendingList,
                    popularSeasonAnime = popularList,
                    upcomingAnime = upcomingList,
                    topRatedAnime = topRatedList,
                    airingSchedule = airingList
                )
            }
        }
    }

    private fun loadUserAniListData() {
        viewModelScope.launch {
            val userListRes = aniListRepository.getUserAnimeList(status = "CURRENT")
            val watching = userListRes.getOrDefault(emptyList())
            if (watching.isNotEmpty()) {
                _uiState.update { it.copy(aniListWatchingList = watching) }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun logout() {
        viewModelScope.launch {
            aniListRepository.logout()
            _uiState.update { it.copy(aniListWatchingList = emptyList(), user = null, savedUsername = null) }
        }
    }

    fun syncAniList() {
        viewModelScope.launch {
            aniListRepository.syncUserLists()
            loadUserAniListData()
        }
    }

    fun loginWithToken(token: String) {
        viewModelScope.launch {
            aniListRepository.authenticateWithToken(token)
        }
    }

    class Factory(
        private val animeRepository: AnimeRepository,
        private val playbackRepository: PlaybackRepository,
        private val aniListRepository: AniListRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                animeRepository,
                playbackRepository,
                aniListRepository,
                settingsRepository
            ) as T
        }
    }
}
