package com.example.core.diagnostics

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class PipelineStageItem(
    val stageNumber: Int,
    val stageName: String,
    val status: StageStatus,
    val summary: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}

enum class StageStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    WARNING,
    ERROR
}

@Immutable
data class StreamPipelineDiagnostics(
    val extensionId: String = "",
    val extensionLoadStatus: String = "Pending",
    val animeId: String = "",
    val episodeId: String = "",
    val resolverMethod: String = "",
    val resolverResponse: String = "",
    val returnedStreamUrls: List<String> = emptyList(),
    val detectedStreamType: String = "",
    val httpHeaders: Map<String, String> = emptyMap(),
    val refererHeader: String = "",
    val originHeader: String = "",
    val userAgentHeader: String = "",
    val cookiesHeader: String = "",
    val subtitleTracks: List<String> = emptyList(),
    val audioTracks: List<String> = emptyList(),
    val mediaItemUri: String = "",
    val exoPlayerPrepState: String = "IDLE",
    val actualPlaybackState: String = "IDLE",
    val exceptionClass: String? = null,
    val exceptionMessage: String? = null,
    val exceptionStackTrace: String? = null,
    val httpStatusCode: Int? = null,
    val stages: List<PipelineStageItem> = defaultStages()
) {
    companion object {
        fun defaultStages(): List<PipelineStageItem> = listOf(
            PipelineStageItem(1, "Extension Selection", StageStatus.PENDING, "Waiting for user-installed extension selection"),
            PipelineStageItem(2, "Extension Runtime Load", StageStatus.PENDING, "Isolated classloader & Injekt bridge"),
            PipelineStageItem(3, "Anime Catalog Match", StageStatus.PENDING, "Matching anime ID with extension"),
            PipelineStageItem(4, "Episode Listing", StageStatus.PENDING, "Resolving episode URL & metadata"),
            PipelineStageItem(5, "Stream Resolver Call", StageStatus.PENDING, "Executing videoListRequest / hosters"),
            PipelineStageItem(6, "Resolver Response", StageStatus.PENDING, "Extracting video models"),
            PipelineStageItem(7, "Stream URLs Discovered", StageStatus.PENDING, "Collecting playable server streams"),
            PipelineStageItem(8, "Stream Type Detection", StageStatus.PENDING, "HLS (.m3u8) / DASH (.mpd) / Progressive"),
            PipelineStageItem(9, "HTTP Headers Assembly", StageStatus.PENDING, "Compiling request headers"),
            PipelineStageItem(10, "Auth & Referer Injection", StageStatus.PENDING, "Injecting Referer, Origin, User-Agent"),
            PipelineStageItem(11, "Subtitle Tracks Binding", StageStatus.PENDING, "Binding VTT / ASS / SRT tracks"),
            PipelineStageItem(12, "Audio Tracks Binding", StageStatus.PENDING, "Parsing multi-audio language tracks"),
            PipelineStageItem(13, "MediaSource Factory", StageStatus.PENDING, "Constructing Media3 MediaSource"),
            PipelineStageItem(14, "ExoPlayer Preparation", StageStatus.PENDING, "Codec queues, buffer & load control"),
            PipelineStageItem(15, "Playback & Error State", StageStatus.PENDING, "Rendering video stream to surface")
        )
    }
}

object PlaybackDiagnosticsManager {
    private val _diagnostics = MutableStateFlow(StreamPipelineDiagnostics())
    val diagnostics: StateFlow<StreamPipelineDiagnostics> = _diagnostics.asStateFlow()

    fun reset() {
        _diagnostics.value = StreamPipelineDiagnostics()
    }

    fun updateStage(
        stageNumber: Int,
        status: StageStatus,
        summary: String,
        details: String = ""
    ) {
        _diagnostics.update { current ->
            val updatedStages = current.stages.map { stage ->
                if (stage.stageNumber == stageNumber) {
                    stage.copy(
                        status = status,
                        summary = summary,
                        details = details,
                        timestamp = System.currentTimeMillis()
                    )
                } else stage
            }
            current.copy(stages = updatedStages)
        }
    }

    fun updateExtensionSelection(extId: String, summary: String = "Selected extension: $extId") {
        _diagnostics.update { it.copy(extensionId = extId) }
        updateStage(1, StageStatus.SUCCESS, summary, "Package/ID: $extId")
    }

    fun updateExtensionLoad(
        success: Boolean,
        statusDesc: String,
        details: String = "",
        error: Throwable? = null
    ) {
        _diagnostics.update {
            it.copy(
                extensionLoadStatus = statusDesc,
                exceptionClass = error?.javaClass?.name ?: it.exceptionClass,
                exceptionMessage = error?.message ?: it.exceptionMessage,
                exceptionStackTrace = error?.stackTraceToString() ?: it.exceptionStackTrace
            )
        }
        updateStage(
            2,
            if (success) StageStatus.SUCCESS else StageStatus.ERROR,
            statusDesc,
            details + if (error != null) "\nError: ${error.javaClass.simpleName}: ${error.message}" else ""
        )
    }

    fun updateAnimeMatch(animeId: String, title: String, details: String = "") {
        _diagnostics.update { it.copy(animeId = animeId) }
        updateStage(3, StageStatus.SUCCESS, "Matched anime: $title (ID: $animeId)", details)
    }

    fun updateEpisodeMatch(episodeId: String, epNumber: Int, details: String = "") {
        _diagnostics.update { it.copy(episodeId = episodeId) }
        updateStage(4, StageStatus.SUCCESS, "Resolved Episode #$epNumber (ID: $episodeId)", details)
    }

    fun updateResolverCall(method: String, details: String = "") {
        _diagnostics.update { it.copy(resolverMethod = method) }
        updateStage(5, StageStatus.IN_PROGRESS, "Invoked resolver: $method", details)
    }

    fun updateResolverResponse(
        success: Boolean,
        summary: String,
        details: String = "",
        error: Throwable? = null
    ) {
        _diagnostics.update {
            it.copy(
                resolverResponse = summary,
                exceptionClass = error?.javaClass?.name ?: it.exceptionClass,
                exceptionMessage = error?.message ?: it.exceptionMessage,
                exceptionStackTrace = error?.stackTraceToString() ?: it.exceptionStackTrace
            )
        }
        updateStage(
            6,
            if (success) StageStatus.SUCCESS else StageStatus.ERROR,
            summary,
            details + if (error != null) "\nError: ${error.javaClass.simpleName}: ${error.message}" else ""
        )
    }

    fun updateStreamUrls(urls: List<String>, count: Int) {
        _diagnostics.update { it.copy(returnedStreamUrls = urls) }
        updateStage(
            7,
            if (urls.isNotEmpty()) StageStatus.SUCCESS else StageStatus.WARNING,
            "Discovered $count stream URL(s)",
            urls.joinToString("\n")
        )
    }

    fun updateStreamType(streamType: String, url: String) {
        _diagnostics.update { it.copy(detectedStreamType = streamType) }
        updateStage(8, StageStatus.SUCCESS, "Detected: $streamType", "Stream URL: $url")
    }

    fun updateHttpHeaders(headers: Map<String, String>) {
        val referer = headers["Referer"] ?: headers["referer"] ?: ""
        val origin = headers["Origin"] ?: headers["origin"] ?: ""
        val ua = headers["User-Agent"] ?: headers["user-agent"] ?: ""
        val cookies = headers["Cookie"] ?: headers["cookie"] ?: ""

        _diagnostics.update {
            it.copy(
                httpHeaders = headers,
                refererHeader = referer,
                originHeader = origin,
                userAgentHeader = ua,
                cookiesHeader = cookies
            )
        }
        updateStage(9, StageStatus.SUCCESS, "Headers configured (${headers.size} headers)", headers.entries.joinToString("\n") { "${it.key}: ${it.value}" })
        updateStage(10, StageStatus.SUCCESS, "Referer & Security headers verified", "Referer: $referer\nOrigin: $origin\nUser-Agent: $ua")
    }

    fun updateSubtitles(tracks: List<String>) {
        _diagnostics.update { it.copy(subtitleTracks = tracks) }
        updateStage(11, StageStatus.SUCCESS, "Subtitles bound (${tracks.size} tracks)", tracks.joinToString("\n"))
    }

    fun updateAudioTracks(tracks: List<String>) {
        _diagnostics.update { it.copy(audioTracks = tracks) }
        updateStage(12, StageStatus.SUCCESS, "Audio tracks bound (${tracks.size} tracks)", tracks.joinToString("\n"))
    }

    fun updateMediaItem(uri: String, mimeType: String?, mediaSourceType: String) {
        _diagnostics.update { it.copy(mediaItemUri = uri) }
        updateStage(13, StageStatus.SUCCESS, "MediaSource created: $mediaSourceType", "URI: $uri\nMimeType: $mimeType")
    }

    fun updateExoPlayerState(stateDesc: String, details: String = "") {
        _diagnostics.update { it.copy(exoPlayerPrepState = stateDesc) }
        updateStage(14, StageStatus.SUCCESS, "ExoPlayer: $stateDesc", details)
    }

    fun updatePlaybackState(
        stateDesc: String,
        isError: Boolean = false,
        error: Throwable? = null,
        httpStatus: Int? = null
    ) {
        _diagnostics.update {
            it.copy(
                actualPlaybackState = stateDesc,
                httpStatusCode = httpStatus ?: it.httpStatusCode,
                exceptionClass = error?.javaClass?.name ?: it.exceptionClass,
                exceptionMessage = error?.message ?: it.exceptionMessage,
                exceptionStackTrace = error?.stackTraceToString() ?: it.exceptionStackTrace
            )
        }
        updateStage(
            15,
            if (isError) StageStatus.ERROR else StageStatus.SUCCESS,
            stateDesc,
            if (error != null) "Exception: ${error.javaClass.name}: ${error.message}\n${error.stackTraceToString()}" else "Playback active"
        )
    }
}
