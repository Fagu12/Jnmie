package com.example.data.repository

import com.example.core.resolver.AnikotoStreamResolver
import com.example.data.provider.KiwiProvider
import com.example.data.provider.MegaPlayProvider
import com.example.data.provider.MockTestSourceProvider
import com.example.data.provider.MultiSourceProvider
import com.example.data.provider.VidCloudProvider
import com.example.data.provider.VidstreamProvider
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.LanguagePreference
import com.example.domain.model.PlaybackSource
import com.example.domain.model.ProviderInfo
import com.example.domain.model.VideoSource
import com.example.domain.provider.AnimeProvider
import com.example.domain.provider.EpisodeProvider
import com.example.domain.repository.ProviderManager
import com.example.domain.repository.SourceResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * Implementation of [ProviderManager] managing authorized streaming providers.
 */
class ProviderManagerImpl(
    private val client: OkHttpClient = OkHttpClient()
) : ProviderManager {

    private val providers = listOf(
        MultiSourceProvider(client),
        VidstreamProvider(client),
        VidCloudProvider(client),
        KiwiProvider(client),
        MegaPlayProvider(client),
        MockTestSourceProvider()
    )

    private val _enabledMap = MutableStateFlow<Map<String, Boolean>>(
        providers.associate { it.id to true }
    )

    private val _registeredProviders = MutableStateFlow(providers)
    override val registeredProviders: Flow<List<AnimeProvider>> = _registeredProviders.asStateFlow()

    override suspend fun getAvailableProviders(): List<ProviderInfo> {
        val enabled = _enabledMap.value
        return providers.map {
            ProviderInfo(
                id = it.id,
                name = it.name,
                baseUrl = it.baseUrl,
                description = "Authorized streaming & embed source provider",
                supportedServers = it.supportedServers,
                isEnabled = enabled[it.id] ?: true,
                isAuthorized = true
            )
        }
    }

    override suspend fun getProviderById(id: String): AnimeProvider? {
        return providers.find { it.id == id }
    }

    override suspend fun setProviderEnabled(providerId: String, isEnabled: Boolean) {
        val updated = _enabledMap.value.toMutableMap()
        updated[providerId] = isEnabled
        _enabledMap.value = updated
        _registeredProviders.value = providers.filter { updated[it.id] != false }
    }
}

/**
 * Production implementation of [SourceResolver].
 * Coordinates stream resolution and failovers across registered authorized providers
 * (Vidstream, VidCloud, Kiwi, MegaPlay, and Multi-Source).
 */
class SourceResolverImpl(
    private val providerManager: ProviderManager,
    private val client: OkHttpClient = OkHttpClient()
) : SourceResolver {

    private val defaultProviders: List<AnimeProvider> = listOf(
        MultiSourceProvider(client),
        VidstreamProvider(client),
        VidCloudProvider(client),
        KiwiProvider(client),
        MegaPlayProvider(client),
        MockTestSourceProvider()
    )

    override val registeredProviders: List<AnimeProvider>
        get() = defaultProviders

    override suspend fun resolveSources(
        animeTitle: String,
        episodeNumber: Int,
        preferredProviderId: String?
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        val dummyEpisode = Episode(
            id = "ep_$episodeNumber",
            animeId = animeTitle,
            number = episodeNumber,
            title = "Episode $episodeNumber"
        )
        resolveSourcesForEpisode(
            animeTitle = animeTitle,
            episode = dummyEpisode,
            preferredProviderId = preferredProviderId
        )
    }

    override suspend fun resolveSourcesForEpisode(
        animeTitle: String,
        episode: Episode,
        preferredProviderId: String?,
        languagePreference: LanguagePreference
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        val allSources = mutableListOf<VideoSource>()
        var lastError: Throwable? = null

        // 1. If episode id is directly an embed/stream URL, resolve it first
        if (episode.id.startsWith("http")) {
            val directResult = resolveFromEmbedUrl(episode.id, emptyMap(), "Authorized Stream")
            if (directResult.isSuccess && !directResult.getOrNull().isNullOrEmpty()) {
                return@withContext directResult
            }
        }

        // 2. Select target providers according to preferredProviderId
        val orderedProviders = if (!preferredProviderId.isNullOrBlank()) {
            defaultProviders.sortedByDescending { it.id == preferredProviderId }
        } else {
            defaultProviders
        }

        for (provider in orderedProviders) {
            try {
                // If it's a multi-source coordinator, search & resolve
                if (provider is MultiSourceProvider) {
                    val searchResult = provider.searchAnime(animeTitle)
                    val targetAnime = searchResult.getOrNull()?.firstOrNull()
                    if (targetAnime != null) {
                        val eps = provider.getEpisodes(targetAnime.id).getOrNull()
                        val targetEp = eps?.find { it.number == episode.number } ?: eps?.firstOrNull()
                        if (targetEp != null) {
                            val streamResult = provider.resolveStream(targetEp.id, null, languagePreference)
                            val resolved = streamResult.getOrNull()
                            if (!resolved.isNullOrEmpty()) {
                                allSources.addAll(resolved)
                                return@withContext Result.success(allSources)
                            }
                        }
                    }
                } else {
                    // Try resolving directly via provider
                    val streamResult = provider.resolveStream(episode.id, null, languagePreference)
                    val resolved = streamResult.getOrNull()
                    if (!resolved.isNullOrEmpty()) {
                        allSources.addAll(resolved)
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (allSources.isNotEmpty()) {
            Result.success(allSources)
        } else {
            Result.failure(
                lastError ?: IllegalStateException("No video sources available for $animeTitle episode ${episode.number} across authorized providers.")
            )
        }
    }

    override suspend fun resolveFromEmbedUrl(
        embedUrl: String,
        headers: Map<String, String>,
        serverName: String
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        try {
            val playback = AnikotoStreamResolver.resolveFromEmbedUrl(embedUrl, headers, client)
            if (playback != null) {
                val sources = AnikotoStreamResolver.convertToVideoSources(
                    serverName = serverName,
                    playbackSource = playback,
                    isDub = false
                )
                Result.success(sources)
            } else {
                Result.failure(Exception("Unable to resolve playable media from authorized embed URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolveAnimeDetails(
        animeTitle: String,
        animeId: String?,
        preferredProviderId: String?
    ): Result<Anime> = withContext(Dispatchers.IO) {
        val multiSource = defaultProviders.filterIsInstance<MultiSourceProvider>().firstOrNull()
        if (multiSource != null) {
            val list = multiSource.searchAnime(animeTitle).getOrNull()
            val match = list?.firstOrNull()
            if (match != null) {
                return@withContext Result.success(match)
            }
        }
        Result.failure(Exception("Anime details not found for $animeTitle"))
    }

    override suspend fun resolveEpisodes(
        animeTitle: String,
        animeId: String?,
        preferredProviderId: String?
    ): Result<List<Episode>> = withContext(Dispatchers.IO) {
        val multiSource = defaultProviders.filterIsInstance<MultiSourceProvider>().firstOrNull()
        if (multiSource != null) {
            val episodesResult = multiSource.getEpisodes(animeTitle, animeId)
            if (episodesResult.isSuccess) {
                return@withContext episodesResult
            }
        }
        Result.failure(Exception("No episodes found for $animeTitle"))
    }
}
