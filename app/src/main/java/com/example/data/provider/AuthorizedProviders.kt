package com.example.data.provider

import com.example.core.resolver.AnikotoStreamResolver
import com.example.domain.model.AnikotoServer
import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.LanguagePreference
import com.example.domain.model.VideoSource
import com.example.domain.provider.AnimeProvider
import com.example.domain.provider.EpisodeProvider
import com.example.domain.provider.ProviderCapability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Clean implementation for Vidstream Authorized Provider.
 */
class VidstreamProvider(
    private val client: OkHttpClient = OkHttpClient()
) : AnimeProvider {
    override val id: String = "provider_vidstream"
    override val name: String = "Vidstream"
    override val baseUrl: String = "https://vidstream.pro"
    override val priority: Int = 10
    override val supportedServers: List<String> = listOf("Vidstream", "MegaCloud", "Vidstream-HD")
    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.STREAM_RESOLUTION,
        ProviderCapability.MULTI_SERVER,
        ProviderCapability.EMBED_EXTRACTION,
        ProviderCapability.DUB_AUDIO
    )

    override suspend fun searchAnime(query: String, page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun getEpisodes(animeId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun resolveStream(
        episodeId: String,
        server: String?,
        language: LanguagePreference
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        try {
            val playbackSource = AnikotoStreamResolver.resolveVidstreamEmbed(
                embedUrl = if (episodeId.startsWith("http")) episodeId else "$baseUrl/e/$episodeId",
                client = client,
                headers = mapOf("Referer" to baseUrl)
            )

            if (playbackSource != null) {
                val sources = AnikotoStreamResolver.convertToVideoSources(
                    serverName = server ?: "Vidstream",
                    playbackSource = playbackSource,
                    isDub = language == LanguagePreference.DUB
                )
                Result.success(sources)
            } else {
                Result.failure(NoSuchElementException("Could not resolve stream from Vidstream for $episodeId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Clean implementation for VidCloud Authorized Provider.
 */
class VidCloudProvider(
    private val client: OkHttpClient = OkHttpClient()
) : AnimeProvider {
    override val id: String = "provider_vidcloud"
    override val name: String = "VidCloud"
    override val baseUrl: String = "https://vidcloud.co"
    override val priority: Int = 8
    override val supportedServers: List<String> = listOf("VidCloud", "VizCloud", "Cloud9")
    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.STREAM_RESOLUTION,
        ProviderCapability.MULTI_SERVER,
        ProviderCapability.EMBED_EXTRACTION,
        ProviderCapability.DUB_AUDIO
    )

    override suspend fun searchAnime(query: String, page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun getEpisodes(animeId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun resolveStream(
        episodeId: String,
        server: String?,
        language: LanguagePreference
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        try {
            val playbackSource = AnikotoStreamResolver.resolveVidcloudEmbed(
                embedUrl = if (episodeId.startsWith("http")) episodeId else "$baseUrl/e/$episodeId",
                client = client,
                headers = mapOf("Referer" to baseUrl)
            )

            if (playbackSource != null) {
                val sources = AnikotoStreamResolver.convertToVideoSources(
                    serverName = server ?: "VidCloud",
                    playbackSource = playbackSource,
                    isDub = language == LanguagePreference.DUB
                )
                Result.success(sources)
            } else {
                Result.failure(NoSuchElementException("Could not resolve stream from VidCloud for $episodeId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Clean implementation for Kiwi Authorized Provider (Direct MP4 / HLS).
 */
class KiwiProvider(
    private val client: OkHttpClient = OkHttpClient()
) : AnimeProvider {
    override val id: String = "provider_kiwi"
    override val name: String = "Kiwi"
    override val baseUrl: String = "https://kiwi.properties"
    override val priority: Int = 6
    override val supportedServers: List<String> = listOf("Kiwi", "Kiwi-Fast", "Kiwi-Direct")
    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.STREAM_RESOLUTION,
        ProviderCapability.EMBED_EXTRACTION
    )

    override suspend fun searchAnime(query: String, page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun getEpisodes(animeId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun resolveStream(
        episodeId: String,
        server: String?,
        language: LanguagePreference
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        try {
            val playbackSource = AnikotoStreamResolver.resolveKiwiEmbed(
                embedUrl = if (episodeId.startsWith("http")) episodeId else "$baseUrl/e/$episodeId",
                client = client,
                headers = mapOf("Referer" to baseUrl)
            )

            if (playbackSource != null) {
                val sources = AnikotoStreamResolver.convertToVideoSources(
                    serverName = server ?: "Kiwi",
                    playbackSource = playbackSource,
                    isDub = language == LanguagePreference.DUB
                )
                Result.success(sources)
            } else {
                Result.failure(NoSuchElementException("Could not resolve stream from Kiwi for $episodeId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Clean implementation for MegaPlay Authorized Provider.
 */
class MegaPlayProvider(
    private val client: OkHttpClient = OkHttpClient()
) : AnimeProvider {
    override val id: String = "provider_megaplay"
    override val name: String = "MegaPlay"
    override val baseUrl: String = "https://megaplay.top"
    override val priority: Int = 7
    override val supportedServers: List<String> = listOf("MegaPlay", "VidPlay", "MegaStream")
    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.STREAM_RESOLUTION,
        ProviderCapability.MULTI_SERVER,
        ProviderCapability.EMBED_EXTRACTION,
        ProviderCapability.DUB_AUDIO
    )

    override suspend fun searchAnime(query: String, page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun getEpisodes(animeId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun resolveStream(
        episodeId: String,
        server: String?,
        language: LanguagePreference
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        try {
            val playbackSource = AnikotoStreamResolver.resolveMegaPlayEmbed(
                embedUrl = if (episodeId.startsWith("http")) episodeId else "$baseUrl/e/$episodeId",
                client = client,
                headers = mapOf("Referer" to baseUrl)
            )

            if (playbackSource != null) {
                val sources = AnikotoStreamResolver.convertToVideoSources(
                    serverName = server ?: "MegaPlay",
                    playbackSource = playbackSource,
                    isDub = language == LanguagePreference.DUB
                )
                Result.success(sources)
            } else {
                Result.failure(NoSuchElementException("Could not resolve stream from MegaPlay for $episodeId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * MultiSource Coordinator Provider.
 * Integrates search, episode listings, and server coordination across all authorized providers.
 */
class MultiSourceProvider(
    private val client: OkHttpClient = OkHttpClient()
) : AnimeProvider {
    override val id: String = "provider_multisource"
    override val name: String = "Anikoto Multi-Source"
    override val baseUrl: String = "https://api.consumet.org/anime/gogoanime"
    override val priority: Int = 100
    override val supportedServers: List<String> = listOf("Vidstream", "VidCloud", "Kiwi", "MegaPlay")
    override val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.SEARCH,
        ProviderCapability.ANIME_DETAILS,
        ProviderCapability.TRENDING,
        ProviderCapability.LATEST_EPISODES,
        ProviderCapability.EPISODE_LIST,
        ProviderCapability.STREAM_RESOLUTION,
        ProviderCapability.MULTI_SERVER,
        ProviderCapability.EMBED_EXTRACTION,
        ProviderCapability.DUB_AUDIO
    )

    override suspend fun searchAnime(query: String, page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/$encoded?page=$page"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Search failed HTTP ${resp.code}"))
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val resultsArr = json.optJSONArray("results") ?: JSONArray()
                val list = mutableListOf<Anime>()
                for (i in 0 until resultsArr.length()) {
                    val obj = resultsArr.getJSONObject(i)
                    list.add(
                        Anime(
                            id = obj.optString("id", "anime_$i"),
                            title = obj.optString("title", "Unknown Anime"),
                            coverUrl = obj.optString("image", null),
                            score = obj.optDouble("rating", 0.0),
                            status = obj.optString("status", "RELEASING")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAnimeDetails(animeId: String): Result<Anime> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.consumet.org/anime/gogoanime/info/$animeId"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Get anime details failed HTTP ${resp.code}"))
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val anime = Anime(
                    id = json.optString("id", animeId),
                    title = json.optString("title", "Unknown Anime"),
                    coverUrl = json.optString("image", null),
                    description = json.optString("description", null),
                    status = json.optString("status", "RELEASING"),
                    totalEpisodes = json.optJSONArray("episodes")?.length() ?: 0,
                    sourceProviderId = id
                )
                Result.success(anime)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEpisodes(animeId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.consumet.org/anime/gogoanime/info/$animeId"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Get episodes failed HTTP ${resp.code}"))
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val epArr = json.optJSONArray("episodes") ?: JSONArray()
                val list = mutableListOf<Episode>()
                for (i in 0 until epArr.length()) {
                    val obj = epArr.getJSONObject(i)
                    val num = obj.optInt("number", i + 1)
                    val epId = obj.optString("id", "$animeId-episode-$num")
                    list.add(
                        Episode(
                            id = epId,
                            animeId = animeId,
                            number = num,
                            title = obj.optString("title", "Episode $num"),
                            durationSeconds = 1440L
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEpisodes(animeTitle: String, animeId: String?): Result<List<Episode>> {
        val targetId = animeId ?: run {
            val searchRes = searchAnime(animeTitle, 1)
            searchRes.getOrNull()?.firstOrNull()?.id ?: ""
        }
        return if (targetId.isNotBlank()) {
            getEpisodes(targetId)
        } else {
            Result.failure(NoSuchElementException("No anime found for title: $animeTitle"))
        }
    }

    override suspend fun getServerList(episodeId: String): Result<List<AnikotoServer>> = withContext(Dispatchers.IO) {
        try {
            if (episodeId.startsWith("http")) {
                return@withContext Result.success(
                    listOf(
                        AnikotoServer(
                            id = "direct",
                            name = "Direct Stream",
                            type = "sub",
                            episodeId = episodeId,
                            embedUrl = episodeId
                        )
                    )
                )
            }
            val url = "https://api.consumet.org/anime/gogoanime/servers/$episodeId"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Failed to fetch servers HTTP ${resp.code}"))
                val body = resp.body?.string().orEmpty()
                val serversArr = JSONArray(body)
                val servers = mutableListOf<AnikotoServer>()
                for (i in 0 until serversArr.length()) {
                    val sObj = serversArr.getJSONObject(i)
                    val sName = sObj.optString("name", "Server $i")
                    val embedUrl = sObj.optString("url", "")
                    if (embedUrl.isNotBlank()) {
                        servers.add(
                            AnikotoServer(
                                id = sName.lowercase().replace(" ", "_"),
                                name = sName,
                                type = "sub",
                                episodeId = episodeId,
                                embedUrl = embedUrl
                            )
                        )
                    }
                }
                Result.success(servers)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolveStream(
        episodeId: String,
        server: String?,
        language: LanguagePreference
    ): Result<List<VideoSource>> = withContext(Dispatchers.IO) {
        try {
            // First check if episodeId is a direct embed URL
            if (episodeId.startsWith("http")) {
                val playbackSource = AnikotoStreamResolver.resolveFromEmbedUrl(episodeId, emptyMap<String, String>(), client)
                if (playbackSource != null) {
                    val sources = AnikotoStreamResolver.convertToVideoSources(
                        serverName = server ?: "Authorized Stream",
                        playbackSource = playbackSource,
                        isDub = language == LanguagePreference.DUB
                    )
                    return@withContext Result.success(sources)
                }
            }

            // Otherwise query provider streaming servers
            val url = "https://api.consumet.org/anime/gogoanime/servers/$episodeId"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to fetch servers HTTP ${resp.code}"))
                }
                val body = resp.body?.string().orEmpty()
                val serversArr = JSONArray(body)
                val allSources = mutableListOf<VideoSource>()

                for (i in 0 until serversArr.length()) {
                    val sObj = serversArr.getJSONObject(i)
                    val sName = sObj.optString("name", "Server $i")
                    val embedUrl = sObj.optString("url", "")
                    if (embedUrl.isNotBlank()) {
                        val playback = AnikotoStreamResolver.resolveFromEmbedUrl(embedUrl, emptyMap<String, String>(), client)
                        if (playback != null) {
                            val resolved = AnikotoStreamResolver.convertToVideoSources(
                                serverName = sName,
                                playbackSource = playback,
                                isDub = language == LanguagePreference.DUB
                            )
                            allSources.addAll(resolved)
                        }
                    }
                }

                if (allSources.isNotEmpty()) {
                    Result.success(allSources)
                } else {
                    Result.failure(NoSuchElementException("No playable streams found on authorized servers for $episodeId"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
