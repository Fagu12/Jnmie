package com.example.core.extension

import com.example.domain.model.Anime
import com.example.domain.model.AudioTrack
import com.example.domain.model.Episode
import com.example.domain.model.Extension
import com.example.domain.model.ExtensionCapability
import com.example.domain.model.FuzzyDate
import com.example.domain.model.SegmentType
import com.example.domain.model.SkipSegment
import com.example.domain.model.SubtitleTrack
import com.example.domain.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Self-contained, robust Mock Extension implementation demonstrating the complete
 * extension pipeline:
 *
 * `anime search -> anime details -> episodes -> source resolution -> VideoSource`
 *
 * This implementation runs completely offline without network dependencies,
 * making it ideal for JVM tests, automated CI/CD pipelines, integration verification,
 * and emulator-free end-to-end testing.
 */
class MockAnimeExtension(
    override val manifest: Extension = MANIFEST
) : AnimeExtension {

    companion object {
        const val ID = "ext_mock_test_provider"
        const val NAME = "Mock Stream Provider"

        val MANIFEST = Extension(
            id = ID,
            name = NAME,
            version = "1.0.0",
            versionCode = 100,
            language = "ENGLISH",
            baseUrl = "https://mock.justanime.test",
            description = "Self-contained mock extension demonstrating full anime search, metadata, episodes, and stream source resolution pipeline without external network dependencies.",
            repoUrl = "https://raw.githubusercontent.com/justanime/anime-extensions/main/index.json",
            isEnabled = true,
            isInstalled = true,
            author = "JustAnime Architecture Team",
            capabilities = setOf(
                ExtensionCapability.SEARCH,
                ExtensionCapability.DETAILS,
                ExtensionCapability.EPISODES,
                ExtensionCapability.STREAM_SOURCES,
                ExtensionCapability.AUTO_SKIP_SEGMENTS,
                ExtensionCapability.MULTI_AUDIO,
                ExtensionCapability.MULTI_SUBTITLES
            ),
            supportedQualities = listOf("1080p", "720p", "480p", "Auto")
        )

        val MOCK_ANIME_CATALOG: List<Anime> = listOf(
            Anime(
                id = "mock-frieren",
                title = "Frieren: Beyond Journey's End",
                englishTitle = "Frieren: Beyond Journey's End",
                romajiTitle = "Sousou no Frieren",
                nativeTitle = "葬送のフリーレン",
                coverUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500",
                coverColor = "#7A9A95",
                bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200",
                description = "An elf mage reflection on life, memory, and mortal bonds after defeating the Demon King alongside her hero party.",
                format = "TV",
                status = "FINISHED",
                totalEpisodes = 28,
                episodeDuration = 24,
                season = "FALL",
                seasonYear = 2023,
                score = 9.3,
                popularity = 250000,
                genres = listOf("Adventure", "Drama", "Fantasy", "Slice of Life"),
                studios = listOf("Madhouse"),
                studio = "Madhouse",
                isFavorite = false,
                startDate = FuzzyDate(2023, 9, 29),
                endDate = FuzzyDate(2024, 3, 22)
            ),
            Anime(
                id = "mock-aot",
                title = "Attack on Titan: The Final Season",
                englishTitle = "Attack on Titan: The Final Season",
                romajiTitle = "Shingeki no Kyojin: The Final Season",
                nativeTitle = "進撃の巨人 The Final Season",
                coverUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500",
                coverColor = "#8A4335",
                bannerUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=1200",
                description = "Humanity's desperate fight for freedom reaches its climax across the sea on the shores of Marley.",
                format = "TV",
                status = "FINISHED",
                totalEpisodes = 28,
                episodeDuration = 24,
                season = "WINTER",
                seasonYear = 2021,
                score = 9.0,
                popularity = 380000,
                genres = listOf("Action", "Drama", "Fantasy", "Mystery"),
                studios = listOf("MAPPA"),
                studio = "MAPPA",
                isFavorite = false,
                startDate = FuzzyDate(2020, 12, 7),
                endDate = FuzzyDate(2023, 11, 4)
            ),
            Anime(
                id = "mock-demon-slayer",
                title = "Demon Slayer: Kimetsu no Yaiba Hashira Training Arc",
                englishTitle = "Demon Slayer: Kimetsu no Yaiba Hashira Training Arc",
                romajiTitle = "Kimetsu no Yaiba: Hashira Geiko-hen",
                nativeTitle = "鬼滅の刃 柱稽古編",
                coverUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500",
                coverColor = "#325C84",
                bannerUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=1200",
                description = "Tanjiro visits the Stone Hashira, Himejima, who intends to prepare him for the battles to come.",
                format = "TV",
                status = "FINISHED",
                totalEpisodes = 8,
                episodeDuration = 24,
                season = "SPRING",
                seasonYear = 2024,
                score = 8.6,
                popularity = 210000,
                genres = listOf("Action", "Fantasy", "Supernatural"),
                studios = listOf("ufotable"),
                studio = "ufotable",
                isFavorite = false,
                startDate = FuzzyDate(2024, 5, 12),
                endDate = FuzzyDate(2024, 6, 30)
            ),
            Anime(
                id = "mock-jjk",
                title = "Jujutsu Kaisen Season 2",
                englishTitle = "Jujutsu Kaisen Season 2",
                romajiTitle = "Jujutsu Kaisen 2nd Season",
                nativeTitle = "呪術廻戦 懐玉・玉折／渋谷事変",
                coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500",
                coverColor = "#203A63",
                bannerUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=1200",
                description = "The past of Satoru Gojo and Suguru Geto during their days as Jujutsu High students, leading into the Shibuya Incident.",
                format = "TV",
                status = "FINISHED",
                totalEpisodes = 23,
                episodeDuration = 24,
                season = "SUMMER",
                seasonYear = 2023,
                score = 8.9,
                popularity = 310000,
                genres = listOf("Action", "Fantasy", "Supernatural"),
                studios = listOf("MAPPA"),
                studio = "MAPPA",
                isFavorite = false,
                startDate = FuzzyDate(2023, 7, 6),
                endDate = FuzzyDate(2023, 12, 28)
            ),
            Anime(
                id = "mock-solo-leveling",
                title = "Solo Leveling",
                englishTitle = "Solo Leveling",
                romajiTitle = "Ore dake Level Up na Ken",
                nativeTitle = "俺だけレベルアップな件",
                coverUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=500",
                coverColor = "#2D3450",
                bannerUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200",
                description = "In a world where hunters must battle deadly monsters to protect the human race, Sung Jinwoo is the weakest of all hunters until he discovers a hidden double dungeon.",
                format = "TV",
                status = "FINISHED",
                totalEpisodes = 12,
                episodeDuration = 24,
                season = "WINTER",
                seasonYear = 2024,
                score = 8.4,
                popularity = 290000,
                genres = listOf("Action", "Adventure", "Fantasy"),
                studios = listOf("A-1 Pictures"),
                studio = "A-1 Pictures",
                isFavorite = false,
                startDate = FuzzyDate(2024, 1, 7),
                endDate = FuzzyDate(2024, 3, 31)
            )
        )
    }

    /**
     * Search anime catalog with keyword matching.
     * Supports simulation of failures via the query string `__FORCE_ERROR__`.
     */
    override suspend fun searchAnime(query: String, page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        if (query == "__FORCE_ERROR__") {
            return@withContext Result.failure(IllegalStateException("Simulated search error for testing pipeline"))
        }

        val trimmed = query.trim()
        val results = if (trimmed.isBlank()) {
            MOCK_ANIME_CATALOG
        } else {
            MOCK_ANIME_CATALOG.filter { anime ->
                anime.title.contains(trimmed, ignoreCase = true) ||
                        anime.englishTitle?.contains(trimmed, ignoreCase = true) == true ||
                        anime.romajiTitle?.contains(trimmed, ignoreCase = true) == true ||
                        anime.genres.any { it.contains(trimmed, ignoreCase = true) }
            }
        }

        val pageSize = 10
        val startIndex = (page - 1) * pageSize
        val pagedList = if (startIndex < results.size) {
            results.drop(startIndex).take(pageSize)
        } else {
            emptyList()
        }

        Result.success(pagedList)
    }

    /**
     * Fetch complete anime details by ID or title slug.
     */
    override suspend fun getAnimeDetails(animeIdOrUrl: String): Result<Anime> = withContext(Dispatchers.IO) {
        if (animeIdOrUrl == "__FORCE_ERROR__") {
            return@withContext Result.failure(IllegalStateException("Simulated details error for testing pipeline"))
        }

        val match = MOCK_ANIME_CATALOG.find {
            it.id.equals(animeIdOrUrl, ignoreCase = true) ||
                    it.title.contains(animeIdOrUrl, ignoreCase = true) ||
                    it.englishTitle?.contains(animeIdOrUrl, ignoreCase = true) == true ||
                    it.romajiTitle?.contains(animeIdOrUrl, ignoreCase = true) == true
        }

        if (match != null) {
            Result.success(match)
        } else {
            Result.failure(NoSuchElementException("Mock extension found no anime matching '$animeIdOrUrl'"))
        }
    }

    /**
     * Generate structured episode list for the given anime.
     */
    override suspend fun getEpisodeList(animeIdOrUrl: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        if (animeIdOrUrl == "__FORCE_ERROR__") {
            return@withContext Result.failure(IllegalStateException("Simulated episode listing error for testing pipeline"))
        }

        val anime = MOCK_ANIME_CATALOG.find {
            it.id.equals(animeIdOrUrl, ignoreCase = true) ||
                    it.title.contains(animeIdOrUrl, ignoreCase = true)
        } ?: MOCK_ANIME_CATALOG.first()

        val count = (anime.totalEpisodes ?: 12).coerceAtLeast(1)
        val episodes = (1..count).map { num ->
            Episode(
                id = "${anime.id}-ep-$num",
                animeId = anime.id,
                number = num,
                title = getMockEpisodeTitle(anime.id, num),
                thumbnail = anime.bannerUrl ?: anime.coverUrl,
                description = "Episode $num of ${anime.title} detailing key battles and revelations.",
                durationSeconds = 1440L,
                introStartSeconds = 85L,
                introEndSeconds = 175L,
                outroStartSeconds = 1290L,
                outroEndSeconds = 1380L,
                recapStartSeconds = 0L,
                recapEndSeconds = 45L
            )
        }

        Result.success(episodes)
    }

    /**
     * Resolve playable video streams with multiple qualities, dual-audio,
     * subtitles, and chapter skip segments.
     */
    override suspend fun resolveVideoSources(
        animeTitle: String,
        episodeNumber: Int,
        episodeId: String?
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        if (animeTitle == "__FORCE_ERROR__") {
            return@withContext Result.failure(IllegalStateException("Simulated source resolution error for testing pipeline"))
        }

        val idPrefix = episodeId ?: "mock_ep_${episodeNumber}"

        // Standard HTTP request headers
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 JustAnime/2.0",
            "Referer" to manifest.baseUrl,
            "Accept" to "*/*"
        )

        // Subtitles (English & French & Spanish)
        val subtitles = listOf(
            SubtitleTrack(
                id = "sub_en",
                label = "English [CC]",
                language = "en",
                url = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-HD.en.vtt",
                format = "VTT",
                isDefault = true
            ),
            SubtitleTrack(
                id = "sub_fr",
                label = "Français",
                language = "fr",
                url = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-HD.fr.vtt",
                format = "VTT",
                isDefault = false
            ),
            SubtitleTrack(
                id = "sub_es",
                label = "Español",
                language = "es",
                url = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-HD.en.vtt",
                format = "VTT",
                isDefault = false
            )
        )

        // Dual Audio Tracks (Japanese original + English Dub)
        val audioTracks = listOf(
            AudioTrack(
                id = "audio_ja",
                label = "Japanese (Original)",
                language = "ja",
                isDefault = true
            ),
            AudioTrack(
                id = "audio_en",
                label = "English (Dub)",
                language = "en",
                isDefault = false
            )
        )

        // Intro / Outro / Recap Skip markers
        val skipSegments = listOf(
            SkipSegment(
                type = SegmentType.RECAP,
                startSeconds = 0L,
                endSeconds = 45L
            ),
            SkipSegment(
                type = SegmentType.INTRO,
                startSeconds = 85L,
                endSeconds = 175L
            ),
            SkipSegment(
                type = SegmentType.OUTRO,
                startSeconds = 1290L,
                endSeconds = 1380L
            )
        )

        val sources = listOf(
            // Primary High-Definition HLS Master Stream (Auto / Multi-bitrate Adaptive)
            VideoSource(
                id = "${idPrefix}_1080p_hls",
                serverName = "Mock CDN Alpha (Fast HLS)",
                quality = "1080p",
                isDub = false,
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                headers = headers,
                subtitles = subtitles,
                audioTracks = audioTracks,
                skipSegments = skipSegments
            ),
            // Primary 720p HD MP4 Direct Stream
            VideoSource(
                id = "${idPrefix}_720p_mp4",
                serverName = "Mock CDN Alpha (Direct MP4)",
                quality = "720p",
                isDub = false,
                streamUrl = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-720p.mp4",
                headers = headers,
                subtitles = subtitles,
                audioTracks = audioTracks,
                skipSegments = skipSegments
            ),
            // Lightweight 480p SD Stream
            VideoSource(
                id = "${idPrefix}_480p_mp4",
                serverName = "Mock CDN Alpha (Data Saver)",
                quality = "480p",
                isDub = false,
                streamUrl = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-576p.mp4",
                headers = headers,
                subtitles = subtitles,
                audioTracks = audioTracks,
                skipSegments = skipSegments
            ),
            // English Dubbed Stream
            VideoSource(
                id = "${idPrefix}_1080p_dub",
                serverName = "Mock CDN Beta (Dubbed)",
                quality = "1080p",
                isDub = true,
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                headers = headers,
                subtitles = subtitles,
                audioTracks = listOf(audioTracks[1], audioTracks[0]),
                skipSegments = skipSegments
            ),
            // Backup High-Availability Mirror
            VideoSource(
                id = "${idPrefix}_backup_mirror",
                serverName = "Mock CDN Mirror (Unified)",
                quality = "1080p",
                isDub = false,
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                headers = headers,
                subtitles = subtitles,
                audioTracks = audioTracks,
                skipSegments = skipSegments
            )
        )

        Result.success(sources)
    }

    private fun getMockEpisodeTitle(animeId: String, episodeNumber: Int): String {
        return when (animeId) {
            "mock-frieren" -> when (episodeNumber) {
                1 -> "The Journey's End"
                2 -> "It Didn't Have to Be Magic..."
                3 -> "Killing Magic"
                4 -> "The Land Where Souls Rest"
                else -> "Episode $episodeNumber"
            }
            "mock-aot" -> when (episodeNumber) {
                1 -> "The Other Side of the Sea"
                2 -> "Midnight Train"
                3 -> "The Door of Hope"
                4 -> "From One Hand to Another"
                else -> "Episode $episodeNumber"
            }
            "mock-demon-slayer" -> when (episodeNumber) {
                1 -> "To Defeat Kibutsuji Muzan"
                2 -> "Water Hashira Giyu Tomioka's Pain"
                3 -> "Fully Recovered Tanjiro Joins the Training!"
                4 -> "To Bring a Smile to One's Face"
                else -> "Episode $episodeNumber"
            }
            else -> "Episode $episodeNumber"
        }
    }
}
