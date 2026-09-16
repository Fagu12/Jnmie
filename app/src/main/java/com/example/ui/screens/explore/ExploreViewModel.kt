package com.example.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AniListUser
import com.example.domain.model.Anime
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val user: AniListUser? = null,
    val searchQuery: String = "",
    val selectedGenre: String = "All",
    val selectedSort: String = "TRENDING_DESC",
    val selectedFormat: String = "All",
    val selectedStatus: String = "All",
    val selectedSeason: String = "All",
    val selectedYear: Int? = null,
    val animeList: List<Anime> = emptyList(),
    val currentPage: Int = 1,
    val hasNextPage: Boolean = true,
    val cardStyle: String = "Saikou",
    val carouselStyle: String = "Classic",
    val error: String? = null
)

class ExploreViewModel(
    private val animeRepository: AnimeRepository,
    private val aniListRepository: AniListRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCatalog(page = 1, reset = true)
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            aniListRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        cardStyle = settings.cardStyle,
                        carouselStyle = settings.carouselStyle
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadCatalog(page = 1, reset = true)
        }
    }

    fun selectGenre(genre: String) {
        if (_uiState.value.selectedGenre == genre) return
        _uiState.update { it.copy(selectedGenre = genre) }
        loadCatalog(page = 1, reset = true)
    }

    fun selectSort(sort: String) {
        if (_uiState.value.selectedSort == sort) return
        _uiState.update { it.copy(selectedSort = sort) }
        loadCatalog(page = 1, reset = true)
    }

    fun selectFormat(format: String) {
        if (_uiState.value.selectedFormat == format) return
        _uiState.update { it.copy(selectedFormat = format) }
        loadCatalog(page = 1, reset = true)
    }

    fun selectStatus(status: String) {
        if (_uiState.value.selectedStatus == status) return
        _uiState.update { it.copy(selectedStatus = status) }
        loadCatalog(page = 1, reset = true)
    }

    fun selectSeason(season: String) {
        if (_uiState.value.selectedSeason == season) return
        _uiState.update { it.copy(selectedSeason = season) }
        loadCatalog(page = 1, reset = true)
    }

    fun selectYear(year: Int?) {
        if (_uiState.value.selectedYear == year) return
        _uiState.update { it.copy(selectedYear = year) }
        loadCatalog(page = 1, reset = true)
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedGenre = "All",
                selectedSort = "TRENDING_DESC",
                selectedFormat = "All",
                selectedStatus = "All",
                selectedSeason = "All",
                selectedYear = null
            )
        }
        loadCatalog(page = 1, reset = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasNextPage || state.isLoading) return
        loadCatalog(page = state.currentPage + 1, reset = false)
    }

    fun refresh() {
        loadCatalog(page = 1, reset = true)
    }

    private fun loadCatalog(page: Int, reset: Boolean) {
        viewModelScope.launch {
            if (reset) {
                _uiState.update { it.copy(isLoading = true, error = null, currentPage = 1) }
            } else {
                _uiState.update { it.copy(isLoadingMore = true, error = null) }
            }

            val state = _uiState.value
            val result = animeRepository.searchAnime(
                query = state.searchQuery,
                genre = state.selectedGenre.takeIf { it != "All" && it != "Trending" },
                tag = null,
                season = state.selectedSeason.takeIf { it != "All" },
                seasonYear = state.selectedYear,
                format = state.selectedFormat.takeIf { it != "All" },
                status = state.selectedStatus.takeIf { it != "All" },
                sort = state.selectedSort,
                page = page
            )

            val newItems = result.getOrDefault(emptyList())

            _uiState.update { current ->
                val combined = if (reset) newItems else current.animeList + newItems
                current.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    animeList = combined,
                    currentPage = page,
                    hasNextPage = newItems.isNotEmpty() && newItems.size >= 15
                )
            }
        }
    }

    class Factory(
        private val animeRepository: AnimeRepository,
        private val aniListRepository: AniListRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExploreViewModel(animeRepository, aniListRepository, settingsRepository) as T
        }
    }
}
