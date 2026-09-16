package com.example.core.extension

import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.Extension
import com.example.domain.model.ExtensionCapability
import com.example.domain.model.VideoSource

/**
 * Domain abstraction representing an anime extension scraper/provider.
 * Exposes anime search, anime details, episode listings, and stream source resolution
 * in a completely decoupled, UI-agnostic architecture.
 *
 * ## Real Extension Implementation Guidelines:
 *
 * Every production-ready extension targeting this system must adhere to the following contract:
 *
 * 1. **Manifest Integrity ([manifest]):**
 *    - Must specify a unique, deterministic [Extension.id] (e.g., `ext_crunchyroll`, `ext_animepahe`).
 *    - Must declare supported capabilities in [Extension.capabilities] ([ExtensionCapability.SEARCH],
 *      [ExtensionCapability.DETAILS], [ExtensionCapability.EPISODES], [ExtensionCapability.STREAM_SOURCES],
 *      [ExtensionCapability.AUTO_SKIP_SEGMENTS], [ExtensionCapability.MULTI_AUDIO], [ExtensionCapability.MULTI_SUBTITLES]).
 *    - Must define supported video resolutions in [Extension.supportedQualities] (e.g., `listOf("1080p", "720p", "480p")`).
 *    - Must declare whether the source contains NSFW/adult content via [Extension.isNsfw].
 *
 * 2. **Anime Search ([searchAnime]):**
 *    - Searches the provider's anime catalog using query string and 1-based pagination index.
 *    - Returns [Result.success] containing list of [Anime] domain entities.
 *    - Returns [Result.success] with an empty list if no matches are found.
 *    - Returns [Result.failure] on network, parsing, or server errors.
 *
 * 3. **Anime Details ([getAnimeDetails]):**
 *    - Fetches deep metadata for an anime given its ID or provider slug.
 *    - Must populate synopsis, genres, episodeCount, broadcast status, and banner/cover assets.
 *    - Returns [Result.failure] if the target title does not exist.
 *
 * 4. **Episode Listing ([getEpisodeList]):**
 *    - Fetches the ordered list of playable episodes.
 *    - Each [Episode] must include a 1-based [Episode.number], provider episode identifier [Episode.id],
 *      episode title, runtime duration in seconds, thumbnail URL, and filler flag.
 *
 * 5. **Stream Source Resolution ([resolveVideoSources]):**
 *    - Resolves playable video URLs (HLS `.m3u8`, DASH `.mpd`, or direct `.mp4` streams) for an episode.
 *    - Must provide required HTTP headers (e.g., `User-Agent`, `Referer`) if the media server enforces anti-hotlinking.
 *    - Should attach multi-audio tracks ([com.example.domain.model.AudioTrack]) and subtitle tracks ([com.example.domain.model.SubtitleTrack]).
 *    - Should attach intro, outro, and recap skip segment timestamps ([com.example.domain.model.SkipSegment]) in seconds.
 */
interface AnimeExtension {
    val manifest: Extension
    val capabilities: Set<ExtensionCapability>
        get() = manifest.capabilities

    /**
     * Search for anime titles matching the given query string.
     *
     * @param query Search keywords entered by user
     * @param page 1-based pagination page index
     * @return Result containing list of found [Anime] items or failure
     */
    suspend fun searchAnime(query: String, page: Int = 1): Result<List<Anime>>

    /**
     * Fetch comprehensive anime details and metadata.
     *
     * @param animeIdOrUrl Unique anime identifier or provider-specific URL slug
     * @return Result containing full [Anime] details or failure
     */
    suspend fun getAnimeDetails(animeIdOrUrl: String): Result<Anime>

    /**
     * Fetch the complete list of available episodes for an anime series.
     *
     * @param animeIdOrUrl Unique anime identifier or provider-specific URL slug
     * @return Result containing ordered list of [Episode] items or failure
     */
    suspend fun getEpisodeList(animeIdOrUrl: String): Result<List<Episode>>

    /**
     * Resolve playable streaming video sources for a specific anime episode.
     *
     * @param animeTitle Clean anime title for cross-referencing
     * @param episodeNumber 1-based episode number
     * @param episodeId Optional provider-specific episode ID / URL identifier
     * @return Result containing list of resolved [VideoSource] streams with subtitles, audio tracks, and skip markers
     */
    suspend fun resolveVideoSources(
        animeTitle: String,
        episodeNumber: Int,
        episodeId: String? = null
    ): Result<List<VideoSource>>
}
