package com.example.ui.screens.details

import com.example.data.mock.MockAnimeData
import com.example.domain.model.Anime
import com.example.domain.model.AppSettings
import com.example.domain.model.Episode
import com.example.domain.model.ExtensionManifest
import com.example.domain.model.PlaybackProgress
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.ExtensionManager
import com.example.domain.repository.PlaybackRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class AnimeDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleAnime = MockAnimeData.featuredAnimeList.first()

    private val fakeEpisodes = (1..12).map { ep ->
        Episode(
            id = "ep_$ep",
            animeId = sampleAnime.id,
            number = ep,
            title = "Episode $ep",
            durationSeconds = 1440L
        )
    }

    private val favoriteListFlow = MutableStateFlow<List<Anime>>(emptyList())

    private val fakeAnimeRepository = object : AnimeRepository {
        override suspend fun getTrendingAnime(page: Int): Result<List<Anime>> = Result.success(emptyList())
        override suspend fun getPopularThisSeason(page: Int): Result<List<Anime>> = Result.success(emptyList())
        override suspend fun getUpcomingAnime(page: Int): Result<List<Anime>> = Result.success(emptyList())
        override suspend fun getTopRated(page: Int): Result<List<Anime>> = Result.success(emptyList())
        override suspend fun getRecentlyUpdated(page: Int): Result<List<Anime>> = Result.success(emptyList())
        override suspend fun getAiringSchedule(page: Int): Result<List<Anime>> = Result.success(emptyList())
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
        ): Result<List<Anime>> = Result.success(emptyList())
        override suspend fun getAnimeDetails(animeId: String): Result<Anime> = Result.success(sampleAnime)
        override suspend fun getEpisodes(animeId: String): Result<List<Episode>> = Result.success(fakeEpisodes)
        override fun getFavoriteAnimeList(): Flow<List<Anime>> = favoriteListFlow
        override fun isFavoriteFlow(animeId: String): Flow<Boolean> = flowOf(false)
        override suspend fun isFavorite(animeId: String): Boolean = false
        override suspend fun getCachedAnime(animeId: String): Anime? = sampleAnime
        override suspend fun toggleFavorite(anime: Anime): Boolean {
            val isFav = favoriteListFlow.value.any { it.id == anime.id }
            if (isFav) {
                favoriteListFlow.value = favoriteListFlow.value.filterNot { it.id == anime.id }
                return false
            } else {
                favoriteListFlow.value = favoriteListFlow.value + anime
                return true
            }
        }
    }

    private val fakePlaybackRepository = object : PlaybackRepository {
        override fun getContinueWatching(): Flow<List<PlaybackProgress>> = flowOf(emptyList())
        override fun getWatchHistory(): Flow<List<PlaybackProgress>> = flowOf(
            listOf(
                PlaybackProgress(
                    animeId = sampleAnime.id,
                    animeTitle = sampleAnime.title,
                    coverUrl = sampleAnime.coverUrl,
                    episodeNumber = 3,
                    episodeTitle = "Episode 3",
                    currentPositionMs = 500000L,
                    durationMs = 1400000L
                )
            )
        )
        override fun getWatchHistoryForAnime(animeId: String): Flow<List<PlaybackProgress>> = flowOf(emptyList())
        override fun getCompletedEpisodesFlow(animeId: String): Flow<List<Int>> = flowOf(emptyList())
        override suspend fun getEpisodeProgress(animeId: String, episodeNumber: Int): PlaybackProgress? = null
        override suspend fun getLatestProgressForAnime(animeId: String): PlaybackProgress? = null
        override suspend fun getCompletedEpisodeNumbers(animeId: String): List<Int> = emptyList()
        override suspend fun savePlaybackProgress(progress: PlaybackProgress) {}
        override suspend fun markEpisodeCompleted(animeId: String, episodeNumber: Int, completed: Boolean) {}
        override suspend fun deleteHistoryForAnime(animeId: String) {}
        override suspend fun deleteEpisodeHistory(animeId: String, episodeNumber: Int) {}
        override suspend fun clearHistory() {}
    }

    private val fakeExtensionManager = object : ExtensionManager {
        override val installedExtensions: Flow<List<com.example.domain.model.Extension>> = flowOf(emptyList())
        override suspend fun installExtension(extension: com.example.domain.model.Extension): Result<Unit> = Result.success(Unit)
        override suspend fun updateExtension(extensionId: String): Result<Unit> = Result.success(Unit)
        override suspend fun uninstallExtension(extensionId: String): Result<Unit> = Result.success(Unit)
        override suspend fun toggleExtension(extensionId: String, isEnabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun getEnabledExtensions(): List<com.example.domain.model.Extension> = emptyList()
        override suspend fun getExtension(extensionId: String): com.example.domain.model.Extension? = null
        override suspend fun getExtensionInstance(extensionId: String): com.example.core.extension.AnimeExtension? = null
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
    fun testLoadAnimeDetailsSuccess() = runTest(testDispatcher) {
        val viewModel = AnimeDetailsViewModel(
            animeId = sampleAnime.id,
            animeRepository = fakeAnimeRepository,
            playbackRepository = fakePlaybackRepository,
            extensionManager = fakeExtensionManager,
            settingsRepository = fakeSettingsRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.anime)
        assertEquals(sampleAnime.title, state.anime?.title)
        assertEquals(12, state.episodes.size)
        assertNotNull(state.continueWatching)
        assertEquals(3, state.continueWatching?.episodeNumber)
        assertEquals(3, state.nextEpisodeToWatch)
    }

    @Test
    fun testToggleWatchlist() = runTest(testDispatcher) {
        val viewModel = AnimeDetailsViewModel(
            animeId = sampleAnime.id,
            animeRepository = fakeAnimeRepository,
            playbackRepository = fakePlaybackRepository,
            extensionManager = fakeExtensionManager,
            settingsRepository = fakeSettingsRepository
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFavorite)
        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFavorite)

        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isFavorite)
    }

    @Test
    fun testEpisodeSortToggle() = runTest(testDispatcher) {
        val viewModel = AnimeDetailsViewModel(
            animeId = sampleAnime.id,
            animeRepository = fakeAnimeRepository,
            playbackRepository = fakePlaybackRepository,
            extensionManager = fakeExtensionManager,
            settingsRepository = fakeSettingsRepository
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.episodeSortAscending)
        assertEquals(1, viewModel.uiState.value.displayedEpisodes.first().number)

        viewModel.toggleEpisodeSort()
        assertFalse(viewModel.uiState.value.episodeSortAscending)
        assertEquals(12, viewModel.uiState.value.displayedEpisodes.first().number)
    }

    @Test
    fun testTabAndDescriptionToggles() = runTest(testDispatcher) {
        val viewModel = AnimeDetailsViewModel(
            animeId = sampleAnime.id,
            animeRepository = fakeAnimeRepository,
            playbackRepository = fakePlaybackRepository,
            extensionManager = fakeExtensionManager,
            settingsRepository = fakeSettingsRepository
        )

        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.selectedTab)
        viewModel.setTab(1)
        assertEquals(1, viewModel.uiState.value.selectedTab)

        assertFalse(viewModel.uiState.value.isDescriptionExpanded)
        viewModel.toggleDescriptionExpanded()
        assertTrue(viewModel.uiState.value.isDescriptionExpanded)
    }
}
