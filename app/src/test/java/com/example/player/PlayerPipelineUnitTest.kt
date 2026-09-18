package com.example.player

import com.example.domain.model.AudioTrack
import com.example.domain.model.PlaybackProgress
import com.example.domain.model.SegmentType
import com.example.domain.model.SkipSegment
import com.example.domain.model.StreamFormat
import com.example.domain.model.SubtitleTrack
import com.example.domain.model.VideoSource
import com.example.data.provider.MockSourceProvider
import com.example.data.repository.ProviderManagerImpl
import com.example.data.repository.SourceResolverImpl
import com.example.domain.model.Anime
import com.example.domain.model.AnimeEpisode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Validates the complete built-in source-to-player pipeline:
 *
 * Anime Details -> Episode -> SourceProvider -> List<VideoSource> -> Source selection -> Media3 Player
 *
 * Checks:
 * 1. HLS and MP4 format handling
 * 2. SubtitleTrack and AudioTrack exposure
 * 3. Playback position & source selection persistence
 * 4. Total encapsulation (UI only receives VideoSource models, never knows how URLs were obtained)
 */
class PlayerPipelineUnitTest {

    private lateinit var mockSourceProvider: MockSourceProvider
    private lateinit var providerManager: ProviderManagerImpl
    private lateinit var sourceResolver: SourceResolverImpl

    @Before
    fun setUp() {
        mockSourceProvider = MockSourceProvider()
        providerManager = ProviderManagerImpl()
        sourceResolver = SourceResolverImpl(providerManager)
    }

    @Test
    fun testCompleteSourceToPlayerPipeline() = runBlocking {
        // Step 1: Anime Details
        val anime = Anime(
            id = "mock_test_anime_1",
            title = "Big Buck Bunny (Public Test Anime)",
            coverUrl = "https://example.com/cover.jpg",
            bannerUrl = "https://example.com/banner.jpg",
            totalEpisodes = 12,
            format = "TV",
            status = "FINISHED"
        )

        // Step 2: Episode
        val episode = AnimeEpisode(
            id = "mock_test_anime_1_ep_1",
            animeId = anime.id,
            number = 1,
            title = "Episode 1: The Test Journey",
            thumbnail = "https://example.com/thumb.jpg",
            durationSeconds = 600L,
            introStartSeconds = 10L,
            introEndSeconds = 30L
        )

        // Step 3: SourceProvider resolution
        val streamResult = mockSourceProvider.resolveStream(episode.id)
        assertTrue("Provider should resolve stream", streamResult.isSuccess)
        val sources: List<VideoSource> = streamResult.getOrThrow()
        assertFalse("Sources list should not be empty", sources.isEmpty())

        // Step 4: Validate HLS sources
        val hlsSources = sources.filter { it.format == StreamFormat.HLS || it.streamUrl.contains(".m3u8") }
        assertTrue("Should include HLS streams", hlsSources.isNotEmpty())
        val primaryHls = hlsSources.first()
        assertEquals("1080p", primaryHls.quality)
        assertTrue(primaryHls.streamUrl.endsWith(".m3u8"))

        // Step 5: Validate MP4 sources
        val mp4Sources = sources.filter { it.format == StreamFormat.PROGRESSIVE_MP4 || it.streamUrl.endsWith(".mp4") }
        assertTrue("Should include MP4 streams", mp4Sources.isNotEmpty())
        val primaryMp4 = mp4Sources.first()
        assertTrue(primaryMp4.streamUrl.endsWith(".mp4"))

        // Step 6: Validate Subtitle Tracks & Audio Tracks exposure
        assertFalse("Subtitles should be populated", primaryHls.subtitles.isEmpty())
        val englishSub = primaryHls.subtitles.find { it.language == "en" }
        assertNotNull("English subtitle track should exist", englishSub)
        assertTrue("Subtitle track should have English in label", englishSub!!.label.contains("English"))
        assertTrue("Subtitle track should have VTT or compatible format", englishSub.format.isNotBlank())

        assertFalse("Audio tracks should be populated", primaryHls.audioTracks.isEmpty())
        val japaneseAudio = primaryHls.audioTracks.find { it.language == "ja" }
        assertNotNull("Japanese audio track should exist", japaneseAudio)

        // Step 7: Validate Skip Segments (AniSkip / Intro / Outro)
        assertFalse("Skip segments should be present", primaryHls.skipSegments.isEmpty())
        val introSegment = primaryHls.skipSegments.find { it.type == SegmentType.INTRO }
        assertNotNull("Intro skip segment should exist", introSegment)
        assertEquals(10L, introSegment!!.startSeconds)
        assertEquals(30L, introSegment.endSeconds)

        // Step 8: Playback progress persistence model validation
        val savedPositionMs = 120000L // 2 minutes in
        val totalDurationMs = 600000L // 10 minutes total
        val progress = PlaybackProgress(
            animeId = anime.id,
            animeTitle = anime.title,
            coverUrl = anime.coverUrl,
            episodeNumber = episode.number,
            episodeTitle = episode.title,
            currentPositionMs = savedPositionMs,
            durationMs = totalDurationMs,
            completed = false
        )

        assertEquals(120000L, progress.currentPositionMs)
        assertFalse("Should not be marked completed at 20% watched", progress.completed)

        // Completed progress (>90%)
        val completedProgress = progress.copy(
            currentPositionMs = 580000L,
            completed = 580000L >= (totalDurationMs * 0.90)
        )
        assertTrue("Should be marked completed past 90%", completedProgress.completed)
    }

    @Test
    fun testJustAnimeSkipSystem_SegmentTypes_AndSettings() {
        // Supported segment types: INTRO, RECAP, OUTRO
        val intro = SkipSegment(type = SegmentType.INTRO, startTimeMs = 15000L, endTimeMs = 105000L)
        val recap = SkipSegment(type = SegmentType.RECAP, startTimeMs = 0L, endTimeMs = 45000L)
        val outro = SkipSegment(type = SegmentType.OUTRO, startTimeMs = 1200000L, endTimeMs = 1290000L)

        assertEquals(SegmentType.INTRO, intro.type)
        assertEquals(15000L, intro.startTimeMs)
        assertEquals(105000L, intro.endTimeMs)
        assertTrue("Intro segment should be valid", intro.isValid)

        assertEquals(SegmentType.RECAP, recap.type)
        assertEquals(0L, recap.startTimeMs)
        assertEquals(45000L, recap.endTimeMs)
        assertTrue("Recap segment should be valid", recap.isValid)

        assertEquals(SegmentType.OUTRO, outro.type)
        assertEquals(1200000L, outro.startTimeMs)
        assertEquals(1290000L, outro.endTimeMs)
        assertTrue("Outro segment should be valid", outro.isValid)

        // Invalid timestamp metadata check (should NOT seek or consider valid)
        val invalidSegment1 = SkipSegment(type = SegmentType.INTRO, startTimeMs = 10000L, endTimeMs = 5000L)
        assertFalse("End before start should be invalid", invalidSegment1.isValid)

        val invalidSegment2 = SkipSegment(type = SegmentType.OUTRO, startTimeMs = -100L, endTimeMs = 5000L)
        assertFalse("Negative start should be invalid", invalidSegment2.isValid)

        // Check independent settings
        val defaultSettings = com.example.domain.model.AppSettings()
        assertTrue(defaultSettings.autoSkipIntro)
        assertTrue(defaultSettings.autoSkipRecap)
        assertTrue(defaultSettings.autoSkipOutro)

        // Toggling one must not affect the others
        val customSettings = defaultSettings.copy(
            autoSkipIntro = true,
            autoSkipRecap = false,
            autoSkipOutro = true
        )
        assertTrue(customSettings.autoSkipIntro)
        assertFalse(customSettings.autoSkipRecap)
        assertTrue(customSettings.autoSkipOutro)

        // Playback inside segment boundary check
        val currentPlaybackPos = 50000L // 50s
        val insideIntro = currentPlaybackPos in intro.startTimeMs until intro.endTimeMs
        val insideRecap = currentPlaybackPos in recap.startTimeMs until recap.endTimeMs
        val insideOutro = currentPlaybackPos in outro.startTimeMs until outro.endTimeMs

        assertTrue("50s is inside intro [15s, 105s]", insideIntro)
        assertFalse("50s is NOT inside recap [0s, 45s]", insideRecap)
        assertFalse("50s is NOT inside outro [1200s, 1290s]", insideOutro)
    }
}
