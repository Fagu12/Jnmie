package com.example.player

import com.example.core.model.AudioTrack
import com.example.core.model.Episode
import com.example.core.model.SkipSegment
import com.example.core.model.SubtitleTrack
import com.example.core.model.VideoSource

enum class ResizeMode(val label: String) {
    ORIGINAL("Original"),
    FIT("Fit"),
    FILL("Cover"),
    ZOOM("Zoom"),
    FIXED_16_9("16:9")
}

data class SubtitleCue(
    val timestampMs: Long,
    val text: String
)

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val isControlsVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isPipActive: Boolean = false,
    val activeSkipSegment: SkipSegment? = null,
    val isAutoSkipCountingDown: Boolean = false,
    val autoSkipSecondsRemaining: Int = 0,
    val autoSkipNotice: String? = null,
    val selectedSource: VideoSource? = null,
    val availableSources: List<VideoSource> = emptyList(),
    val availableQualities: List<String> = emptyList(),
    val selectedQuality: String = "Auto",
    val selectedSubtitle: SubtitleTrack? = null,
    val selectedAudioTrack: AudioTrack? = null,
    val error: String? = null,
    val isEnded: Boolean = false,
    // Subtitle Sync
    val subtitleDelayMs: Long = 0L,
    val currentSubtitleCues: List<SubtitleCue> = emptyList(),
    // Audio layout
    val audioChannels: String = "Stereo",
    // Decoder HW+ / SW
    val isHardwareDecoder: Boolean = true,
    // Skip duration
    val megaSkipDurationSeconds: Int = 85,
    val doubleTapSeekSeconds: Int = 10,
    // Experimental Settings
    val experimentalSettingsEnabled: Boolean = false,
    val videoSyncMode: String = "audio",
    val frameInterpolation: Boolean = false,
    val audioPitchCorrection: Boolean = true,
    val cacheMinutes: Int = 60,
    val demuxerReadaheadSeconds: Int = 120,
    val demuxerMaxBufferMb: Int = 512,
    // Shaders / 4K
    val shadersEnabled: Boolean = false,
    val shaderProfile: String = "Anime4K: Mode A (Fast)",
    // Gestures
    val gestureBrightnessVolumeEnabled: Boolean = true,
    val holdToSpeedUpEnabled: Boolean = true,
    val swipeToSeekEnabled: Boolean = true
)
