package com.example.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Anime
import com.example.domain.repository.SearchRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedGenre: String = "All",
    val selectedFormat: String = "All",
    val isListView: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val results: List<Anime> = emptyList(),
    val popularTags: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val genres: List<String> = listOf(
        "All", "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Sci-Fi", "Supernatural", "Romance", "Mystery", "Sports"
    ),
    val formats: List<String> = listOf("All", "TV", "MOVIE", "OVA"),
    val cardStyle: String = "Saikou"
)

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialData()
        observeRecentSearches()
        observeSettings()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val tags = searchRepository.getPopularSearchTags()
            _uiState.update { it.copy(popularTags = tags) }
            performSearch(query = "", genre = "All", format = "All", saveHistory = false)
        }
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            searchRepository.getRecentSearches().collect { recent ->
                _uiState.update { it.copy(recentSearches = recent) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(cardStyle = settings.cardStyle) }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, errorMessage = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            performSearch(
                query = newQuery,
                genre = _uiState.value.selectedGenre,
                format = _uiState.value.selectedFormat,
                saveHistory = newQuery.isNotBlank()
            )
        }
    }

    fun onGenreSelect(genre: String) {
        val nextGenre = if (_uiState.value.selectedGenre == genre) "All" else genre
        _uiState.update { it.copy(selectedGenre = nextGenre, errorMessage = null) }
        performSearch(
            query = _uiState.value.query,
            genre = nextGenre,
            format = _uiState.value.selectedFormat,
            saveHistory = false
        )
    }

    fun onFormatSelect(format: String) {
        val nextFormat = if (_uiState.value.selectedFormat == format) "All" else format
        _uiState.update { it.copy(selectedFormat = nextFormat, errorMessage = null) }
        performSearch(
            query = _uiState.value.query,
            genre = _uiState.value.selectedGenre,
            format = nextFormat,
            saveHistory = false
        )
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isListView = !it.isListView) }
    }

    fun onTagClick(tag: String) {
        _uiState.update { it.copy(query = tag, errorMessage = null) }
        performSearch(
            query = tag,
            genre = _uiState.value.selectedGenre,
            format = _uiState.value.selectedFormat,
            saveHistory = true
        )
    }

    fun onRecentSearchClick(query: String) {
        _uiState.update { it.copy(query = query, errorMessage = null) }
        performSearch(
            query = query,
            genre = _uiState.value.selectedGenre,
            format = _uiState.value.selectedFormat,
            saveHistory = true
        )
    }

    fun onRemoveRecentSearch(query: String) {
        viewModelScope.launch {
            searchRepository.removeRecentSearch(query)
        }
    }

    fun onClearRecentSearches() {
        viewModelScope.launch {
            searchRepository.clearRecentSearches()
        }
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "", errorMessage = null) }
        performSearch(
            query = "",
            genre = _uiState.value.selectedGenre,
            format = _uiState.value.selectedFormat,
            saveHistory = false
        )
    }

    fun retry() {
        performSearch(
            query = _uiState.value.query,
            genre = _uiState.value.selectedGenre,
            format = _uiState.value.selectedFormat,
            saveHistory = _uiState.value.query.isNotBlank()
        )
    }

    private fun performSearch(
        query: String,
        genre: String?,
        format: String?,
        saveHistory: Boolean
    ) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (saveHistory && query.isNotBlank()) {
                searchRepository.saveRecentSearch(query)
            }

            val result = searchRepository.searchAnime(
                query = query,
                genre = if (genre == "All") null else genre,
                format = if (format == "All") null else format,
                page = 1
            )

            result.fold(
                onSuccess = { animeList ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            results = animeList,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            results = emptyList(),
                            errorMessage = error.localizedMessage ?: "Unable to complete search. Check your connection or try again."
                        )
                    }
                }
            )
        }
    }

    class Factory(
        private val searchRepository: SearchRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(searchRepository, settingsRepository) as T
        }
    }
}
