package com.example.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.core.model.AudioTrack
import com.example.core.model.SegmentType
import com.example.core.model.SkipSegment
import com.example.core.model.SubtitleTrack
import com.example.core.model.VideoSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Concrete implementation of [PlayerController] using AndroidX Media3 / ExoPlayer.
 * Supports HLS, DASH, HTTP MP4 streams, custom headers, subtitles, audio track switching,
 * chapter skip markers, and error recovery.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class Media3PlayerManager(
    private val context: Context,
    private val onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit,
    private val isAutoSkipIntroEnabled: () -> Boolean = { true },
    private val isAutoSkipOutroEnabled: () -> Boolean = { true },
    private val onErrorCallback: ((error: androidx.media3.common.PlaybackException, isHttp403: Boolean) -> Unit)? = null
) : PlayerController {

    private fun createDataSourceFactory(customHeaders: Map<String, String> = emptyMap()): DefaultDataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 JustAnime/2.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        if (customHeaders.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(customHeaders)
        }

        return DefaultDataSource.Factory(context, httpFactory)
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(createDataSourceFactory())
        )
        .setSeekBackIncrementMs(10000)
        .setSeekForwardIncrementMs(10000)
        .build()

    override val playerInstance: Player
        get() = exoPlayer

    private val _uiState = MutableStateFlow(PlayerUiState())
    override val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var lastAutoSkippedSegment: SkipSegment? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            _uiState.update {
                it.copy(
                    isBuffering = isBuffering,
                    durationMs = duration,
                    error = null
                )
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val rootCause = error.cause
            val isHttp403 = (rootCause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException && rootCause.responseCode == 403) ||
                    error.message?.contains("403") == true

            val isHttpError = rootCause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException

            val userMessage = when {
                isHttp403 -> "Server stream access denied (HTTP 403). Switch server or tap retry."
                isHttpError -> "Server error code ${(rootCause as androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException).responseCode}. Tap to retry or change server."
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network connection issue. Please check your internet connection."
                else -> error.localizedMessage ?: "Playback error encountered. Tap to retry."
            }

            _uiState.update {
                it.copy(
                    isBuffering = false,
                    error = userMessage
                )
            }

            onErrorCallback?.invoke(error, isHttp403)
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        startProgressTracker()
    }

    override fun retry() {
        val currentSource = _uiState.value.selectedSource ?: return
        val pos = if (exoPlayer.currentPosition > 0) exoPlayer.currentPosition else _uiState.value.currentPositionMs
        prepareSource(currentSource, startPositionMs = pos, autoPlay = true)
    }

    override fun prepareSource(
        source: VideoSource,
        startPositionMs: Long,
        autoPlay: Boolean
    ) {
        lastAutoSkippedSegment = null
        try {
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(source.streamUrl)

            // Detect format (HLS, DASH, MP4)
            val streamUrlLower = source.streamUrl.lowercase()
            when {
                streamUrlLower.contains(".m3u8") || streamUrlLower.contains("m3u8") -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                }
                streamUrlLower.contains(".mpd") || streamUrlLower.contains("mpd") -> {
                    mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                }
                streamUrlLower.contains(".mp4") -> {
                    mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
                }
            }

            // Subtitles configuration
            if (source.subtitles.isNotEmpty()) {
                val subtitleConfigs = source.subtitles
                    .filter { it.url.isNotBlank() }
                    .map { sub ->
                        val mime = when (sub.format.uppercase()) {
                            "VTT" -> MimeTypes.TEXT_VTT
                            "SSA", "ASS" -> MimeTypes.TEXT_SSA
                            else -> MimeTypes.APPLICATION_SUBRIP
                        }
                        MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                            .setMimeType(mime)
                            .setLanguage(sub.language)
                            .setLabel(sub.label)
                            .setSelectionFlags(if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
                            .build()
                    }
                mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
            }

            val mediaItem = mediaItemBuilder.build()
            val customDataSourceFactory = createDataSourceFactory(source.headers)
            val customMediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(customDataSourceFactory)
            val mediaSource = customMediaSourceFactory.createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()

            if (startPositionMs > 0) {
                exoPlayer.seekTo(startPositionMs)
            }

            exoPlayer.playWhenReady = autoPlay

            _uiState.update {
                it.copy(
                    selectedSource = source,
                    selectedSubtitle = source.subtitles.find { s -> s.isDefault } ?: source.subtitles.firstOrNull(),
                    selectedAudioTrack = source.audioTracks.find { a -> a.isDefault } ?: source.audioTracks.firstOrNull(),
                    currentPositionMs = startPositionMs,
                    error = null
                )
            }
        } catch (t: Throwable) {
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    error = "Failed to load video stream: ${t.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L)))
    }

    override fun seekForward(offsetMs: Long) {
        val target = (exoPlayer.currentPosition + offsetMs).coerceAtMost(exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
    }

    override fun seekBackward(offsetMs: Long) {
        val target = (exoPlayer.currentPosition - offsetMs).coerceAtLeast(0L)
        exoPlayer.seekTo(target)
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    override fun setResizeMode(mode: ResizeMode) {
        _uiState.update { it.copy(resizeMode = mode) }
    }

    override fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked) }
    }

    override fun setControlsVisible(visible: Boolean) {
        if (!_uiState.value.isLocked) {
            _uiState.update { it.copy(isControlsVisible = visible) }
        }
    }

    override fun selectSubtitle(track: SubtitleTrack?) {
        _uiState.update { it.copy(selectedSubtitle = track) }
        if (track == null) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(track.language)
                .build()
        }
    }

    override fun selectAudioTrack(track: AudioTrack?) {
        _uiState.update { it.copy(selectedAudioTrack = track) }
        if (track != null) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setPreferredAudioLanguage(track.language)
                .build()
        }
    }

    override fun selectQuality(quality: String) {
        val available = _uiState.value.availableSources
        val matched = available.find { it.quality.equals(quality, ignoreCase = true) }
        if (matched != null && matched != _uiState.value.selectedSource) {
            val currentPos = exoPlayer.currentPosition
            prepareSource(matched, startPositionMs = currentPos, autoPlay = true)
        }
    }

    override fun skipActiveSegment() {
        val active = _uiState.value.activeSkipSegment ?: return
        val targetMs = active.endSeconds * 1000L
        seekTo(targetMs)
        _uiState.update { it.copy(activeSkipSegment = null) }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.playbackState != Player.STATE_IDLE) {
                    val currentPos = exoPlayer.currentPosition
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else _uiState.value.durationMs
                    val currentSec = currentPos / 1000L

                    val skipSegments = _uiState.value.selectedSource?.skipSegments ?: emptyList()
                    val activeSegment = skipSegments.find { seg ->
                        currentSec >= seg.startSeconds && currentSec < seg.endSeconds
                    }

                    var autoSkipNotice: String? = null
                    if (activeSegment != null && activeSegment != lastAutoSkippedSegment) {
                        val shouldAutoSkip = when (activeSegment.type) {
                            SegmentType.INTRO -> isAutoSkipIntroEnabled()
                            SegmentType.OUTRO -> isAutoSkipOutroEnabled()
                            SegmentType.RECAP -> true
                        }

                        if (shouldAutoSkip && currentSec >= activeSegment.startSeconds && currentSec < activeSegment.startSeconds + 3) {
                            lastAutoSkippedSegment = activeSegment
                            seekTo(activeSegment.endSeconds * 1000L)
                            autoSkipNotice = "Auto-skipped ${activeSegment.type.name.lowercase().replaceFirstChar { it.uppercase() }}"
                        }
                    }

                    _uiState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = duration,
                            activeSkipSegment = activeSegment,
                            autoSkipNotice = autoSkipNotice ?: it.autoSkipNotice
                        )
                    }

                    onProgressUpdate(currentPos, duration)
                }
                delay(400)
            }
        }
    }

    override fun release() {
        progressJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
