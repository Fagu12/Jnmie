package com.example.provider

import com.example.data.provider.MockTestSourceProvider
import com.example.data.repository.ProviderManagerImpl
import com.example.data.repository.SourceResolverImpl
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.SegmentType
import com.example.domain.provider.ProviderCapability
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit & pipeline tests for the built-in [SourceProvider] architecture:
 *
 * `searchAnime -> getAnimeDetails -> getEpisodes -> resolveStream -> VideoSource`
 */
class SourceProviderPipelineTest {

    private lateinit var mockProvider: MockTestSourceProvider
    private lateinit var providerManager: ProviderManagerImpl
    private lateinit var sourceResolver: SourceResolverImpl

    @Before
    fun setUp() {
        mockProvider = MockTestSourceProvider()
        providerManager = ProviderManagerImpl()
        sourceResolver = SourceResolverImpl(providerManager)
    }

    @Test
    fun mockTestSourceProvider_capabilitiesAndMetadataAreValid() {
        assertEquals("provider_mock_test", mockProvider.id)
        assertTrue(mockProvider.isEnabled)
        assertTrue(mockProvider.isAuthorized)
        assertTrue(mockProvider.hasCapability(ProviderCapability.STREAM_RESOLUTION))
        assertTrue(mockProvider.hasCapability(ProviderCapability.EPISODE_LIST))
        assertTrue(mockProvider.hasCapability(ProviderCapability.SEARCH))
        assertTrue(mockProvider.hasCapability(ProviderCapability.SUBTITLE_INJECTION))
        assertTrue(mockProvider.hasCapability(ProviderCapability.DUB_AUDIO))
    }

    @Test
    fun completePipeline_search_details_episodes_streamResolution_succeeds() = runBlocking {
        // 1. Search
        val searchRes = mockProvider.searchAnime("Big Buck Bunny")
        assertTrue("Search should succeed", searchRes.isSuccess)
        val animeList = searchRes.getOrThrow()
        assertFalse("Search should return results", animeList.isEmpty())
        val firstAnime = animeList.first()
        assertTrue(firstAnime.title.contains("Big Buck Bunny"))

        // 2. Anime Details
        val detailsRes = mockProvider.getAnimeDetails(firstAnime.id)
        assertTrue("Details should succeed", detailsRes.isSuccess)
        val animeDetails = detailsRes.getOrThrow()
        assertEquals(firstAnime.id, animeDetails.id)
        assertEquals(12, animeDetails.totalEpisodes)

        // 3. Episode Listings
        val episodesRes = mockProvider.getEpisodes(animeDetails.id)
        assertTrue("Episodes should succeed", episodesRes.isSuccess)
        val episodes = episodesRes.getOrThrow()
        assertEquals(12, episodes.size)

        val firstEp = episodes.first()
        assertEquals(1, firstEp.number)
        assertNotNull(firstEp.thumbnail)

        // 4. Stream Resolution
        val streamRes = mockProvider.resolveStream(firstEp.id)
        assertTrue("Stream resolution should succeed", streamRes.isSuccess)
        val sources = streamRes.getOrThrow()
        assertFalse("Sources list should not be empty", sources.isEmpty())

        // 5. VideoSource validation
        val hlsSource = sources.find { it.quality == "1080p" }
        assertNotNull("1080p HLS source should exist", hlsSource)
        assertTrue(hlsSource!!.streamUrl.endsWith(".m3u8"))
        assertFalse("Subtitles should be present", hlsSource.subtitles.isEmpty())
        assertFalse("Audio tracks should be present", hlsSource.audioTracks.isEmpty())
        assertFalse("Skip segments should be present", hlsSource.skipSegments.isEmpty())

        val introSkip = hlsSource.skipSegments.find { it.type == SegmentType.INTRO }
        assertNotNull("Intro skip should exist", introSkip)
        assertEquals(10L, introSkip!!.startSeconds)
        assertEquals(30L, introSkip.endSeconds)
    }

    @Test
    fun sourceResolver_orchestratesWithRegisteredProviders() = runBlocking {
        val testEpisode = AnimeEpisode(
            id = "mock_test_anime_1_ep_1",
            animeId = "mock_test_anime_1",
            number = 1,
            title = "Episode 1: The Test Journey",
            thumbnail = "https://example.com/thumb.jpg",
            durationSeconds = 600L
        )

        val sourcesRes = sourceResolver.resolveSourcesForEpisode(
            animeTitle = "Big Buck Bunny (Public Test Anime)",
            episode = testEpisode,
            preferredProviderId = "provider_mock_test"
        )
        assertTrue(sourcesRes.isSuccess)
        val sources = sourcesRes.getOrThrow()
        assertTrue(sources.isNotEmpty())
    }
}
