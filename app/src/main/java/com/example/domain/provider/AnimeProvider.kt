package com.example.domain.provider

import com.example.domain.model.AnikotoServer
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.LanguagePreference
import com.example.domain.model.ProviderInfo
import com.example.domain.model.VideoSource
import kotlinx.coroutines.flow.Flow

/**
 * Capabilities supported by an [AnimeProvider].
 */
enum class ProviderCapability {
    SEARCH,
    ANIME_DETAILS,
    TRENDING,
    LATEST_EPISODES,
    EPISODE_LIST,
    STREAM_RESOLUTION,
    MULTI_SERVER,
    EMBED_EXTRACTION,
    SUBTITLE_INJECTION,
    DUB_AUDIO
}

/**
 * Dedicated contract for episode listings.
 */
interface EpisodeProvider {
    suspend fun getEpisodes(animeTitle: String, animeId: String? = null): Result<List<Episode>>
}

/**
 * Clean domain abstraction for an authorized anime metadata and streaming provider.
 * Completely decoupled from Android UI, ViewModel, and Media3/ExoPlayer layers.
 */
interface AnimeProvider : EpisodeProvider {
    val id: String
    val name: String
    val baseUrl: String
    val iconUrl: String? get() = null
    val supportedServers: List<String> get() = listOf(name)
    val isEnabled: Boolean get() = true
    val isAuthorized: Boolean get() = true
    val priority: Int get() = 0
    val capabilities: Set<ProviderCapability> get() = setOf(
        ProviderCapability.STREAM_RESOLUTION,
        ProviderCapability.EPISODE_LIST
    )

    fun hasCapability(capability: ProviderCapability): Boolean = capabilities.contains(capability)

    /**
     * Searches for anime by query string.
     */
    suspend fun searchAnime(query: String, page: Int = 1): Result<List<Anime>> = Result.success(emptyList())

    /**
     * Fetches detailed metadata for a specific anime ID.
     */
    suspend fun getAnimeDetails(animeId: String): Result<Anime> = Result.failure(
        UnsupportedOperationException("Anime details not supported by $name")
    )

    /**
     * Fetches trending or popular anime from this provider.
     */
    suspend fun getTrendingAnime(page: Int = 1): Result<List<Anime>> = Result.success(emptyList())

    /**
     * Fetches recently updated anime or episodes.
     */
    suspend fun getLatestUpdates(page: Int = 1): Result<List<Anime>> = Result.success(emptyList())

    /**
     * Fetches the complete episode list for an anime given its ID.
     */
    suspend fun getEpisodes(animeId: String): Result<List<Episode>> = Result.success(emptyList())

    /**
     * Fetches episode list given an anime title and optional fallback ID.
     */
    override suspend fun getEpisodes(animeTitle: String, animeId: String?): Result<List<Episode>> {
        val targetId = animeId ?: run {
            searchAnime(animeTitle, 1).getOrNull()?.firstOrNull()?.id ?: ""
        }
        return if (targetId.isNotBlank()) {
            getEpisodes(targetId)
        } else {
            Result.failure(NoSuchElementException("No anime found for title: $animeTitle in provider $name"))
        }
    }

    /**
     * Discovers available streaming server options for a given episode ID.
     */
    suspend fun getServerList(episodeId: String): Result<List<AnikotoServer>> = Result.success(
        supportedServers.map { serverName ->
            AnikotoServer(
                id = serverName.lowercase().replace(" ", "_"),
                name = serverName,
                type = "sub",
                episodeId = episodeId,
                embedUrl = if (episodeId.startsWith("http")) episodeId else "$baseUrl/e/$episodeId"
            )
        }
    )

    /**
     * Resolves direct, playable video stream sources (HLS/MP4) for a given episode ID, server, and language.
     */
    suspend fun resolveStream(
        episodeId: String,
        server: String? = null,
        language: LanguagePreference = LanguagePreference.SUB
    ): Result<List<VideoSource>>

    /**
     * Convenience method to resolve stream sources given anime title and episode number.
     */
    suspend fun resolveEpisodeStreams(
        animeTitle: String,
        episodeNumber: Int,
        server: String? = null,
        language: LanguagePreference = LanguagePreference.SUB
    ): Result<List<VideoSource>> {
        val episodes = getEpisodes(animeTitle).getOrNull()
        val targetEpisode = episodes?.find { it.number == episodeNumber } ?: episodes?.firstOrNull()
            ?: return Result.failure(NoSuchElementException("Episode $episodeNumber not found for $animeTitle in $name"))
        return resolveStream(targetEpisode.id, server, language)
    }
}

/**
 * Dedicated contract for extracting media streams from raw embed URLs or hosters.
 */
interface StreamExtractor {
    val serverName: String
    val supportedDomains: List<String>
    suspend fun extract(embedUrl: String, headers: Map<String, String> = emptyMap()): Result<List<VideoSource>>
}

/**
 * Domain coordinator for resolving streams across all registered authorized providers.
 */
interface SourceResolver {
    val registeredProviders: List<AnimeProvider>

    suspend fun resolveSources(
        animeTitle: String,
        episodeNumber: Int,
        preferredProviderId: String? = null
    ): Result<List<VideoSource>>

    suspend fun resolveSourcesForEpisode(
        animeTitle: String,
        episode: Episode,
        preferredProviderId: String? = null,
        languagePreference: LanguagePreference = LanguagePreference.SUB
    ): Result<List<VideoSource>>

    suspend fun resolveFromEmbedUrl(
        embedUrl: String,
        headers: Map<String, String> = emptyMap(),
        serverName: String = "Authorized Stream"
    ): Result<List<VideoSource>>

    suspend fun resolveAnimeDetails(
        animeTitle: String,
        animeId: String? = null,
        preferredProviderId: String? = null
    ): Result<Anime>

    suspend fun resolveEpisodes(
        animeTitle: String,
        animeId: String? = null,
        preferredProviderId: String? = null
    ): Result<List<Episode>>
}

/**
 * Domain coordinator for managing and querying anime providers.
 */
interface ProviderManager {
    val registeredProviders: Flow<List<AnimeProvider>>
    suspend fun getAvailableProviders(): List<ProviderInfo>
    suspend fun getProviderById(id: String): AnimeProvider?
    suspend fun setProviderEnabled(providerId: String, isEnabled: Boolean)
}
