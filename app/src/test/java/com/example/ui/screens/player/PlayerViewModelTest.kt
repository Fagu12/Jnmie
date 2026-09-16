package com.example.ui.screens.player

import com.example.core.interfaces.AnimeRepository
import com.example.core.interfaces.AppSettings
import com.example.core.interfaces.PlaybackRepository
import com.example.core.interfaces.SettingsRepository
import com.example.core.interfaces.SourceResolver
import com.example.core.model.Anime
import com.example.core.model.Episode
import com.example.core.model.PlaybackProgress
import com.example.core.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleAnime = Anime(
        id = "1",
        title = "Test Anime",
        coverUrl = "https://example.com/cover.jpg",
        description = "A test anime description"
    )

    private val sampleEpisodes = listOf(
        Episode(id = "ep1", number = 1, title = "Episode 1"),
        Episode(id = "ep2", number = 2, title = "Episode 2")
    )

    private val sampleSources = listOf(
        VideoSource(
            id = "src1",
            name = "Test Server 1",
            streamUrl = "https://example.com/stream1.m3u8",
            quality = "1080p"
        ),
        VideoSource(
            id = "src2",
            name = "Test Server 2",
            streamUrl = "https://example.com/stream2.m3u8",
            quality = "720p"
        )
    )

    private class FakeAnimeRepository(
        private val anime: Anime,
        private val episodes: List<Episode>
    ) : AnimeRepository {
        override suspend fun getTrendingAnime(): Result<List<Anime>> = Result.success(listOf(anime))
        override suspend fun getPopularAnime(): Result<List<Anime>> = Result.success(listOf(anime))
        override suspend fun searchAnime(query: String, filter: com.example.core.interfaces.AnimeFilter?): Result<List<Anime>> = Result.success(listOf(anime))
        override suspend fun getAnimeDetails(id: String): Result<Anime> = Result.success(anime)
        override suspend fun getEpisodes(animeId: String): Result<List<Episode>> = Result.success(episodes)
        override suspend fun getLibraryAnime(): Flow<List<Anime>> = flowOf(listOf(anime))
        override suspend fun toggleLibraryStatus(animeId: String): Boolean = true
    }

    private class FakeSourceResolver(
        private val sources: List<VideoSource>
    ) : SourceResolver {
        override suspend fun resolveSources(animeTitle: String, episodeNumber: Int): Result<List<VideoSource>> {
            return Result.success(sources)
        }
    }

    private class FakePlaybackRepository : PlaybackRepository {
        val savedProgress = mutableMapOf<String, PlaybackProgress>()

        override suspend fun getPlaybackHistory(): Flow<List<PlaybackProgress>> = flowOf(savedProgress.values.toList())
        override suspend fun getEpisodeProgress(animeId: String, episodeNumber: Int): PlaybackProgress? {
            return savedProgress["${animeId}_$episodeNumber"]
        }
        override suspend fun savePlaybackProgress(progress: PlaybackProgress) {
            savedProgress["${progress.animeId}_${progress.episodeNumber}"] = progress
        }
        override suspend fun clearHistory() {
            savedProgress.clear()
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val settings = MutableStateFlow(AppSettings(autoSkipIntro = true, autoSkipOutro = true))
        override val settingsFlow: Flow<AppSettings> = settings
        override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
        override suspend fun resetToDefaults() {
            settings.value = AppSettings()
        }
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
    fun playerViewModel_loadsAnimeAndEpisode_correctly() = runTest {
        val fakeAnimeRepo = FakeAnimeRepository(sampleAnime, sampleEpisodes)
        val fakeSourceResolver = FakeSourceResolver(sampleSources)
        val fakePlaybackRepo = FakePlaybackRepository()
        val fakeSettingsRepo = FakeSettingsRepository()

        // Pass a mock/null context since test env does not support Android context creation
        // We test logic on PlayerScreenState & state flows
        val viewModel = PlayerViewModel(
            context = org.mockito.Mockito.mock(android.content.Context::class.java),
            animeId = "1",
            episodeNumber = 1,
            animeRepository = fakeAnimeRepo,
            sourceResolver = fakeSourceResolver,
            playbackRepository = fakePlaybackRepo,
            settingsRepository = fakeSettingsRepo
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Test Anime", state.anime?.title)
        assertEquals(1, state.episode?.number)
        assertEquals(2, state.episodes.size)
    }
}
