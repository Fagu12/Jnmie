package com.example.player

import com.example.core.model.AudioTrack
import com.example.core.model.Episode
import com.example.core.model.SkipSegment
import com.example.core.model.SubtitleTrack
import com.example.core.model.VideoSource

enum class ResizeMode {
    FIT,
    FILL,
    ZOOM,
    FIXED_16_9
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val isControlsVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isPipActive: Boolean = false,
    val activeSkipSegment: SkipSegment? = null,
    val autoSkipNotice: String? = null,
    val selectedSource: VideoSource? = null,
    val availableSources: List<VideoSource> = emptyList(),
    val selectedSubtitle: SubtitleTrack? = null,
    val selectedAudioTrack: AudioTrack? = null,
    val error: String? = null
)
