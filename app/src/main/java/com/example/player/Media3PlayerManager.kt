package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.core.model.AudioTrack
import com.example.core.model.SegmentType
import com.example.core.model.SkipSegment
import com.example.core.model.SubtitleTrack
import com.example.core.model.VideoSource
import com.example.player.cache.PlayerCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Concrete implementation of [PlayerController] using AndroidX Media3 / ExoPlayer.
 * Features progressive caching, asynchronous codec queues, multi-track audio & subtitle support,
 * custom aspect ratios, subtitle offset synchronization, and robust error recovery.
 */
@OptIn(UnstableApi::class)
class Media3PlayerManager(
    private val context: Context,
    private val onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit,
    private val isAutoSkipIntroEnabled: () -> Boolean = { true },
    private val isAutoSkipOutroEnabled: () -> Boolean = { true },
    private val isAutoSkipRecapEnabled: () -> Boolean = { true },
    private val onErrorCallback: ((error: androidx.media3.common.PlaybackException, isHttp403: Boolean, isHttp410: Boolean) -> Unit)? = null
) : PlayerController {

    private val appContext = context.applicationContext

    private fun createDataSourceFactory(customHeaders: Map<String, String> = emptyMap()): androidx.media3.datasource.DataSource.Factory {
        val userAgent = customHeaders["User-Agent"]
            ?: customHeaders["user-agent"]
            ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 JustAnime/2.0"

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(25000)
            .setReadTimeoutMs(30000)

        if (customHeaders.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(customHeaders)
        }

        val upstream = DefaultDataSource.Factory(appContext, httpFactory)
        return PlayerCacheManager.buildCacheDataSourceFactory(appContext, upstream)
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setRenderersFactory(
            DefaultRenderersFactory(appContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                .setEnableDecoderFallback(true)
                .setAllowedVideoJoiningTimeMs(5000)
                .setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val decoders = MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                    if (!_uiState.value.isHardwareDecoder) {
                        // Prioritize software decoders to prevent component resource interface failure
                        decoders.sortedBy { it.hardwareAccelerated }
                    } else {
                        decoders
                    }
                }
        )
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15_000, // minBufferMs
                    50_000, // maxBufferMs
                    2_000,  // bufferForPlaybackMs
                    4_000   // bufferForPlaybackAfterRebufferMs
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        )
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(appContext)
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
    private val processedSkipSegments = mutableSetOf<SkipSegment>()
    private val cancelledAutoSkipSegments = mutableSetOf<SkipSegment>()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                com.example.core.diagnostics.PlaybackDiagnosticsManager.updatePlaybackState("Playing: Rendering real stream frames")
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val isEnded = playbackState == Player.STATE_ENDED
            val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            val bufferedPos = exoPlayer.bufferedPosition

            val stateName = when (playbackState) {
                Player.STATE_IDLE -> "STATE_IDLE"
                Player.STATE_BUFFERING -> "STATE_BUFFERING"
                Player.STATE_READY -> "STATE_READY"
                Player.STATE_ENDED -> "STATE_ENDED"
                else -> "UNKNOWN"
            }
            com.example.core.diagnostics.PlaybackDiagnosticsManager.updateExoPlayerState(stateName, "Duration: ${duration}ms, Buffered: ${bufferedPos}ms")

            _uiState.update {
                it.copy(
                    isBuffering = isBuffering,
                    isEnded = isEnded,
                    durationMs = duration,
                    bufferedPositionMs = bufferedPos,
                    error = null
                )
            }
        }

        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            val videoQualities = mutableSetOf<String>()
            val embeddedSubs = mutableListOf<SubtitleTrack>()
            val embeddedAudios = mutableListOf<AudioTrack>()

            var trackIndex = 0
            for (group in tracks.groups) {
                when (group.type) {
                    C.TRACK_TYPE_VIDEO -> {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            if (format.height > 0) {
                                videoQualities.add("${format.height}p")
                            }
                        }
                    }
                    C.TRACK_TYPE_TEXT -> {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val lang = format.language.orEmpty()
                            val label = format.label ?: if (lang.isNotBlank()) lang else "Subtitle ${embeddedSubs.size + 1}"
                            embeddedSubs.add(
                                SubtitleTrack(
                                    id = "embedded_sub_${trackIndex++}",
                                    label = label,
                                    language = lang,
                                    url = "",
                                    isDefault = group.isTrackSelected(i)
                                )
                            )
                        }
                    }
                    C.TRACK_TYPE_AUDIO -> {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val lang = format.language.orEmpty()
                            val label = format.label ?: if (lang.isNotBlank()) lang else "Audio ${embeddedAudios.size + 1}"
                            embeddedAudios.add(
                                AudioTrack(
                                    id = "embedded_audio_${trackIndex++}",
                                    label = label,
                                    language = lang,
                                    isDefault = group.isTrackSelected(i)
                                )
                            )
                        }
                    }
                }
            }

            val sortedQualities = videoQualities
                .sortedByDescending { it.replace("p", "").toIntOrNull() ?: 0 }
                .toMutableList()

            if (sortedQualities.size > 1) {
                sortedQualities.add(0, "Auto")
            }

            _uiState.update { current ->
                val currentSubs = current.selectedSource?.subtitles ?: emptyList()
                val mergedSubs = (currentSubs + embeddedSubs.filter { emb -> currentSubs.none { it.label.equals(emb.label, ignoreCase = true) } }).distinctBy { it.id }

                val currentAudios = current.selectedSource?.audioTracks ?: emptyList()
                val mergedAudios = (currentAudios + embeddedAudios.filter { emb -> currentAudios.none { it.label.equals(emb.label, ignoreCase = true) } }).distinctBy { it.id }

                current.copy(
                    availableQualities = if (sortedQualities.isNotEmpty()) sortedQualities else current.availableQualities,
                    selectedSubtitle = current.selectedSubtitle ?: mergedSubs.find { it.isDefault } ?: mergedSubs.firstOrNull(),
                    selectedAudioTrack = current.selectedAudioTrack ?: mergedAudios.find { it.isDefault } ?: mergedAudios.firstOrNull()
                )
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val rootCause = error.cause
            val isHttpError = rootCause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
            val httpCode = if (isHttpError) (rootCause as androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException).responseCode else null
            val isHttp403 = httpCode == 403 || error.message?.contains("403") == true
            val isHttp410 = httpCode == 410 || error.message?.contains("410") == true || rootCause?.message?.contains("410") == true
            val isDecoderError = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED

            com.example.core.diagnostics.PlaybackDiagnosticsManager.updatePlaybackState(
                "Player Error: ${error.message ?: error.errorCodeName}",
                isError = true,
                error = rootCause ?: error,
                httpStatus = httpCode
            )

            if (isDecoderError && _uiState.value.isHardwareDecoder) {
                _uiState.update { it.copy(isHardwareDecoder = false) }
                val currentSource = _uiState.value.selectedSource
                if (currentSource != null) {
                    val pos = if (exoPlayer.currentPosition > 0) exoPlayer.currentPosition else _uiState.value.currentPositionMs
                    prepareSource(currentSource, startPositionMs = pos, autoPlay = true)
                    return
                }
            }

            val userMessage = when {
                isHttp410 -> "Playback source expired (HTTP 410). Switching to alternative server..."
                isHttp403 -> "Server stream access denied (HTTP 403). Switch server or tap retry."
                isHttpError -> "Server error code $httpCode. Tap to retry or change server."
                isDecoderError -> "Hardware decoder unavailable. Retrying with software fallback..."
                error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network connection issue. Please check your internet connection."
                else -> "${error.errorCodeName}: ${error.localizedMessage ?: "Playback error encountered. Tap to retry."}"
            }

            _uiState.update {
                it.copy(
                    isBuffering = false,
                    error = userMessage
                )
            }

            onErrorCallback?.invoke(error, isHttp403, isHttp410)
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
        processedSkipSegments.clear()
        cancelledAutoSkipSegments.clear()
        try {
            com.example.core.diagnostics.PlaybackDiagnosticsManager.updateHttpHeaders(source.headers)

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(source.streamUrl)

            // Detect format (HLS, DASH, MP4)
            val streamUrlLower = source.streamUrl.lowercase()
            val detectedMimeType = when {
                streamUrlLower.contains(".m3u8") || streamUrlLower.contains("m3u8") || streamUrlLower.contains("master") || streamUrlLower.contains("playlist") -> {
                    com.example.core.diagnostics.PlaybackDiagnosticsManager.updateStreamType("HLS (.m3u8)", source.streamUrl)
                    MimeTypes.APPLICATION_M3U8
                }
                streamUrlLower.contains(".mpd") || streamUrlLower.contains("mpd") -> {
                    com.example.core.diagnostics.PlaybackDiagnosticsManager.updateStreamType("DASH (.mpd)", source.streamUrl)
                    MimeTypes.APPLICATION_MPD
                }
                streamUrlLower.contains(".mp4") -> {
                    com.example.core.diagnostics.PlaybackDiagnosticsManager.updateStreamType("Progressive MP4", source.streamUrl)
                    MimeTypes.VIDEO_MP4
                }
                streamUrlLower.contains(".webm") || streamUrlLower.contains(".mkv") -> {
                    com.example.core.diagnostics.PlaybackDiagnosticsManager.updateStreamType("Matroska/WebM", source.streamUrl)
                    MimeTypes.VIDEO_WEBM
                }
                else -> {
                    com.example.core.diagnostics.PlaybackDiagnosticsManager.updateStreamType("Generic Media Stream", source.streamUrl)
                    null
                }
            }

            if (detectedMimeType != null) {
                mediaItemBuilder.setMimeType(detectedMimeType)
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
                com.example.core.diagnostics.PlaybackDiagnosticsManager.updateSubtitles(source.subtitles.map { "${it.label} (${it.language}) -> ${it.url}" })
            } else {
                com.example.core.diagnostics.PlaybackDiagnosticsManager.updateSubtitles(emptyList())
            }

            if (source.audioTracks.isNotEmpty()) {
                com.example.core.diagnostics.PlaybackDiagnosticsManager.updateAudioTracks(source.audioTracks.map { "${it.label} (${it.language})" })
            } else {
                com.example.core.diagnostics.PlaybackDiagnosticsManager.updateAudioTracks(emptyList())
            }

            val mediaItem = mediaItemBuilder.build()
            val customDataSourceFactory = createDataSourceFactory(source.headers)

            val mediaSource: androidx.media3.exoplayer.source.MediaSource = when {
                source.format == com.example.domain.model.StreamFormat.HLS || detectedMimeType == MimeTypes.APPLICATION_M3U8 -> {
                    androidx.media3.exoplayer.hls.HlsMediaSource.Factory(customDataSourceFactory)
                        .setAllowChunklessPreparation(true)
                        .createMediaSource(mediaItem)
                }
                source.format == com.example.domain.model.StreamFormat.PROGRESSIVE_MP4 || detectedMimeType == MimeTypes.VIDEO_MP4 -> {
                    androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(customDataSourceFactory)
                        .createMediaSource(mediaItem)
                }
                detectedMimeType == MimeTypes.APPLICATION_MPD -> {
                    androidx.media3.exoplayer.dash.DashMediaSource.Factory(customDataSourceFactory)
                        .createMediaSource(mediaItem)
                }
                else -> {
                    androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(customDataSourceFactory)
                        .createMediaSource(mediaItem)
                }
            }

            com.example.core.diagnostics.PlaybackDiagnosticsManager.updateMediaItem(
                uri = source.streamUrl,
                mimeType = detectedMimeType,
                mediaSourceType = mediaSource.javaClass.simpleName
            )

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()

            com.example.core.diagnostics.PlaybackDiagnosticsManager.updateExoPlayerState("Preparing Stream in ExoPlayer", "Seeking to ${startPositionMs}ms")

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
                    currentSubtitleCues = emptyList(),
                    error = null
                )
            }
        } catch (t: Throwable) {
            com.example.core.diagnostics.PlaybackDiagnosticsManager.updatePlaybackState(
                "Failed to load stream: ${t.message}",
                isError = true,
                error = t
            )
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

    override fun cycleResizeMode() {
        val nextMode = when (_uiState.value.resizeMode) {
            ResizeMode.ORIGINAL -> ResizeMode.FIT
            ResizeMode.FIT -> ResizeMode.FILL
            ResizeMode.FILL -> ResizeMode.ZOOM
            ResizeMode.ZOOM -> ResizeMode.ORIGINAL
            ResizeMode.FIXED_16_9 -> ResizeMode.ORIGINAL
        }
        _uiState.update { it.copy(resizeMode = nextMode) }
    }

    override fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked, isControlsVisible = !it.isLocked) }
    }

    override fun seekTo(positionMs: Long) {
        val dur = exoPlayer.duration.coerceAtLeast(0L)
        exoPlayer.seekTo(positionMs.coerceIn(0L, dur))
    }

    override fun seekForward(offsetMs: Long) {
        val dur = exoPlayer.duration.coerceAtLeast(0L)
        val target = (exoPlayer.currentPosition + offsetMs).coerceAtMost(dur)
        exoPlayer.seekTo(target)
    }

    override fun seekBackward(offsetMs: Long) {
        val target = (exoPlayer.currentPosition - offsetMs).coerceAtLeast(0L)
        exoPlayer.seekTo(target)
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (_uiState.value.audioPitchCorrection) {
            exoPlayer.playbackParameters = PlaybackParameters(speed, 1.0f)
        } else {
            exoPlayer.setPlaybackSpeed(speed)
        }
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    override fun setResizeMode(mode: ResizeMode) {
        _uiState.update { it.copy(resizeMode = mode) }
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

    override fun adjustSubtitleDelay(deltaMs: Long) {
        _uiState.update { it.copy(subtitleDelayMs = it.subtitleDelayMs + deltaMs) }
    }

    override fun resetSubtitleDelay() {
        _uiState.update { it.copy(subtitleDelayMs = 0L) }
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

    override fun setAudioChannels(channels: String) {
        _uiState.update { it.copy(audioChannels = channels) }
    }

    override fun toggleHardwareDecoder() {
        val nextHw = !_uiState.value.isHardwareDecoder
        _uiState.update { it.copy(isHardwareDecoder = nextHw) }
        val currentSource = _uiState.value.selectedSource
        if (currentSource != null) {
            val pos = if (exoPlayer.currentPosition > 0) exoPlayer.currentPosition else _uiState.value.currentPositionMs
            prepareSource(currentSource, startPositionMs = pos, autoPlay = exoPlayer.playWhenReady)
        }
    }

    override fun selectQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }

        if (quality.equals("Auto", ignoreCase = true)) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearVideoSizeConstraints()
                .build()
            return
        }

        val height = quality.replace("p", "").trim().toIntOrNull()
        if (height != null && height > 0) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(height * 16 / 9, height)
                .setMinVideoSize((height * 16 / 9) - 20, height - 20)
                .build()
            return
        }

        val available = _uiState.value.availableSources
        val matched = available.find { it.quality.equals(quality, ignoreCase = true) }
        if (matched != null && matched != _uiState.value.selectedSource) {
            val currentPos = exoPlayer.currentPosition
            prepareSource(matched, startPositionMs = currentPos, autoPlay = true)
        }
    }

    override fun skipActiveSegment() {
        val active = _uiState.value.activeSkipSegment ?: return
        processedSkipSegments.add(active)
        val targetMs = active.endSeconds * 1000L
        seekTo(targetMs)
        _uiState.update { it.copy(activeSkipSegment = null) }
    }

    override fun megaSkip() {
        val durationMs = _uiState.value.megaSkipDurationSeconds * 1000L
        seekForward(durationMs)
    }

    override fun cancelAutoSkip() {
        val active = _uiState.value.activeSkipSegment ?: return
        cancelledAutoSkipSegments.add(active)
        _uiState.update { it.copy(isAutoSkipCountingDown = false, autoSkipSecondsRemaining = 0) }
    }

    override fun setExperimentalSettings(
        enabled: Boolean?,
        frameInterpolation: Boolean?,
        pitchCorrection: Boolean?,
        cacheMinutes: Int?,
        demuxerBufferMb: Int?
    ) {
        _uiState.update { current ->
            current.copy(
                experimentalSettingsEnabled = enabled ?: current.experimentalSettingsEnabled,
                frameInterpolation = frameInterpolation ?: current.frameInterpolation,
                audioPitchCorrection = pitchCorrection ?: current.audioPitchCorrection,
                cacheMinutes = cacheMinutes ?: current.cacheMinutes,
                demuxerMaxBufferMb = demuxerBufferMb ?: current.demuxerMaxBufferMb
            )
        }
    }

    override fun setShadersEnabled(enabled: Boolean) {
        _uiState.update { it.copy(shadersEnabled = enabled) }
    }

    override fun setShaderProfile(profile: String) {
        _uiState.update { it.copy(shaderProfile = profile) }
    }

    override fun setDoubleTapSeekSeconds(seconds: Int) {
        _uiState.update { it.copy(doubleTapSeekSeconds = seconds) }
    }

    override fun setMegaSkipDurationSeconds(seconds: Int) {
        _uiState.update { it.copy(megaSkipDurationSeconds = seconds) }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.playbackState != Player.STATE_IDLE) {
                    val currentPos = exoPlayer.currentPosition
                    val bufferedPos = exoPlayer.bufferedPosition
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else _uiState.value.durationMs
                    val currentSec = currentPos / 1000L

                    val skipSegments = _uiState.value.selectedSource?.skipSegments ?: emptyList()
                    val activeSegment = skipSegments.find { seg ->
                        currentSec >= seg.startSeconds && currentSec < seg.endSeconds && !processedSkipSegments.contains(seg)
                    }

                    var autoSkipNotice: String? = null
                    var isCountingDown = false
                    var remainingSeconds = 0

                    if (activeSegment != null) {
                        val shouldAutoSkip = when (activeSegment.type) {
                            SegmentType.INTRO -> isAutoSkipIntroEnabled()
                            SegmentType.OUTRO -> isAutoSkipOutroEnabled()
                            SegmentType.RECAP -> isAutoSkipRecapEnabled()
                        }

                        if (shouldAutoSkip && !cancelledAutoSkipSegments.contains(activeSegment)) {
                            val secondsIntoSegment = currentSec - activeSegment.startSeconds
                            if (secondsIntoSegment >= 0) {
                                val delaySeconds = 4L
                                val timeRemaining = (delaySeconds - secondsIntoSegment).toInt()

                                if (timeRemaining <= 0) {
                                    processedSkipSegments.add(activeSegment)
                                    seekTo(activeSegment.endSeconds * 1000L)
                                    autoSkipNotice = "Auto-skipped ${activeSegment.type.name.lowercase().replaceFirstChar { it.uppercase() }}"
                                } else {
                                    isCountingDown = true
                                    remainingSeconds = timeRemaining
                                }
                            }
                        }
                    }

                    _uiState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            bufferedPositionMs = bufferedPos,
                            durationMs = duration,
                            activeSkipSegment = activeSegment,
                            isAutoSkipCountingDown = isCountingDown,
                            autoSkipSecondsRemaining = remainingSeconds,
                            autoSkipNotice = autoSkipNotice ?: it.autoSkipNotice
                        )
                    }

                    onProgressUpdate(currentPos, duration)
                }
                delay(300)
            }
        }
    }

    override fun release() {
        progressJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.clearVideoSurface()
        exoPlayer.stop()
        exoPlayer.release()
        scope.cancel()
    }
}
