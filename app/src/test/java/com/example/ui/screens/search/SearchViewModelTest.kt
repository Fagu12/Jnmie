package com.example.ui.screens.search

import com.example.data.mock.MockAnimeData
import com.example.data.repository.SearchRepositoryImpl
import com.example.domain.model.Anime
import com.example.domain.model.AppSettings
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeAnimeRepository = object : AnimeRepository {
        override suspend fun getTrendingAnime(page: Int): Result<List<Anime>> =
            Result.success(MockAnimeData.trendingAnime)

        override suspend fun getPopularThisSeason(page: Int): Result<List<Anime>> =
            Result.success(MockAnimeData.popularThisSeason)

        override suspend fun getUpcomingAnime(page: Int): Result<List<Anime>> =
            Result.success(emptyList())

        override suspend fun getTopRated(page: Int): Result<List<Anime>> =
            Result.success(emptyList())

        override suspend fun getRecentlyUpdated(page: Int): Result<List<Anime>> =
            Result.success(MockAnimeData.recentlyUpdated)

        override suspend fun getAiringSchedule(page: Int): Result<List<Anime>> =
            Result.success(emptyList())

        override suspend fun searchAnime(
            query: String,
            genre: String?,
            tag: String?,
            season: String?,
            seasonYear: Int?,
            format: String?,
            status: String?,
            sort: String?,
            page: Int
        ): Result<List<Anime>> {
            val results = MockAnimeData.searchCatalog(query, genre)
            return Result.success(results)
        }

        override suspend fun getAnimeDetails(animeId: String): Result<Anime> =
            Result.success(MockAnimeData.featuredAnimeList.first())

        override suspend fun getEpisodes(animeId: String): Result<List<com.example.domain.model.Episode>> =
            Result.success(emptyList())

        override fun getFavoriteAnimeList(): Flow<List<Anime>> =
            flowOf(emptyList())

        override fun isFavoriteFlow(animeId: String): Flow<Boolean> =
            flowOf(false)

        override suspend fun isFavorite(animeId: String): Boolean = false

        override suspend fun getCachedAnime(animeId: String): Anime? = null

        override suspend fun toggleFavorite(anime: Anime): Boolean = true
    }

    private val fakeSettingsRepository = object : SettingsRepository {
        override val settingsFlow: Flow<AppSettings> = flowOf(AppSettings())
        override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {}
        override suspend fun setAutoSkipIntro(enabled: Boolean) {}
        override suspend fun setAutoSkipOutro(enabled: Boolean) {}
        override suspend fun setAutoSkipRecap(enabled: Boolean) {}
        override suspend fun setDefaultQuality(quality: String) {}
        override suspend fun setAudioSubPreference(pref: String) {}
        override suspend fun setSkipDurationSeconds(seconds: Int) {}
        override suspend fun setPlayerGestures(enabled: Boolean) {}
        override suspend fun setPlayerTheme(theme: String) {}
        override suspend fun setDefaultPlaybackSpeed(speed: Float) {}
        override suspend fun setAutoPlayNext(enabled: Boolean) {}
        override suspend fun setBackgroundPlayback(enabled: Boolean) {}
        override suspend fun setPipEnabled(enabled: Boolean) {}
        override suspend fun setBufferCacheMb(mb: Int) {}
        override suspend fun setHardwareAcceleration(enabled: Boolean) {}
        override suspend fun setHideAdultContent(hide: Boolean) {}
        override suspend fun setUnifiedLibrary(unified: Boolean) {}
        override suspend fun setCardStyle(style: String) {}
        override suspend fun setHistoryCardStyle(style: String) {}
        override suspend fun setCarouselStyle(style: String) {}
        override suspend fun setNavBarStyle(style: String) {}
        override suspend fun setNavBarMargin(margin: Int) {}
        override suspend fun setPreferredProvider(providerId: String) {}
        override suspend fun setSyncConflictStrategy(strategy: String) {}
        override suspend fun setAutoSyncAniList(enabled: Boolean) {}
        override suspend fun setSubEnabled(enabled: Boolean) {}
        override suspend fun setSubLanguage(language: String) {}
        override suspend fun setSubFontFamily(family: String) {}
        override suspend fun setSubFontSize(size: String) {}
        override suspend fun setSubFontWeight(weight: String) {}
        override suspend fun setSubTextColor(color: String) {}
        override suspend fun setSubBackgroundStyle(style: String) {}
        override suspend fun setSubBackgroundOpacity(opacity: Float) {}
        override suspend fun setSubOutlineStyle(style: String) {}
        override suspend fun setSubPosition(position: String) {}
        override suspend fun resetSettings() {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialSearchLoad() = runTest(testDispatcher) {
        val searchRepo = SearchRepositoryImpl(fakeAnimeRepository)
        val viewModel = SearchViewModel(searchRepo, fakeSettingsRepository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.results.isNotEmpty())
        assertTrue(state.popularTags.isNotEmpty())
        assertTrue(state.recentSearches.isNotEmpty())
    }

    @Test
    fun testQuerySearchExecution() = runTest(testDispatcher) {
        val searchRepo = SearchRepositoryImpl(fakeAnimeRepository)
        val viewModel = SearchViewModel(searchRepo, fakeSettingsRepository)

        advanceUntilIdle()

        viewModel.onQueryChange("Dandadan")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Dandadan", state.query)
        assertTrue(state.results.any { it.title.contains("DAN", ignoreCase = true) || it.romajiTitle?.contains("Dandadan", ignoreCase = true) == true })
    }

    @Test
    fun testGenreFilterToggle() = runTest(testDispatcher) {
        val searchRepo = SearchRepositoryImpl(fakeAnimeRepository)
        val viewModel = SearchViewModel(searchRepo, fakeSettingsRepository)

        advanceUntilIdle()

        viewModel.onGenreSelect("Action")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Action", state.selectedGenre)
        assertTrue(state.results.all { it.genres.any { g -> g.equals("Action", ignoreCase = true) } })
    }

    @Test
    fun testToggleViewMode() = runTest(testDispatcher) {
        val searchRepo = SearchRepositoryImpl(fakeAnimeRepository)
        val viewModel = SearchViewModel(searchRepo, fakeSettingsRepository)

        assertFalse(viewModel.uiState.value.isListView)
        viewModel.toggleViewMode()
        assertTrue(viewModel.uiState.value.isListView)
        viewModel.toggleViewMode()
        assertFalse(viewModel.uiState.value.isListView)
    }

    @Test
    fun testRecentSearchesManagement() = runTest(testDispatcher) {
        val searchRepo = SearchRepositoryImpl(fakeAnimeRepository)
        val viewModel = SearchViewModel(searchRepo, fakeSettingsRepository)

        advanceUntilIdle()

        viewModel.onRecentSearchClick("Bleach")
        advanceUntilIdle()

        assertEquals("Bleach", viewModel.uiState.value.query)

        viewModel.onRemoveRecentSearch("Bleach")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.recentSearches.contains("Bleach"))

        viewModel.onClearRecentSearches()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.recentSearches.isEmpty())
    }
}
