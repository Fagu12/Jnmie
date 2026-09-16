package com.example.domain.model

/**
 * Domain layer business models.
 * Completely decoupled from framework dependencies, serialization, or UI state.
 */

data class Anime(
    val id: String,
    val title: String,
    val nativeTitle: String? = null,
    val romajiTitle: String? = null,
    val englishTitle: String? = null,
    val synonyms: List<String> = emptyList(),
    val bannerUrl: String? = null,
    val coverUrl: String? = null,
    val coverColor: String? = null,
    val description: String? = null,
    val score: Double = 0.0,
    val meanScore: Double? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val status: String = "RELEASING", // RELEASING, FINISHED, NOT_YET_RELEASED, CANCELLED, HIATUS
    val format: String = "TV", // TV, TV_SHORT, MOVIE, SPECIAL, OVA, ONA, MUSIC
    val studio: String? = null,
    val studios: List<String> = emptyList(),
    val producers: List<String> = emptyList(),
    val source: String? = null, // ORIGINAL, MANGA, LIGHT_NOVEL, VISUAL_NOVEL, VIDEO_GAME, OTHER
    val countryOfOrigin: String? = null, // JP, KR, CN
    val season: String? = null, // WINTER, SPRING, SUMMER, FALL
    val seasonYear: Int? = null,
    val genres: List<String> = emptyList(),
    val tags: List<AnimeTag> = emptyList(),
    val totalEpisodes: Int? = null,
    val currentEpisodeCount: Int? = null,
    val episodeDuration: Int? = null, // in minutes
    val startDate: FuzzyDate? = null,
    val endDate: FuzzyDate? = null,
    val nextAiring: NextAiringEpisode? = null,
    val nextAiringEpisode: Int? = null,
    val nextAiringTime: Long? = null,
    val trailer: AnimeTrailer? = null,
    val relations: List<AnimeRelation> = emptyList(),
    val recommendations: List<AnimeRecommendation> = emptyList(),
    val characters: List<AnimeCharacter> = emptyList(),
    val staff: List<AnimeStaff> = emptyList(),
    val externalLinks: List<AnimeExternalLink> = emptyList(),
    val streamingLinks: List<AnimeExternalLink> = emptyList(),
    val isFavorite: Boolean = false,
    val userListStatus: String? = null, // CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED
    val userProgress: Int? = null,
    val userScore: Double? = null,
    val sourceProviderId: String? = null
)

data class AnimeTag(
    val id: Int,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val rank: Int? = null,
    val isMediaSpoiler: Boolean = false
)

data class FuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
) {
    fun formatted(): String? {
        if (year == null) return null
        val monthStr = when (month) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> null
        }
        return if (monthStr != null && day != null) {
            "$monthStr $day, $year"
        } else if (monthStr != null) {
            "$monthStr $year"
        } else {
            "$year"
        }
    }
}

data class AnimeTrailer(
    val id: String? = null,
    val site: String? = null, // youtube, dailymotion
    val thumbnail: String? = null
) {
    val watchUrl: String?
        get() = when (site?.lowercase()) {
            "youtube" -> id?.let { "https://www.youtube.com/watch?v=$it" }
            "dailymotion" -> id?.let { "https://www.dailymotion.com/video/$it" }
            else -> null
        }
}

data class AnimeRelation(
    val relationType: String, // SEQUEL, PREQUEL, SIDE_STORY, SPIN_OFF, ALTERNATIVE, PARENT, SUMMARY, OTHER
    val animeId: String,
    val title: String,
    val format: String? = null,
    val status: String? = null,
    val coverUrl: String? = null,
    val seasonYear: Int? = null,
    val totalEpisodes: Int? = null
)

data class AnimeRecommendation(
    val animeId: String,
    val title: String,
    val coverUrl: String? = null,
    val format: String? = null,
    val status: String? = null,
    val score: Double? = null,
    val rating: Int = 0
)

data class AnimeCharacter(
    val id: Int,
    val name: String,
    val nativeName: String? = null,
    val role: String = "MAIN", // MAIN, SUPPORTING, BACKGROUND
    val imageUrl: String? = null,
    val voiceActorName: String? = null,
    val voiceActorNativeName: String? = null,
    val voiceActorImageUrl: String? = null,
    val voiceActorLanguage: String? = null
)

data class AnimeStaff(
    val id: Int,
    val name: String,
    val nativeName: String? = null,
    val role: String,
    val imageUrl: String? = null
)

data class AnimeExternalLink(
    val id: Int,
    val url: String,
    val site: String,
    val type: String? = null, // STREAMING, SOCIAL, INFO
    val iconUrl: String? = null,
    val color: String? = null
)

data class NextAiringEpisode(
    val episode: Int,
    val airingAt: Long,
    val timeUntilAiring: Long = 0L
)

data class Episode(
    val id: String,
    val animeId: String,
    val number: Int,
    val title: String,
    val thumbnail: String? = null,
    val description: String? = null,
    val durationSeconds: Long = 1440L,
    val introStartSeconds: Long? = null,
    val introEndSeconds: Long? = null,
    val outroStartSeconds: Long? = null,
    val outroEndSeconds: Long? = null,
    val recapStartSeconds: Long? = null,
    val recapEndSeconds: Long? = null,
    val watchedProgressMs: Long = 0L,
    val isWatched: Boolean = false
)

data class VideoSource(
    val id: String,
    val serverName: String,
    val quality: String, // e.g. "1080p", "720p", "480p", "Auto"
    val isDub: Boolean = false,
    val streamUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val skipSegments: List<SkipSegment> = emptyList()
)

data class SubtitleTrack(
    val id: String,
    val label: String,
    val language: String,
    val url: String,
    val format: String = "VTT", // VTT, ASS, SRT
    val isDefault: Boolean = false
)

data class AudioTrack(
    val id: String,
    val label: String,
    val language: String,
    val isDefault: Boolean = false
)

enum class SegmentType {
    INTRO,
    OUTRO,
    RECAP
}

data class SkipSegment(
    val type: SegmentType,
    val startSeconds: Long,
    val endSeconds: Long
)

enum class ExtensionCapability(val displayName: String) {
    SEARCH("Anime Search"),
    DETAILS("Anime Details"),
    EPISODES("Episode Listings"),
    STREAM_SOURCES("Stream Sources"),
    AUTO_SKIP_SEGMENTS("Skip Markers"),
    MULTI_SUBTITLES("Multi-Subtitles"),
    MULTI_AUDIO("Dual Audio"),
    DIRECT_DOWNLOAD("Direct Download")
}

data class Extension(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int = 1,
    val language: String = "ENGLISH",
    val iconUrl: String? = null,
    val baseUrl: String,
    val description: String? = null,
    val isNsfw: Boolean = false,
    val repoUrl: String,
    val isEnabled: Boolean = true,
    val isInstalled: Boolean = true,
    val hasUpdate: Boolean = false,
    val latestVersion: String? = null,
    val author: String = "Community",
    val capabilities: Set<ExtensionCapability> = setOf(
        ExtensionCapability.SEARCH,
        ExtensionCapability.DETAILS,
        ExtensionCapability.EPISODES,
        ExtensionCapability.STREAM_SOURCES,
        ExtensionCapability.AUTO_SKIP_SEGMENTS
    ),
    val supportedQualities: List<String> = listOf("1080p", "720p", "480p")
)

typealias ExtensionManifest = Extension

data class Repository(
    val url: String,
    val name: String,
    val description: String? = null,
    val extensionCount: Int = 0,
    val lastRefreshed: Long = System.currentTimeMillis(),
    val branch: String = "main",
    val author: String? = null,
    val websiteUrl: String? = null,
    val isValid: Boolean = true
)

typealias ExtensionRepo = Repository

data class AniListUser(
    val id: Long,
    val name: String,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val totalAnimeWatched: Int = 0,
    val totalEpisodesWatched: Int = 0,
    val daysWatched: Double = 0.0,
    val meanScore: Double = 0.0
)

data class AniListMediaListEntry(
    val id: Long,
    val mediaId: Int,
    val status: String, // CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING
    val progress: Int,
    val score: Double = 0.0,
    val updatedAt: Long = 0L,
    val anime: Anime
)

data class PlaybackProgress(
    val animeId: String,
    val animeTitle: String,
    val coverUrl: String?,
    val episodeNumber: Int,
    val episodeTitle: String,
    val currentPositionMs: Long,
    val durationMs: Long,
    val lastWatchedTimestamp: Long = System.currentTimeMillis(),
    val completed: Boolean = false
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

data class AppSettings(
    val darkTheme: Boolean = true,
    val amoledPureBlack: Boolean = true,
    val autoSkipIntro: Boolean = true,
    val autoSkipOutro: Boolean = true,
    val autoSkipRecap: Boolean = true,
    val defaultQuality: String = "1080p",
    val audioSubPreference: String = "Sub",
    val skipDurationSeconds: Int = 85,
    val playerGestures: Boolean = true,
    val playerTheme: String = "Dark OLED",
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoPlayNext: Boolean = true,
    val backgroundPlayback: Boolean = false,
    val pipEnabled: Boolean = true,
    val bufferCacheMb: Int = 128,
    val autoUpdateExtensions: Boolean = true,
    val hardwareAcceleration: Boolean = true,
    val hideAdultContent: Boolean = true,
    val unifiedLibrary: Boolean = true,
    val askForTrackingPermission: Boolean = true,
    val showCommunityRecommendations: Boolean = true,
    val cardStyle: String = "Saikou",
    val historyCardStyle: String = "Frosted Glass",
    val carouselStyle: String = "Classic",
    val navBarStyle: String = "Dynamic Pill",
    val navBarMargin: Int = 32,
    val preferredProviderId: String = "core_anilist",
    val syncConflictStrategy: String = "HIGHEST_PROGRESS",
    val autoSyncAniList: Boolean = true,
    val lastSyncedTimestamp: Long = 0L
)

