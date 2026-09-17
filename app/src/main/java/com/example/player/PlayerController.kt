package com.example.player

import androidx.media3.common.Player
import com.example.core.model.AudioTrack
import com.example.core.model.SubtitleTrack
import com.example.core.model.VideoSource
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean architectural abstraction for video playback.
 * Decouples the Jetpack Compose Player UI and ViewModel from the underlying
 * AndroidX Media3 / ExoPlayer implementation details.
 */
interface PlayerController {
    /**
     * Observable UI state representing player position, buffering, errors,
     * available sources, subtitles, audio tracks, and skip markers.
     */
    val uiState: StateFlow<PlayerUiState>

    /**
     * Underlying Media3 Player instance for surface binding in Compose AndroidView.
     */
    val playerInstance: Player?

    /**
     * Prepares and initializes playback for the given [VideoSource].
     *
     * @param source The media source containing stream URL, headers, and metadata.
     * @param startPositionMs Saved position in milliseconds to seek to upon load.
     * @param autoPlay Whether to immediately start playback when ready.
     */
    fun prepareSource(source: VideoSource, startPositionMs: Long = 0L, autoPlay: Boolean = true)

    /** Starts or resumes playback. */
    fun play()

    /** Pauses playback. */
    fun pause()

    /** Toggles between play and pause states. */
    fun togglePlayPause()

    /** Cycles through available resize modes. */
    fun cycleResizeMode()

    /** Toggles control panel lock state. */
    fun toggleLock()

    /** Seeks to a specific timestamp in milliseconds. */
    fun seekTo(positionMs: Long)

    /** Seeks forward by [offsetMs] milliseconds (default 10,000ms). */
    fun seekForward(offsetMs: Long = 10000L)

    /** Seeks backward by [offsetMs] milliseconds (default 10,000ms). */
    fun seekBackward(offsetMs: Long = 10000L)

    /** Sets playback speed multiplier (e.g., 0.5x, 1.0x, 1.25x, 1.5x, 2.0x). */
    fun setPlaybackSpeed(speed: Float)

    /** Configures video aspect ratio / resize mode. */
    fun setResizeMode(mode: ResizeMode)

    /** Controls visibility of on-screen controls overlay. */
    fun setControlsVisible(visible: Boolean)

    /** Selects active subtitle track, or null to disable subtitles. */
    fun selectSubtitle(track: SubtitleTrack?)

    /** Adjusts subtitle synchronization offset in milliseconds. */
    fun adjustSubtitleDelay(deltaMs: Long)

    /** Resets subtitle synchronization offset to 0. */
    fun resetSubtitleDelay()

    /** Selects active audio track (e.g. Japanese original vs English dub). */
    fun selectAudioTrack(track: AudioTrack?)

    /** Configures audio channel layout (e.g. Mono, Stereo, 5.1, 7.1). */
    fun setAudioChannels(channels: String)

    /** Toggles between Hardware (HW+) and Software (SW) decoder modes. */
    fun toggleHardwareDecoder()

    /** Selects desired stream quality resolution. */
    fun selectQuality(quality: String)

    /** Skips active intro, outro, or recap segment marker. */
    fun skipActiveSegment()

    /** Skips by the custom MegaSkip duration. */
    fun megaSkip()

    /** Cancels an ongoing auto-skip countdown. */
    fun cancelAutoSkip()

    /** Updates experimental playback settings. */
    fun setExperimentalSettings(
        enabled: Boolean? = null,
        frameInterpolation: Boolean? = null,
        pitchCorrection: Boolean? = null,
        cacheMinutes: Int? = null,
        demuxerBufferMb: Int? = null
    )

    /** Enables or disables custom shader enhancement. */
    fun setShadersEnabled(enabled: Boolean)

    /** Sets active shader profile. */
    fun setShaderProfile(profile: String)

    /** Sets double-tap seek increment in seconds. */
    fun setDoubleTapSeekSeconds(seconds: Int)

    /** Sets MegaSkip duration in seconds. */
    fun setMegaSkipDurationSeconds(seconds: Int)

    /** Retries playback from current position after a network or stream error. */
    fun retry()

    /** Releases player resources, listeners, and background tracking jobs. */
    fun release()
}
