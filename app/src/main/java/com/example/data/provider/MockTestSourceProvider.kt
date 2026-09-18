package com.example.data.provider

import com.example.domain.model.AnikotoServer
import com.example.domain.model.Anime
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AudioTrack
import com.example.domain.model.LanguagePreference
import com.example.domain.model.SegmentType
import com.example.domain.model.SkipSegment
import com.example.domain.model.StreamFormat
import com.example.domain.model.SubtitleTrack
import com.example.domain.model.VideoSource
import com.example.domain.provider.ProviderCapability
import com.example.domain.provider.SourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Built-in Mock / Test Source Provider.
 * Provides legitimate, publicly available open-license test media streams (HLS and MP4),
 * complete with multiple quality renditions, WebVTT subtitle tracks, audio tracks, and AniSkip-compatible
 * SkipSegment metadata (Intro 10s-30s, Outro 500s-530s).
 *
 * Fully compliant and decoupled from encrypted/reverse-engineered token algorithms.
 */
class MockTestSourceProvider : SourceProvider {
    override val id: String = "provider_mock_test"
    override val name: String = "Test Media (Verified Public Source)"
    override val baseUrl: String = "https://test-streams.mux.dev"
    override val priority: Int = 1
    override val isAuthorized: Boolean = true
    override val isEnabled: Boolean = true
    override val supportedServers: List<String> = listOf("Test HLS (Mux)", "Test MP4 (Google CDN)", "Open Source Stream")

    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.SEARCH,
        ProviderCapability.ANIME_DETAILS,
        ProviderCapability.TRENDING,
        ProviderCapability.LATEST_EPISODES,
        ProviderCapability.EPISODE_LIST,
        ProviderCapability.STREAM_RESOLUTION,
        ProviderCapability.MULTI_SERVER,
        ProviderCapability.SUBTITLE_INJECTION,
        ProviderCapability.DUB_AUDIO
    )

    override suspend fun searchAnime(query: String, page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        val sampleAnime = listOf(
            Anime(
                id = "mock_test_anime_1",
                title = "Big Buck Bunny (Public Test Anime)",
                romajiTitle = "Big Buck Bunny",
                englishTitle = "Big Buck Bunny Test",
                coverUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                bannerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                description = "Open source test media project for verifying HLS, MP4, subtitles, and player skipping controls.",
                score = 8.5,
                status = "FINISHED",
                totalEpisodes = 12,
                sourceProviderId = id
            ),
            Anime(
                id = "mock_test_anime_2",
                title = "Tears of Steel (Sci-Fi Test)",
                romajiTitle = "Tears of Steel",
                englishTitle = "Tears of Steel",
                coverUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                bannerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                description = "Open sci-fi VFX open source test media project.",
                score = 8.0,
                status = "FINISHED",
                totalEpisodes = 1,
                sourceProviderId = id
            )
        )
        val filtered = sampleAnime.filter { it.title.contains(query, ignoreCase = true) }
        Result.success(if (filtered.isNotEmpty()) filtered else sampleAnime)
    }

    override suspend fun getAnimeDetails(animeId: String): Result<Anime> = withContext(Dispatchers.IO) {
        Result.success(
            Anime(
                id = animeId,
                title = if (animeId.contains("2")) "Tears of Steel (Sci-Fi Test)" else "Big Buck Bunny (Public Test Anime)",
                coverUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                description = "Open source test media project for verifying HLS, MP4, subtitles, and player skipping controls.",
                score = 8.5,
                status = "FINISHED",
                totalEpisodes = 12,
                sourceProviderId = id
            )
        )
    }

    override suspend fun getEpisodes(animeId: String): Result<List<AnimeEpisode>> = withContext(Dispatchers.IO) {
        val count = if (animeId.contains("2")) 1 else 12
        val list = (1..count).map { num ->
            AnimeEpisode(
                id = "${animeId}_ep_$num",
                animeId = animeId,
                number = num,
                title = "Episode $num: The Test Journey",
                thumbnail = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                description = "Standard verified test media stream for episode $num.",
                durationSeconds = 600L,
                introStartSeconds = 10L,
                introEndSeconds = 30L,
                outroStartSeconds = 500L,
                outroEndSeconds = 530L
            )
        }
        Result.success(list)
    }

    override suspend fun getServerList(episodeId: String): Result<List<AnikotoServer>> = withContext(Dispatchers.IO) {
        Result.success(
            listOf(
                AnikotoServer(
                    id = "server_test_hls",
                    name = "Test HLS (Mux)",
                    type = "sub",
                    episodeId = episodeId,
                    embedUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                ),
                AnikotoServer(
                    id = "server_test_mp4",
                    name = "Test MP4 (Google CDN)",
                    type = "sub",
                    episodeId = episodeId,
                    embedUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                )
            )
        )
    }

    override suspend fun resolveStream(
        episodeId: String,
        server: String?,
        language: LanguagePreference
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        val isDub = language == LanguagePreference.DUB

        val testSubtitles = listOf(
            SubtitleTrack(
                id = "sub_en",
                label = "English (Full)",
                language = "en",
                url = "https://raw.githubusercontent.com/brenopolanski/html5-video-webvtt-example/master/subtitles/subtitles-en.vtt",
                format = "VTT",
                isDefault = true
            ),
            SubtitleTrack(
                id = "sub_ja",
                label = "Japanese (Original)",
                language = "ja",
                url = "https://raw.githubusercontent.com/brenopolanski/html5-video-webvtt-example/master/subtitles/subtitles-ja.vtt",
                format = "VTT",
                isDefault = false
            )
        )

        val testAudioTracks = listOf(
            AudioTrack(
                id = "audio_ja",
                label = "Japanese (Original Audio)",
                language = "ja",
                isDefault = !isDub
            ),
            AudioTrack(
                id = "audio_en",
                label = "English (Dub Audio)",
                language = "en",
                isDefault = isDub
            )
        )

        val sampleSkipSegments = listOf(
            SkipSegment(
                type = SegmentType.INTRO,
                startTimeMs = 10000L,
                endTimeMs = 30000L
            ),
            SkipSegment(
                type = SegmentType.OUTRO,
                startTimeMs = 500000L,
                endTimeMs = 530000L
            )
        )

        val sources = listOf(
            VideoSource(
                id = "src_test_hls_1080p",
                sourceName = name,
                serverName = "Test HLS (Mux Multi-Bitrate)",
                quality = "1080p",
                isDub = isDub,
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                headers = mapOf(
                    "User-Agent" to "JustAnime/2.0 (Linux; Android)"
                ),
                subtitles = testSubtitles,
                audioTracks = testAudioTracks,
                skipSegments = sampleSkipSegments,
                format = StreamFormat.HLS
            ),
            VideoSource(
                id = "src_test_mp4_720p",
                sourceName = name,
                serverName = "Test MP4 (Google CDN Direct)",
                quality = "720p",
                isDub = isDub,
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                headers = emptyMap(),
                subtitles = testSubtitles,
                audioTracks = testAudioTracks,
                skipSegments = sampleSkipSegments,
                format = StreamFormat.PROGRESSIVE_MP4
            )
        )

        Result.success(sources)
    }
}

/**
 * Typealias for exact naming requirement.
 */
typealias MockSourceProvider = MockTestSourceProvider
