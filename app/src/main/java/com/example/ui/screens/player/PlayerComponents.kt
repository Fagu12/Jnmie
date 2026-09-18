package com.example.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlayerUiState
import com.example.player.ResizeMode
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.SkipSegmentYellow

/**
 * Top bar overlay for Just Anime Player.
 * Matches reference video layout:
 * - Back button (circular dark translucent button)
 * - Episode title (large, bold)
 * - Subtitle row with anime title, Episode number badge, Quality badge
 * - Action buttons: HW+/SW decoder pill, Lock button, PiP button, Settings gear button
 */
@Composable
fun PlayerTopBar(
    state: PlayerScreenState,
    uiState: PlayerUiState,
    onBack: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleDecoder: () -> Unit,
    onPipClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDiagnosticsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Back Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                )
                .testTag("player_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Episode & Anime Information
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.episode?.title?.takeIf { it.isNotBlank() }
                    ?: (state.episode?.let { "Episode ${it.number}" } ?: "Playing Episode"),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = state.anime?.title ?: "",
                    color = Color(0xFFB0B4C8),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Episode badge pill
                state.episode?.let { ep ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AnimePrimary.copy(alpha = 0.2f))
                            .border(1.dp, AnimePrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EP ${ep.number}",
                            color = AnimePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Resolution badge pill
                val quality = uiState.selectedSource?.quality ?: "1080p"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = quality,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right Action Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // HW+ / SW Decoder badge pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (uiState.isHardwareDecoder) AnimePrimary.copy(alpha = 0.25f) else Color(0x33FFFFFF))
                    .border(
                        1.dp,
                        if (uiState.isHardwareDecoder) AnimePrimary.copy(alpha = 0.6f) else Color(0x44FFFFFF),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleDecoder
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("player_decoder_badge")
            ) {
                Text(
                    text = if (uiState.isHardwareDecoder) "HW+" else "SW",
                    color = if (uiState.isHardwareDecoder) AnimePrimary else Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Diagnostics Button
            if (onDiagnosticsClick != null) {
                CircularPlayerIconButton(
                    icon = androidx.compose.material.icons.Icons.Filled.Tune,
                    contentDescription = "Playback Diagnostics",
                    onClick = onDiagnosticsClick,
                    testTag = "player_diagnostics_button"
                )
            }

            // Lock Button
            CircularPlayerIconButton(
                icon = if (uiState.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = "Lock controls",
                onClick = onToggleLock,
                testTag = "player_lock_button"
            )

            // PiP Button
            CircularPlayerIconButton(
                icon = Icons.Filled.PictureInPictureAlt,
                contentDescription = "Picture in picture",
                onClick = onPipClick,
                testTag = "player_pip_button"
            )

            // Settings Button
            CircularPlayerIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = "Player settings",
                onClick = onSettingsClick,
                testTag = "player_settings_button"
            )
        }
    }
}

/**
 * Center media playback controls (Previous, Play/Pause, Next).
 */
@Composable
fun PlayerCenterControls(
    uiState: PlayerUiState,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous Episode Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (hasPrevious) Color(0x44000000) else Color(0x22000000))
                .border(1.dp, Color(0x22FFFFFF), CircleShape)
                .clickable(
                    enabled = hasPrevious,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPrevious
                )
                .testTag("player_prev_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous Episode",
                tint = if (hasPrevious) Color.White else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }

        // Center Play / Pause Button with lavender glowing badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AnimePrimary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayPause
                )
                .testTag("player_play_pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                tint = Color(0xFF0E0F17),
                modifier = Modifier.size(40.dp)
            )
        }

        // Next Episode Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (hasNext) Color(0x44000000) else Color(0x22000000))
                .border(1.dp, Color(0x22FFFFFF), CircleShape)
                .clickable(
                    enabled = hasNext,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNext
                )
                .testTag("player_next_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next Episode",
                tint = if (hasNext) Color.White else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Floating MegaSkip / Skip segment pill button placed above the timeline.
 */
@Composable
fun PlayerSkipPill(
    uiState: PlayerUiState,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSegment = uiState.activeSkipSegment
    val label = when {
        activeSegment != null -> {
            val segmentName = when (activeSegment.type) {
                com.example.domain.model.SegmentType.INTRO -> "Intro"
                com.example.domain.model.SegmentType.RECAP -> "Recap"
                com.example.domain.model.SegmentType.OUTRO -> "Outro"
            }
            if (uiState.isAutoSkipCountingDown && uiState.autoSkipSecondsRemaining > 0) {
                "Auto-skipping $segmentName (${uiState.autoSkipSecondsRemaining}s)"
            } else {
                "Skip $segmentName"
            }
        }
        else -> "+${uiState.megaSkipDurationSeconds}s"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xEE1A1B28))
            .border(1.dp, AnimePrimary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSkipClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("player_skip_pill"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FastForward,
                contentDescription = null,
                tint = AnimePrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Bottom controls bar:
 * - Seekbar timeline with current/duration time badges, episodes button, and shaders button
 * - Action buttons row: Quality (HD), Subtitle (CC), Sync, Resize Mode, Speed, Audio
 */
@Composable
fun PlayerBottomBar(
    uiState: PlayerUiState,
    onSeek: (Long) -> Unit,
    onEpisodesClick: () -> Unit,
    onShadersClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onSyncClick: () -> Unit,
    onResizeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onAudioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top row of bottom bar: Time & Seekbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current time badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = formatTime(uiState.currentPositionMs),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Episodes list button
            CircularPlayerIconButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = "Episode list",
                onClick = onEpisodesClick,
                size = 38.dp,
                iconSize = 20.dp,
                testTag = "player_episodes_button"
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Shaders / Color profiles button
            CircularPlayerIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = "Color profiles & shaders",
                onClick = onShadersClick,
                size = 38.dp,
                iconSize = 20.dp,
                testTag = "player_shaders_button"
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Seekbar
            val duration = uiState.durationMs
            val position = uiState.currentPositionMs
            val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

            Slider(
                value = progress,
                onValueChange = { frac ->
                    onSeek((frac * duration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = AnimePrimary,
                    activeTrackColor = AnimePrimary,
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("player_progress_slider")
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Total time badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = formatTime(duration),
                    color = Color(0xFFCCD0E0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quality (HD)
            PlayerActionButton(
                icon = Icons.Filled.HighQuality,
                label = uiState.selectedSource?.quality ?: "Quality",
                onClick = onQualityClick,
                testTag = "player_quality_action"
            )

            // Subtitle (CC)
            PlayerActionButton(
                icon = Icons.Filled.ClosedCaption,
                label = uiState.selectedSubtitle?.language ?: "Subtitles",
                onClick = onSubtitleClick,
                testTag = "player_subtitles_action"
            )

            // Subtitle Sync
            PlayerActionButton(
                icon = Icons.Filled.Sync,
                label = if (uiState.subtitleDelayMs != 0L) "${uiState.subtitleDelayMs / 1000f}s" else "Sync",
                onClick = onSyncClick,
                highlight = uiState.subtitleDelayMs != 0L,
                testTag = "player_sync_action"
            )

            // Resize Mode
            PlayerActionButton(
                icon = Icons.Filled.AspectRatio,
                label = uiState.resizeMode.label,
                onClick = onResizeClick,
                testTag = "player_resize_action"
            )

            // Playback Speed
            PlayerActionButton(
                icon = Icons.Filled.Speed,
                label = "${uiState.playbackSpeed}x",
                onClick = onSpeedClick,
                testTag = "player_speed_action"
            )

            // Audio Track
            PlayerActionButton(
                icon = Icons.Filled.Audiotrack,
                label = uiState.selectedAudioTrack?.language ?: "Audio",
                onClick = onAudioClick,
                testTag = "player_audio_action"
            )
        }
    }
}

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    highlight: Boolean = false,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) AnimePrimary.copy(alpha = 0.2f) else Color(0x33000000))
            .border(
                1.dp,
                if (highlight) AnimePrimary.copy(alpha = 0.5f) else Color(0x1AFFFFFF),
                RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlight) AnimePrimary else Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (highlight) AnimePrimary else Color(0xFFE2E4F0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CircularPlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0x66000000))
            .border(1.dp, Color(0x22FFFFFF), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
