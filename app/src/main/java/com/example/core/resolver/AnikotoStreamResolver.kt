package com.example.core.resolver

import android.util.Log
import com.example.domain.model.AudioTrack
import com.example.domain.model.PlaybackSource
import com.example.domain.model.PlaybackSource.Hls
import com.example.domain.model.PlaybackSource.Mp4
import com.example.domain.model.PlaybackSource.Embed
import com.example.domain.model.SubtitleTrack
import com.example.domain.model.VideoSource
import com.example.domain.model.AnikotoServer
import com.example.domain.model.LanguagePreference
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Anikoto & Multi-Server Stream Resolver.
 * Resolves Anikoto episode servers (Vidstream, VidCloud, Kiwi, MegaPlay, VidPlay, etc.)
 * to actual playback sources (HLS .m3u8, MP4, or supported Embed) rather than passing
 * raw embed URLs to ExoPlayer.
 */
object AnikotoStreamResolver {

    private const val TAG = "ANIKOTO"
    private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    private val defaultClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Fallback priority order for server selection.
     * 1. Vidstream
     * 2. VidCloud
     * 3. Kiwi
     * 4. MegaPlay / VidPlay
     * 5. Other servers actually returned by Anikoto.
     */
    fun sortServersByPriority(servers: List<AnikotoServer>): List<AnikotoServer> {
        return servers.sortedWith(compareBy { server ->
            val name = server.name.lowercase()
            when {
                name.contains("vidstream") -> 1
                name.contains("vidcloud") || name.contains("vizcloud") -> 2
                name.contains("kiwi") -> 3
                name.contains("megaplay") || name.contains("vidplay") -> 4
                else -> 5
            }
        })
    }

    /**
     * Filters and orders servers based on user language preference (SUB, DUB, AUTO).
     * If DUB is requested and available, returns DUB servers; otherwise falls back to SUB.
     */
    fun filterServersByLanguage(
        servers: List<AnikotoServer>,
        languagePreference: LanguagePreference
    ): List<AnikotoServer> {
        if (servers.isEmpty()) return emptyList()

        return when (languagePreference) {
            LanguagePreference.DUB -> {
                val dubServers = servers.filter { it.type.equals("dub", ignoreCase = true) || it.name.contains("dub", ignoreCase = true) }
                if (dubServers.isNotEmpty()) {
                    sortServersByPriority(dubServers)
                } else {
                    // Fallback to SUB if DUB isn't available
                    sortServersByPriority(servers)
                }
            }
            LanguagePreference.SUB -> {
                val subServers = servers.filter { !it.type.equals("dub", ignoreCase = true) && !it.name.contains("dub", ignoreCase = true) }
                if (subServers.isNotEmpty()) {
                    sortServersByPriority(subServers)
                } else {
                    sortServersByPriority(servers)
                }
            }
            LanguagePreference.AUTO -> {
                sortServersByPriority(servers)
            }
        }
    }

    /**
     * Resolves an Anikoto server embedUrl or server ID to an actual PlaybackSource.
     */
    suspend fun resolveServerStream(
        animeTitle: String,
        episodeNumber: Int,
        server: AnikotoServer,
        language: LanguagePreference,
        client: OkHttpClient = defaultClient
    ): Result<PlaybackSource> {
        val embedUrl = server.embedUrl.trim()
        val serverName = server.name

        // Check if embedUrl is already a direct video stream (.m3u8 or .mp4)
        if (isDirectStreamUrl(embedUrl)) {
            val headers = createPlaybackHeaders(embedUrl)
            val isHls = embedUrl.contains(".m3u8", ignoreCase = true)
            val source = if (isHls) {
                PlaybackSource.Hls(url = embedUrl, headers = headers)
            } else {
                PlaybackSource.Mp4(url = embedUrl, headers = headers)
            }
            logResolution(
                anime = animeTitle,
                episode = episodeNumber,
                language = language.label,
                server = serverName,
                embedUrl = embedUrl,
                resolver = "DirectStream",
                playbackMode = if (isHls) "HLS" else "MP4",
                sourceType = if (isHls) "m3u8" else "mp4",
                resolved = embedUrl
            )
            return Result.success(source)
        }

        // 1. Resolve MegaPlay / megaplay.buzz
        if (embedUrl.contains("megaplay", ignoreCase = true)) {
            val megaResult = resolveMegaPlayStream(animeTitle, episodeNumber, server, language, client)
            if (megaResult.isSuccess) return megaResult
        }

        // 2. Resolve Vidstream / Vidcloud / Rabbitstream / Megacloud
        if (embedUrl.contains("vidstream", ignoreCase = true) ||
            embedUrl.contains("vidcloud", ignoreCase = true) ||
            embedUrl.contains("vizcloud", ignoreCase = true) ||
            embedUrl.contains("rabbitstream", ignoreCase = true) ||
            embedUrl.contains("megacloud", ignoreCase = true)
        ) {
            val vidResult = resolveVidstreamStream(animeTitle, episodeNumber, server, language, client)
            if (vidResult.isSuccess) return vidResult
        }

        // 3. Resolve Kiwi Stream / Filelions / Streamwish
        if (embedUrl.contains("kiwi", ignoreCase = true) ||
            embedUrl.contains("streamwish", ignoreCase = true) ||
            embedUrl.contains("filelions", ignoreCase = true)
        ) {
            val kiwiResult = resolveGenericHosterStream(animeTitle, episodeNumber, server, language, client)
            if (kiwiResult.isSuccess) return kiwiResult
        }

        // 4. Try generic stream resolution endpoint or HTML scraping
        val genericResult = resolveFromEmbedPageOrApi(animeTitle, episodeNumber, server, language, client)
        if (genericResult.isSuccess) return genericResult

        // 5. If extraction produces an embed fallback
        logResolution(
            anime = animeTitle,
            episode = episodeNumber,
            language = language.label,
            server = serverName,
            embedUrl = embedUrl,
            resolver = "EmbedFallback",
            playbackMode = "EmbedOnly",
            sourceType = "embed",
            resolved = embedUrl
        )
        return Result.success(PlaybackSource.Embed(url = embedUrl))
    }

    /**
     * Resolves MegaPlay embed URLs (e.g. https://megaplay.buzz/stream/s-2/... or https://megaplay.buzz/embed/...)
     */
    private fun resolveMegaPlayStream(
        animeTitle: String,
        episodeNumber: Int,
        server: AnikotoServer,
        language: LanguagePreference,
        client: OkHttpClient
    ): Result<PlaybackSource> {
        val embedUrl = server.embedUrl
        val embedId = extractIdFromUrl(embedUrl)

        val candidateApiUrls = mutableListOf<String>()
        if (embedId.isNotBlank()) {
            candidateApiUrls.add("https://megaplay.buzz/stream/getSources?id=$embedId")
            candidateApiUrls.add("https://megaplay.buzz/api/source/$embedId")
        }
        candidateApiUrls.add(embedUrl)

        for (apiUrl in candidateApiUrls) {
            try {
                val reqBuilder = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .header("Referer", "https://megaplay.buzz/")
                    .header("X-Requested-With", "XMLHttpRequest")

                val response = client.newCall(reqBuilder.build()).execute()
                val body = response.body?.string() ?: continue

                if (response.code == 410) {
                    Log.w(TAG, "MegaPlay returned HTTP 410 for $apiUrl")
                    continue
                }

                if (response.isSuccessful && body.isNotBlank()) {
                    val parsed = parseStreamJsonResponse(body, "https://megaplay.buzz/")
                    if (parsed != null) {
                        logResolution(
                            anime = animeTitle,
                            episode = episodeNumber,
                            language = language.label,
                            server = server.name,
                            embedUrl = embedUrl,
                            resolver = "MegaPlayResolver",
                            playbackMode = if (parsed is PlaybackSource.Hls) "HLS" else "MP4",
                            sourceType = if (parsed is PlaybackSource.Hls) "m3u8" else "mp4",
                            resolved = when (parsed) {
                                is PlaybackSource.Hls -> parsed.url
                                is PlaybackSource.Mp4 -> parsed.url
                                is PlaybackSource.Embed -> parsed.url
                            }
                        )
                        return Result.success(parsed)
                    }

                    // Try regex search in HTML page for .m3u8
                    val m3u8Match = extractM3u8FromHtml(body)
                    if (m3u8Match != null) {
                        val headers = mapOf(
                            "Referer" to "https://megaplay.buzz/",
                            "User-Agent" to DEFAULT_USER_AGENT
                        )
                        val hlsSource = PlaybackSource.Hls(url = m3u8Match, headers = headers)
                        logResolution(
                            anime = animeTitle,
                            episode = episodeNumber,
                            language = language.label,
                            server = server.name,
                            embedUrl = embedUrl,
                            resolver = "MegaPlayHtmlRegex",
                            playbackMode = "HLS",
                            sourceType = "m3u8",
                            resolved = m3u8Match
                        )
                        return Result.success(hlsSource)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "MegaPlay resolve candidate failed: ${e.message}")
            }
        }

        return Result.failure(IOException("Failed to resolve MegaPlay stream for $embedUrl"))
    }

    /**
     * Resolves Vidstream / Vidcloud / Vizcloud embeds.
     */
    private fun resolveVidstreamStream(
        animeTitle: String,
        episodeNumber: Int,
        server: AnikotoServer,
        language: LanguagePreference,
        client: OkHttpClient
    ): Result<PlaybackSource> {
        val embedUrl = server.embedUrl
        val embedId = extractIdFromUrl(embedUrl)
        val host = getDomain(embedUrl)

        val candidateUrls = mutableListOf<String>()
        if (embedId.isNotBlank()) {
            candidateUrls.add("https://$host/embed-2/ajax/e-1/getSources?id=$embedId")
            candidateUrls.add("https://$host/ajax/episode/sources?id=$embedId")
            candidateUrls.add("https://$host/stream/getSources?id=$embedId")
        }
        candidateUrls.add(embedUrl)

        for (apiUrl in candidateUrls) {
            try {
                val reqBuilder = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .header("Referer", embedUrl)
                    .header("X-Requested-With", "XMLHttpRequest")

                val response = client.newCall(reqBuilder.build()).execute()
                val body = response.body?.string() ?: continue

                if (response.code == 410) {
                    Log.w(TAG, "Vidstream returned HTTP 410 for $apiUrl")
                    continue
                }

                if (response.isSuccessful && body.isNotBlank()) {
                    val parsed = parseStreamJsonResponse(body, embedUrl)
                    if (parsed != null) {
                        logResolution(
                            anime = animeTitle,
                            episode = episodeNumber,
                            language = language.label,
                            server = server.name,
                            embedUrl = embedUrl,
                            resolver = "VidstreamResolver",
                            playbackMode = if (parsed is PlaybackSource.Hls) "HLS" else "MP4",
                            sourceType = if (parsed is PlaybackSource.Hls) "m3u8" else "mp4",
                            resolved = when (parsed) {
                                is PlaybackSource.Hls -> parsed.url
                                is PlaybackSource.Mp4 -> parsed.url
                                is PlaybackSource.Embed -> parsed.url
                            }
                        )
                        return Result.success(parsed)
                    }

                    val m3u8Match = extractM3u8FromHtml(body)
                    if (m3u8Match != null) {
                        val headers = mapOf(
                            "Referer" to embedUrl,
                            "User-Agent" to DEFAULT_USER_AGENT
                        )
                        val hlsSource = PlaybackSource.Hls(url = m3u8Match, headers = headers)
                        logResolution(
                            anime = animeTitle,
                            episode = episodeNumber,
                            language = language.label,
                            server = server.name,
                            embedUrl = embedUrl,
                            resolver = "VidstreamHtmlRegex",
                            playbackMode = "HLS",
                            sourceType = "m3u8",
                            resolved = m3u8Match
                        )
                        return Result.success(hlsSource)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Vidstream candidate failed: ${e.message}")
            }
        }

        return Result.failure(IOException("Failed to resolve Vidstream stream for $embedUrl"))
    }

    /**
     * Resolves generic hoster streams (Kiwi, Streamwish, Filelions).
     */
    private fun resolveGenericHosterStream(
        animeTitle: String,
        episodeNumber: Int,
        server: AnikotoServer,
        language: LanguagePreference,
        client: OkHttpClient
    ): Result<PlaybackSource> {
        val embedUrl = server.embedUrl
        try {
            val req = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Referer", embedUrl)
                .build()

            val response = client.newCall(req).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotBlank()) {
                val parsed = parseStreamJsonResponse(body, embedUrl)
                if (parsed != null) return Result.success(parsed)

                val m3u8Match = extractM3u8FromHtml(body)
                if (m3u8Match != null) {
                    val headers = mapOf("Referer" to embedUrl, "User-Agent" to DEFAULT_USER_AGENT)
                    val hlsSource = PlaybackSource.Hls(url = m3u8Match, headers = headers)
                    logResolution(
                        anime = animeTitle,
                        episode = episodeNumber,
                        language = language.label,
                        server = server.name,
                        embedUrl = embedUrl,
                        resolver = "GenericHosterRegex",
                        playbackMode = "HLS",
                        sourceType = "m3u8",
                        resolved = m3u8Match
                    )
                    return Result.success(hlsSource)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Generic hoster resolve failed: ${e.message}")
        }
        return Result.failure(IOException("Failed to resolve generic hoster stream"))
    }

    /**
     * Resolves from embed page or API endpoint by inspecting the response.
     */
    private fun resolveFromEmbedPageOrApi(
        animeTitle: String,
        episodeNumber: Int,
        server: AnikotoServer,
        language: LanguagePreference,
        client: OkHttpClient
    ): Result<PlaybackSource> {
        val embedUrl = server.embedUrl
        try {
            val req = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Referer", embedUrl)
                .build()

            val response = client.newCall(req).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotBlank()) {
                val parsed = parseStreamJsonResponse(body, embedUrl)
                if (parsed != null) {
                    logResolution(
                        anime = animeTitle,
                        episode = episodeNumber,
                        language = language.label,
                        server = server.name,
                        embedUrl = embedUrl,
                        resolver = "EmbedPageJson",
                        playbackMode = if (parsed is PlaybackSource.Hls) "HLS" else "MP4",
                        sourceType = if (parsed is PlaybackSource.Hls) "m3u8" else "mp4",
                        resolved = when (parsed) {
                            is PlaybackSource.Hls -> parsed.url
                            is PlaybackSource.Mp4 -> parsed.url
                            is PlaybackSource.Embed -> parsed.url
                        }
                    )
                    return Result.success(parsed)
                }

                val m3u8Match = extractM3u8FromHtml(body)
                if (m3u8Match != null) {
                    val headers = mapOf("Referer" to embedUrl, "User-Agent" to DEFAULT_USER_AGENT)
                    val hlsSource = PlaybackSource.Hls(url = m3u8Match, headers = headers)
                    logResolution(
                        anime = animeTitle,
                        episode = episodeNumber,
                        language = language.label,
                        server = server.name,
                        embedUrl = embedUrl,
                        resolver = "EmbedPageRegex",
                        playbackMode = "HLS",
                        sourceType = "m3u8",
                        resolved = m3u8Match
                    )
                    return Result.success(hlsSource)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "resolveFromEmbedPageOrApi failed: ${e.message}")
        }
        return Result.failure(IOException("Failed to extract stream from $embedUrl"))
    }

    /**
     * Parses JSON responses supporting all possible API response formats:
     * - "m3u8": "..."
     * - "url": "..."
     * - "file": "..."
     * - "src": "..."
     * - "sources": [ { "file": "...", "type": "hls" }, ... ]
     * - "source": { "file": "..." }
     * - "links": [ ... ]
     * - "hlsProxyUrl": "..."
     * - "tracks": [ { "file": "...", "label": "English", "kind": "captions" } ]
     * - "subtitles": [ ... ]
     */
    fun parseStreamJsonResponse(jsonString: String, referer: String): PlaybackSource? {
        val trimmed = jsonString.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null

        try {
            val json = if (trimmed.startsWith("{")) JSONObject(trimmed) else JSONObject()
            var primaryUrl: String? = null
            var hlsProxyUrl: String? = null
            val subtitles = mutableListOf<SubtitleTrack>()
            val audioTracks = mutableListOf<AudioTrack>()

            // Read direct fields
            val candidateKeys = listOf("m3u8", "url", "file", "src", "streamUrl", "playbackUrl")
            for (key in candidateKeys) {
                if (json.has(key)) {
                    val candidate = json.optString(key)
                    if (candidate.isNotBlank() && candidate != "null") {
                        primaryUrl = candidate
                        break
                    }
                }
            }

            // Read hlsProxyUrl if available
            if (json.has("hlsProxyUrl")) {
                val proxy = json.optString("hlsProxyUrl")
                if (proxy.isNotBlank() && proxy != "null") {
                    hlsProxyUrl = proxy
                }
            }

            // Read sources array
            if (primaryUrl.isNullOrBlank() && json.has("sources")) {
                val sourcesObj = json.opt("sources")
                if (sourcesObj is JSONArray && sourcesObj.length() > 0) {
                    for (i in 0 until sourcesObj.length()) {
                        val item = sourcesObj.opt(i)
                        if (item is JSONObject) {
                            val url = item.optString("file").ifBlank { item.optString("url") }.ifBlank { item.optString("src") }
                            if (url.isNotBlank() && url != "null") {
                                primaryUrl = url
                                break
                            }
                        } else if (item is String && item.isNotBlank()) {
                            primaryUrl = item
                            break
                        }
                    }
                } else if (sourcesObj is JSONObject) {
                    primaryUrl = sourcesObj.optString("file").ifBlank { sourcesObj.optString("url") }.ifBlank { sourcesObj.optString("src") }
                }
            }

            // Read source object
            if (primaryUrl.isNullOrBlank() && json.has("source")) {
                val srcObj = json.optJSONObject("source")
                if (srcObj != null) {
                    primaryUrl = srcObj.optString("file").ifBlank { srcObj.optString("url") }.ifBlank { srcObj.optString("src") }
                }
            }

            // Read links array
            if (primaryUrl.isNullOrBlank() && json.has("links")) {
                val linksArr = json.optJSONArray("links")
                if (linksArr != null && linksArr.length() > 0) {
                    for (i in 0 until linksArr.length()) {
                        val item = linksArr.opt(i)
                        if (item is JSONObject) {
                            val linkUrl = item.optString("link").ifBlank { item.optString("url") }.ifBlank { item.optString("file") }
                            if (linkUrl.isNotBlank() && linkUrl != "null") {
                                primaryUrl = linkUrl
                                break
                            }
                        } else if (item is String && item.isNotBlank()) {
                            primaryUrl = item
                            break
                        }
                    }
                }
            }

            // Parse subtitles / tracks only if actually returned
            val tracksArray = json.optJSONArray("tracks") ?: json.optJSONArray("subtitles")
            if (tracksArray != null) {
                for (i in 0 until tracksArray.length()) {
                    val trackObj = tracksArray.optJSONObject(i) ?: continue
                    val trackUrl = trackObj.optString("file").ifBlank { trackObj.optString("url") }
                    val label = trackObj.optString("label").ifBlank { trackObj.optString("lang") }.ifBlank { "Track ${i + 1}" }
                    val kind = trackObj.optString("kind").ifBlank { "captions" }
                    val isDefault = trackObj.optBoolean("default", false)

                    if (trackUrl.isNotBlank() && trackUrl != "null") {
                        if (kind.contains("audio", ignoreCase = true)) {
                            audioTracks.add(
                                AudioTrack(
                                    id = "audio_$i",
                                    label = label,
                                    language = trackObj.optString("lang").ifBlank { label },
                                    isDefault = isDefault
                                )
                            )
                        } else {
                            subtitles.add(
                                SubtitleTrack(
                                    id = "sub_$i",
                                    label = label,
                                    language = trackObj.optString("lang").ifBlank { label },
                                    url = trackUrl,
                                    format = if (trackUrl.contains(".ass", ignoreCase = true)) "ASS" else "VTT",
                                    isDefault = isDefault
                                )
                            )
                        }
                    }
                }
            }

            if (!primaryUrl.isNullOrBlank()) {
                val headers = mapOf(
                    "Referer" to referer,
                    "User-Agent" to DEFAULT_USER_AGENT
                )
                return if (primaryUrl.contains(".m3u8", ignoreCase = true) || primaryUrl.contains("master", ignoreCase = true) || primaryUrl.contains("playlist", ignoreCase = true)) {
                    PlaybackSource.Hls(
                        url = primaryUrl,
                        headers = headers,
                        subtitles = subtitles,
                        audioTracks = audioTracks,
                        hlsProxyUrl = hlsProxyUrl
                    )
                } else if (primaryUrl.contains(".mp4", ignoreCase = true)) {
                    PlaybackSource.Mp4(
                        url = primaryUrl,
                        headers = headers,
                        subtitles = subtitles,
                        audioTracks = audioTracks
                    )
                } else {
                    PlaybackSource.Hls(
                        url = primaryUrl,
                        headers = headers,
                        subtitles = subtitles,
                        audioTracks = audioTracks,
                        hlsProxyUrl = hlsProxyUrl
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "parseStreamJsonResponse failed: ${e.message}")
        }
        return null
    }

    /**
     * Converts a PlaybackSource into a list of VideoSource models.
     */
    fun convertToVideoSources(
        serverName: String,
        playbackSource: PlaybackSource,
        isDub: Boolean
    ): List<VideoSource> {
        val vs = toVideoSource(
            sourceId = "${serverName.lowercase().replace(" ", "_")}_${if (isDub) "dub" else "sub"}",
            serverName = serverName,
            playbackSource = playbackSource,
            isDub = isDub
        )
        return listOf(vs)
    }

    /**
     * Resolves a generic embed URL into a PlaybackSource.
     */
    suspend fun resolveFromEmbedUrl(
        embedUrl: String,
        headers: Map<String, String> = emptyMap(),
        client: OkHttpClient = defaultClient
    ): PlaybackSource? {
        val server = AnikotoServer(
            id = "generic",
            name = "Stream Server",
            embedUrl = embedUrl,
            type = "sub"
        )
        val res = resolveServerStream(
            animeTitle = "Anime",
            episodeNumber = 1,
            server = server,
            language = LanguagePreference.AUTO,
            client = client
        )
        return res.getOrNull()
    }

    suspend fun resolveVidstreamEmbed(
        embedUrl: String,
        client: OkHttpClient = defaultClient,
        headers: Map<String, String> = emptyMap()
    ): PlaybackSource? {
        val server = AnikotoServer(id = "vidstream", name = "Vidstream", embedUrl = embedUrl, type = "sub")
        return resolveVidstreamStream("Anime", 1, server, LanguagePreference.SUB, client).getOrNull()
    }

    suspend fun resolveVidcloudEmbed(
        embedUrl: String,
        client: OkHttpClient = defaultClient,
        headers: Map<String, String> = emptyMap()
    ): PlaybackSource? {
        val server = AnikotoServer(id = "vidcloud", name = "VidCloud", embedUrl = embedUrl, type = "sub")
        return resolveVidstreamStream("Anime", 1, server, LanguagePreference.SUB, client).getOrNull()
    }

    suspend fun resolveKiwiEmbed(
        embedUrl: String,
        client: OkHttpClient = defaultClient,
        headers: Map<String, String> = emptyMap()
    ): PlaybackSource? {
        val server = AnikotoServer(id = "kiwi", name = "Kiwi", embedUrl = embedUrl, type = "sub")
        return resolveGenericHosterStream("Anime", 1, server, LanguagePreference.SUB, client).getOrNull()
    }

    suspend fun resolveMegaPlayEmbed(
        embedUrl: String,
        client: OkHttpClient = defaultClient,
        headers: Map<String, String> = emptyMap()
    ): PlaybackSource? {
        val server = AnikotoServer(id = "megaplay", name = "MegaPlay", embedUrl = embedUrl, type = "sub")
        return resolveMegaPlayStream("Anime", 1, server, LanguagePreference.SUB, client).getOrNull()
    }

    /**
     * Converts a PlaybackSource into Animey's VideoSource model.
     */
    fun toVideoSource(
        sourceId: String,
        serverName: String,
        playbackSource: PlaybackSource,
        isDub: Boolean
    ): VideoSource {
        return when (playbackSource) {
            is PlaybackSource.Hls -> VideoSource(
                id = sourceId,
                sourceName = serverName,
                serverName = serverName,
                quality = "Auto",
                isDub = isDub,
                streamUrl = playbackSource.url,
                headers = playbackSource.headers,
                subtitles = playbackSource.subtitles,
                audioTracks = playbackSource.audioTracks,
                hlsProxyUrl = playbackSource.hlsProxyUrl
            )
            is PlaybackSource.Mp4 -> VideoSource(
                id = sourceId,
                sourceName = serverName,
                serverName = serverName,
                quality = "1080p",
                isDub = isDub,
                streamUrl = playbackSource.url,
                headers = playbackSource.headers,
                subtitles = playbackSource.subtitles,
                audioTracks = playbackSource.audioTracks
            )
            is PlaybackSource.Embed -> VideoSource(
                id = sourceId,
                sourceName = serverName,
                serverName = "$serverName (Embed)",
                quality = "Embed",
                isDub = isDub,
                streamUrl = playbackSource.url,
                embedUrl = playbackSource.url
            )
        }
    }

    private fun isDirectStreamUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".m3u8") || lower.contains(".m3u8?") ||
                lower.endsWith(".mp4") || lower.contains(".mp4?")
    }

    private fun extractM3u8FromHtml(html: String): String? {
        val patterns = listOf(
            Pattern.compile("""["'](https?://[^"']+\.m3u8[^"']*)["']"""),
            Pattern.compile("""file\s*:\s*["'](https?://[^"']+)["']"""),
            Pattern.compile("""source\s*:\s*["'](https?://[^"']+)["']""")
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val candidate = matcher.group(1) ?: continue
                if (candidate.contains(".m3u8", ignoreCase = true) || candidate.contains("playlist", ignoreCase = true)) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun extractIdFromUrl(url: String): String {
        return try {
            val clean = url.substringBefore("?").substringBefore("#")
            val tokens = clean.split("/").filter { it.isNotBlank() }
            tokens.lastOrNull { it != "sub" && it != "dub" && it != "embed" && it != "stream" } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun getDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: "megaplay.buzz"
        } catch (_: Exception) {
            "megaplay.buzz"
        }
    }

    private fun createPlaybackHeaders(url: String): Map<String, String> {
        val host = getDomain(url)
        return mapOf(
            "Referer" to "https://$host/",
            "Origin" to "https://$host",
            "User-Agent" to DEFAULT_USER_AGENT
        )
    }

    private fun logResolution(
        anime: String,
        episode: Int,
        language: String,
        server: String,
        embedUrl: String,
        resolver: String,
        playbackMode: String,
        sourceType: String,
        resolved: String
    ) {
        Log.i(TAG, "[ANIKOTO] anime=$anime")
        Log.i(TAG, "[ANIKOTO] episode=$episode")
        Log.i(TAG, "[ANIKOTO] language=$language")
        Log.i(TAG, "[ANIKOTO] server=$server")
        Log.i(TAG, "[ANIKOTO] embedUrl=$embedUrl")
        Log.i(TAG, "[ANIKOTO] resolver=$resolver")
        Log.i(TAG, "[ANIKOTO] playbackMode=$playbackMode")
        Log.i(TAG, "[ANIKOTO] sourceType=$sourceType")
        Log.i(TAG, "[ANIKOTO] resolved=$resolved")
    }
}
