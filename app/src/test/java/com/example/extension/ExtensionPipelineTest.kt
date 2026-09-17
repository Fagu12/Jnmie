package com.example.extension

import com.example.core.extension.AnimeExtension
import com.example.core.extension.MockAnimeExtension
import com.example.data.remote.extension.SafeExtensionEngine
import com.example.data.repository.SourceResolverImpl
import com.example.domain.model.Episode
import com.example.domain.model.Extension
import com.example.domain.model.ExtensionCapability
import com.example.domain.model.SegmentType
import com.example.domain.repository.ExtensionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end integration and unit tests for the Anime Extension Pipeline:
 *
 * `anime search -> anime details -> episodes -> source resolution -> VideoSource`
 *
 * Verifies that the mock extension implementation, ExtensionManager, and SourceResolver
 * operate deterministically in an isolated sandbox without external network calls.
 */
class ExtensionPipelineTest {

    private lateinit var mockExtension: MockAnimeExtension
    private lateinit var safeEngine: SafeExtensionEngine

    @Before
    fun setUp() {
        mockExtension = MockAnimeExtension()
        safeEngine = SafeExtensionEngine()
    }

    @Test
    fun mockExtension_manifestIsValid() {
        val manifest = mockExtension.manifest
        assertEquals(MockAnimeExtension.ID, manifest.id)
        assertEquals("Mock Stream Provider", manifest.name)
        assertTrue(manifest.isEnabled)
        assertTrue(manifest.isInstalled)
        assertTrue(manifest.capabilities.contains(ExtensionCapability.SEARCH))
        assertTrue(manifest.capabilities.contains(ExtensionCapability.DETAILS))
        assertTrue(manifest.capabilities.contains(ExtensionCapability.EPISODES))
        assertTrue(manifest.capabilities.contains(ExtensionCapability.STREAM_SOURCES))
        assertTrue(manifest.capabilities.contains(ExtensionCapability.AUTO_SKIP_SEGMENTS))
        assertTrue(manifest.capabilities.contains(ExtensionCapability.MULTI_AUDIO))
        assertTrue(manifest.capabilities.contains(ExtensionCapability.MULTI_SUBTITLES))
        assertTrue(manifest.supportedQualities.contains("1080p"))
    }

    @Test
    fun completePipeline_search_details_episodes_sourceResolution_succeeds() = runBlocking {
        // Step 1: Anime Search
        val searchResult = mockExtension.searchAnime("Frieren")
        assertTrue("Search should succeed", searchResult.isSuccess)
        val searchList = searchResult.getOrThrow()
        assertFalse("Search should find results", searchList.isEmpty())
        val foundAnime = searchList.first()
        assertTrue(foundAnime.title.contains("Frieren"))

        // Step 2: Anime Details
        val detailsResult = mockExtension.getAnimeDetails(foundAnime.id)
        assertTrue("Details should succeed", detailsResult.isSuccess)
        val detailedAnime = detailsResult.getOrThrow()
        assertEquals(foundAnime.id, detailedAnime.id)
        assertNotNull("Description should not be null", detailedAnime.description)
        assertTrue("Episodes count should be positive", (detailedAnime.totalEpisodes ?: 0) > 0)
        assertFalse("Genres should not be empty", detailedAnime.genres.isEmpty())

        // Step 3: Episode Listing
        val episodeListResult = mockExtension.getEpisodeList(detailedAnime.id)
        assertTrue("Episode list should succeed", episodeListResult.isSuccess)
        val episodes = episodeListResult.getOrThrow()
        assertEquals(28, episodes.size)

        val firstEpisode = episodes.first()
        assertEquals(1, firstEpisode.number)
        assertEquals("The Journey's End", firstEpisode.title)
        assertNotNull("Thumbnail should be present", firstEpisode.thumbnail)
        assertTrue("Duration should be positive", firstEpisode.durationSeconds > 0)

        // Step 4: Source Resolution
        val sourceResult = mockExtension.resolveVideoSources(
            animeTitle = detailedAnime.title,
            episodeNumber = firstEpisode.number,
            episodeId = firstEpisode.id
        )
        assertTrue("Source resolution should succeed", sourceResult.isSuccess)
        val sources = sourceResult.getOrThrow()
        assertFalse("Sources list should not be empty", sources.isEmpty())

        // Step 5: Verify VideoSource integrity
        val masterHls = sources.find { it.quality == "1080p" && !it.isDub }
        assertNotNull("1080p master source should exist", masterHls)
        assertTrue("Stream URL should be valid HLS", masterHls!!.streamUrl.endsWith(".m3u8"))
        assertNotNull("Stream headers should contain User-Agent", masterHls.headers["User-Agent"])

        // Subtitles Verification
        assertFalse("Subtitles should be populated", masterHls.subtitles.isEmpty())
        val englishSub = masterHls.subtitles.find { it.language == "en" }
        assertNotNull("English subtitles should exist", englishSub)
        assertTrue("Default subtitle should be true", englishSub!!.isDefault)

        // Audio Tracks Verification
        assertFalse("Audio tracks should be populated", masterHls.audioTracks.isEmpty())
        val japaneseAudio = masterHls.audioTracks.find { it.language == "ja" }
        assertNotNull("Japanese audio track should exist", japaneseAudio)

        // Skip Segments Verification
        assertFalse("Skip segments should be populated", masterHls.skipSegments.isEmpty())
        val introSkip = masterHls.skipSegments.find { it.type == SegmentType.INTRO }
        assertNotNull("Intro skip segment should exist", introSkip)
        assertEquals(85L, introSkip!!.startSeconds)
        assertEquals(175L, introSkip.endSeconds)

        val outroSkip = masterHls.skipSegments.find { it.type == SegmentType.OUTRO }
        assertNotNull("Outro skip segment should exist", outroSkip)
        assertEquals(1290L, outroSkip!!.startSeconds)
        assertEquals(1380L, outroSkip.endSeconds)
    }

    @Test
    fun sourceResolver_orchestratesWithMockExtension_andHandlesEpisodeDomain() = runBlocking {
        // Stub ExtensionManager for isolated testing
        val fakeManager = object : ExtensionManager {
            override val installedExtensions: Flow<List<Extension>> =
                flowOf(listOf(MockAnimeExtension.MANIFEST))

            override suspend fun installExtension(extension: Extension): Result<Unit> = Result.success(Unit)
            override suspend fun updateExtension(extensionId: String): Result<Unit> = Result.success(Unit)
            override suspend fun uninstallExtension(extensionId: String): Result<Unit> = Result.success(Unit)
            override suspend fun toggleExtension(extensionId: String, isEnabled: Boolean): Result<Unit> = Result.success(Unit)
            override suspend fun getEnabledExtensions(): List<Extension> = listOf(MockAnimeExtension.MANIFEST)
            override suspend fun getExtension(extensionId: String): Extension = MockAnimeExtension.MANIFEST
            override suspend fun getExtensionInstance(extensionId: String): AnimeExtension = mockExtension
        }

        val sourceResolver = SourceResolverImpl(fakeManager)

        // Test resolveSources
        val sourcesResult = sourceResolver.resolveSources(
            animeTitle = "Frieren: Beyond Journey's End",
            episodeNumber = 1,
            preferredProviderId = MockAnimeExtension.ID
        )
        assertTrue(sourcesResult.isSuccess)
        val sources = sourcesResult.getOrThrow()
        assertTrue(sources.isNotEmpty())
        assertEquals("1080p", sources.first().quality)

        // Test resolveSourcesForEpisode
        val testEpisode = Episode(
            id = "mock-frieren-ep-1",
            animeId = "mock-frieren",
            number = 1,
            title = "The Journey's End",
            thumbnail = "https://example.com/thumb.jpg",
            durationSeconds = 1440L
        )

        val epSourcesResult = sourceResolver.resolveSourcesForEpisode(
            animeTitle = "Frieren: Beyond Journey's End",
            episode = testEpisode,
            preferredProviderId = MockAnimeExtension.ID
        )
        assertTrue(epSourcesResult.isSuccess)
        val epSources = epSourcesResult.getOrThrow()
        assertTrue(epSources.isNotEmpty())

        // Test resolveAnimeDetails dynamically
        val detailsResult = sourceResolver.resolveAnimeDetails(
            animeTitle = "Frieren: Beyond Journey's End",
            preferredProviderId = MockAnimeExtension.ID
        )
        assertTrue(detailsResult.isSuccess)
        val details = detailsResult.getOrThrow()
        assertTrue(details.title.contains("Frieren"))

        // Test resolveEpisodes dynamically
        val episodesResult = sourceResolver.resolveEpisodes(
            animeTitle = "Frieren: Beyond Journey's End",
            preferredProviderId = MockAnimeExtension.ID
        )
        assertTrue(episodesResult.isSuccess)
        val episodes = episodesResult.getOrThrow()
        assertEquals(28, episodes.size)
    }

    @Test
    fun mockExtension_errorHandling_returnsFailureGracefully() = runBlocking {
        val searchErr = mockExtension.searchAnime("__FORCE_ERROR__")
        assertTrue("Simulated search error should return failure", searchErr.isFailure)

        val detailsErr = mockExtension.getAnimeDetails("non_existent_anime_id_12345")
        assertTrue("Non-existent anime details should return failure", detailsErr.isFailure)

        val sourceErr = mockExtension.resolveVideoSources("__FORCE_ERROR__", 1)
        assertTrue("Simulated source resolution error should return failure", sourceErr.isFailure)
    }
}
