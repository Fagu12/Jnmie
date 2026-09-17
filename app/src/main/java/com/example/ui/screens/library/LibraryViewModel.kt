package com.example.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.interfaces.AniListRepository
import com.example.core.interfaces.AnimeRepository
import com.example.core.interfaces.PlaybackRepository
import com.example.core.interfaces.SettingsRepository
import com.example.core.model.AniListMediaListEntry
import com.example.core.model.AniListUser
import com.example.core.model.Anime
import com.example.core.model.ConflictStrategy
import com.example.core.model.PlaybackProgress
import com.example.domain.model.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryTab {
    ANILIST,
    ANIME,
    HISTORY,
    FAVORITES
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.ANILIST,
    val user: AniListUser? = null,
    val isAuthenticated: Boolean = false,
    val aniListEntries: List<AniListMediaListEntry> = emptyList(),
    val aniListFilterStatus: String? = null, // null for ALL, "CURRENT", "COMPLETED", "PLANNING"
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val libraryAnime: List<Anime> = emptyList(),
    val favorites: List<Anime> = emptyList(),
    val watchHistory: List<PlaybackProgress> = emptyList(),
    val cardStyle: String = "Saikou",
    val historyCardStyle: String = "Frosted Glass",
    val error: String? = null
)

class LibraryViewModel(
    private val animeRepository: AnimeRepository,
    private val playbackRepository: PlaybackRepository,
    private val aniListRepository: AniListRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            animeRepository.getFavoriteAnimeList().collect { favs ->
                _uiState.update { it.copy(favorites = favs) }
            }
        }
        viewModelScope.launch {
            playbackRepository.getWatchHistory().collect { history ->
                _uiState.update { it.copy(watchHistory = history) }
            }
        }
        viewModelScope.launch {
            aniListRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            aniListRepository.isAuthenticated.collect { auth ->
                _uiState.update { 
                    it.copy(
                        isAuthenticated = auth,
                        selectedTab = if (auth && it.selectedTab == LibraryTab.ANILIST) LibraryTab.ANILIST else it.selectedTab
                    ) 
                }
            }
        }
        viewModelScope.launch {
            aniListRepository.cachedUserList.collect { entries ->
                _uiState.update { it.copy(aniListEntries = entries) }
            }
        }
        viewModelScope.launch {
            aniListRepository.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
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

    private fun loadData() {
        viewModelScope.launch {
            val res = animeRepository.getTrendingAnime(1)
            val trending = res.getOrDefault(emptyList())
            _uiState.update { it.copy(libraryAnime = trending, error = res.exceptionOrNull()?.message) }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setAniListFilterStatus(status: String?) {
        _uiState.update { it.copy(aniListFilterStatus = status) }
    }

    fun syncAniList() {
        viewModelScope.launch {
            aniListRepository.syncUserLists(ConflictStrategy.HIGHEST_PROGRESS)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            playbackRepository.clearHistory()
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
            return LibraryViewModel(animeRepository, playbackRepository, aniListRepository, settingsRepository) as T
        }
    }
}
