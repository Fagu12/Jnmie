package com.example.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.core.config.AppConfig
import com.example.core.model.AudioTrack
import com.example.core.model.Episode
import com.example.core.model.SegmentType
import com.example.core.model.SubtitleTrack
import com.example.player.ResizeMode
import com.example.ui.components.ServerSelectBottomSheet
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSheetBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.SkipSegmentYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val playerState = state.playerUiState
    val playerController = viewModel.getPlayerController()
    val context = LocalContext.current
    val activity = context as? Activity

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }

    // Auto-hide controls after 4 seconds of inactivity while playing
    LaunchedEffect(playerState.isControlsVisible, playerState.isPlaying) {
        if (playerState.isControlsVisible && playerState.isPlaying) {
            delay(4000L)
            viewModel.hideControls()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.toggleControls()
            }
            .testTag("player_screen_root")
    ) {
        // Staging placeholder if streaming disabled
        if (!AppConfig.IS_STREAMING_ENABLED) {
            StreamingFoundationPlaceholder(
                anime = state.anime,
                episode = state.episode,
                onBack = onBack
            )
            return@Box
        }

        // AndroidX Media3 PlayerView Surface
        if (playerController != null && playerController.playerInstance != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerController.playerInstance
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        resizeMode = when (playerState.resizeMode) {
                            ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            ResizeMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        }
                    }
                },
                update = { view ->
                    view.resizeMode = when (playerState.resizeMode) {
                        ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        ResizeMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering / Resolving Loading Overlay
        if ((playerState.isBuffering || state.isResolvingSource) && playerState.error == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AnimePrimary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (state.isResolvingSource) "Resolving streams from extension..." else "Buffering...",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Playback Error Banner & Retry Dialog
        if (playerState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF161726))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Error",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Playback Issue",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = playerState.error ?: "Unable to stream this video.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.retryPlayback() },
                            colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.setServerSheetVisible(true) },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Dns,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch Server", color = Color.White)
                        }
                    }
                }
            }
        }

        // Auto-skip Toast Notice
        AnimatedVisibility(
            visible = playerState.autoSkipNotice != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161726).copy(alpha = 0.95f))
                    .border(1.dp, SkipSegmentYellow.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = playerState.autoSkipNotice ?: "",
                    color = SkipSegmentYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Floating Skip Intro / Outro / Recap Pill Button
        AnimatedVisibility(
            visible = playerState.activeSkipSegment != null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 96.dp)
        ) {
            val seg = playerState.activeSkipSegment
            val label = when (seg?.type) {
                SegmentType.INTRO -> "+85 Skip Intro"
                SegmentType.OUTRO -> "+90 Skip Outro"
                SegmentType.RECAP -> "Skip Recap"
                else -> "Skip"
            }

            Row(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(SkipSegmentYellow)
                    .clickable { viewModel.skipActiveSegment() }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("skip_intro_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Main On-Screen Player Controls Overlay
        AnimatedVisibility(
            visible = playerState.isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.episode?.title ?: "Playing Episode",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "HW+",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${state.anime?.title ?: ""} • Episode ${state.episode?.number ?: 1}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Top Toolbar Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Subtitles Menu
                        IconButton(onClick = { viewModel.setSubtitleSheetVisible(true) }) {
                            Icon(
                                imageVector = Icons.Filled.Subtitles,
                                contentDescription = "Subtitles",
                                tint = if (playerState.selectedSubtitle != null) AnimePrimary else Color.White
                            )
                        }

                        // Audio Track Menu
                        IconButton(onClick = { viewModel.setAudioSheetVisible(true) }) {
                            Icon(
                                imageVector = Icons.Filled.Audiotrack,
                                contentDescription = "Audio Track",
                                tint = Color.White
                            )
                        }

                        // Lock Controls Icon
                        IconButton(onClick = { viewModel.toggleLock() }) {
                            Icon(
                                imageVector = if (playerState.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = "Lock",
                                tint = if (playerState.isLocked) AnimePrimary else Color.White
                            )
                        }

                        // Picture-in-Picture
                        IconButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                val params = PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                                activity.enterPictureInPictureMode(params)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.PictureInPictureAlt,
                                contentDescription = "PiP",
                                tint = Color.White
                            )
                        }

                        // Player Settings
                        IconButton(onClick = { viewModel.setSettingsSheetVisible(true) }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Controls (Play/Pause, Rewind, Forward, Episode Navigation)
                if (!playerState.isLocked) {
                    val currentEpNum = state.episode?.number ?: 1
                    val hasPrevEp = state.episodes.any { it.number == currentEpNum - 1 }
                    val hasNextEp = state.episodes.any { it.number == currentEpNum + 1 }

                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Episode
                        IconButton(
                            onClick = { if (hasPrevEp) viewModel.loadAnimeAndEpisode(currentEpNum - 1) },
                            enabled = hasPrevEp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous Episode",
                                tint = if (hasPrevEp) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Rewind 10s
                        IconButton(
                            onClick = { playerController?.seekBackward(10000L) },
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        // Play/Pause
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(16.dp, CircleShape)
                                .clip(CircleShape)
                                .background(AnimePrimary)
                                .clickable { playerController?.togglePlayPause() }
                                .testTag("player_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        // Fast Forward 10s
                        IconButton(
                            onClick = { playerController?.seekForward(10000L) },
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Next Episode
                        IconButton(
                            onClick = { if (hasNextEp) viewModel.loadAnimeAndEpisode(currentEpNum + 1) },
                            enabled = hasNextEp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next Episode",
                                tint = if (hasNextEp) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline Bar & Quick Actions Toolbar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    if (!playerState.isLocked) {
                        val durationMs = playerState.durationMs.coerceAtLeast(1L)
                        val displayPosMs = if (isScrubbing) scrubPositionMs.toLong() else playerState.currentPositionMs

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Canvas highlight segments for intro/outro/recap skip markers
                            val skipSegments = playerState.selectedSource?.skipSegments ?: emptyList()
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(horizontal = 6.dp)
                            ) {
                                val trackWidth = size.width
                                skipSegments.forEach { seg ->
                                    val startX = (seg.startSeconds * 1000L.toFloat() / durationMs) * trackWidth
                                    val endX = (seg.endSeconds * 1000L.toFloat() / durationMs) * trackWidth
                                    val segWidth = (endX - startX).coerceAtLeast(4f)
                                    drawRect(
                                        color = SkipSegmentYellow,
                                        topLeft = Offset(startX, 0f),
                                        size = Size(segWidth, size.height)
                                    )
                                }
                            }

                            // Interactive Timeline Slider
                            Slider(
                                value = displayPosMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                                onValueChange = {
                                    isScrubbing = true
                                    scrubPositionMs = it
                                },
                                onValueChangeFinished = {
                                    isScrubbing = false
                                    playerController?.seekTo(scrubPositionMs.toLong())
                                },
                                valueRange = 0f..durationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = AnimePrimary,
                                    activeTrackColor = AnimePrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Timestamps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(displayPosMs),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatTime(durationMs),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Episodes Quick Sheet Button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { viewModel.setEpisodesSheetVisible(true) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Episodes",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Server / Source Menu
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { viewModel.setServerSheetVisible(true) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${playerState.selectedSource?.serverName?.uppercase() ?: "SERVER"} [${if (playerState.selectedSource?.isDub == true) "DUB" else "SUB"}]",
                                    color = AnimePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Quality Menu
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { viewModel.setQualitySheetVisible(true) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HighQuality,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = playerState.selectedSource?.quality ?: "1080p",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Playback Speed Menu
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { viewModel.setSpeedSheetVisible(true) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${playerState.playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Fullscreen / Aspect Ratio Toggle Button
                            IconButton(onClick = {
                                val modes = ResizeMode.entries
                                val nextMode = modes[(playerState.resizeMode.ordinal + 1) % modes.size]
                                viewModel.setResizeMode(nextMode)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.AspectRatio,
                                    contentDescription = "Resize Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Bottom Sheets ---

        // 1. Source / Server Selection Sheet
        if (state.showServerSheet && state.episode != null) {
            ServerSelectBottomSheet(
                episode = state.episode!!,
                sources = playerState.availableSources,
                currentSource = playerState.selectedSource,
                onSelectSource = { src ->
                    viewModel.selectSource(src)
                    viewModel.setServerSheetVisible(false)
                },
                onDismiss = { viewModel.setServerSheetVisible(false) }
            )
        }

        // 2. Quality Selection Sheet
        if (state.showQualitySheet) {
            QualitySelectBottomSheet(
                currentQuality = playerState.selectedSource?.quality ?: "1080p",
                availableQualities = listOf("1080p", "720p", "480p", "360p", "Auto"),
                onSelectQuality = { q ->
                    viewModel.selectQuality(q)
                    viewModel.setQualitySheetVisible(false)
                },
                onDismiss = { viewModel.setQualitySheetVisible(false) }
            )
        }

        // 3. Subtitle Menu Sheet
        if (state.showSubtitleSheet) {
            SubtitleSelectBottomSheet(
                subtitles = playerState.selectedSource?.subtitles ?: emptyList(),
                currentSubtitle = playerState.selectedSubtitle,
                onSelectSubtitle = { sub ->
                    viewModel.selectSubtitle(sub)
                    viewModel.setSubtitleSheetVisible(false)
                },
                onDismiss = { viewModel.setSubtitleSheetVisible(false) }
            )
        }

        // 4. Audio Track Sheet
        if (state.showAudioSheet) {
            AudioSelectBottomSheet(
                audioTracks = playerState.selectedSource?.audioTracks ?: emptyList(),
                currentAudioTrack = playerState.selectedAudioTrack,
                onSelectAudioTrack = { track ->
                    viewModel.selectAudioTrack(track)
                    viewModel.setAudioSheetVisible(false)
                },
                onDismiss = { viewModel.setAudioSheetVisible(false) }
            )
        }

        // 5. Playback Speed Menu Sheet
        if (state.showSpeedSheet) {
            SpeedSelectBottomSheet(
                currentSpeed = playerState.playbackSpeed,
                onSelectSpeed = { speed ->
                    viewModel.setPlaybackSpeed(speed)
                    viewModel.setSpeedSheetVisible(false)
                },
                onDismiss = { viewModel.setSpeedSheetVisible(false) }
            )
        }

        // 6. Episodes Quick Menu Sheet
        if (state.showEpisodesSheet) {
            PlayerEpisodesBottomSheet(
                episodes = state.episodes,
                currentEpisodeNumber = state.episode?.number ?: 1,
                onSelectEpisode = { epNum ->
                    viewModel.loadAnimeAndEpisode(epNum)
                    viewModel.setEpisodesSheetVisible(false)
                },
                onDismiss = { viewModel.setEpisodesSheetVisible(false) }
            )
        }

        // 7. Player Settings Sheet (Auto-Skip Intro/Outro/Recap)
        if (state.showSettingsSheet) {
            PlayerSettingsBottomSheet(
                autoSkipIntro = state.autoSkipIntro,
                autoSkipOutro = state.autoSkipOutro,
                onToggleAutoSkipIntro = { viewModel.setAutoSkipIntro(it) },
                onToggleAutoSkipOutro = { viewModel.setAutoSkipOutro(it) },
                onDismiss = { viewModel.setSettingsSheetVisible(false) }
            )
        }
    }
}

// --- Player Sheet Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySelectBottomSheet(
    currentQuality: String,
    availableQualities: List<String>,
    onSelectQuality: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSheetBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Video Quality",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            availableQualities.forEach { q ->
                val isSelected = q.equals(currentQuality, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceVariant else Color(0xFF141520))
                        .clickable { onSelectQuality(q) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = q,
                        color = if (isSelected) AnimePrimary else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = AnimePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtitleSelectBottomSheet(
    subtitles: List<SubtitleTrack>,
    currentSubtitle: SubtitleTrack?,
    onSelectSubtitle: (SubtitleTrack?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSheetBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Subtitles",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Option to turn off subtitles
            val isOffSelected = currentSubtitle == null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isOffSelected) DarkSurfaceVariant else Color(0xFF141520))
                    .clickable { onSelectSubtitle(null) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Off",
                    color = if (isOffSelected) AnimePrimary else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isOffSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Off Selected",
                        tint = AnimePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            subtitles.forEach { sub ->
                val isSelected = currentSubtitle?.url == sub.url
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceVariant else Color(0xFF141520))
                        .clickable { onSelectSubtitle(sub) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sub.label} (${sub.language.uppercase()})",
                        color = if (isSelected) AnimePrimary else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = AnimePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioSelectBottomSheet(
    audioTracks: List<AudioTrack>,
    currentAudioTrack: AudioTrack?,
    onSelectAudioTrack: (AudioTrack) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSheetBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Audio Track",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            val displayTracks = if (audioTracks.isNotEmpty()) audioTracks else listOf(
                AudioTrack(id = "ja", label = "Japanese (Original)", language = "ja", isDefault = true),
                AudioTrack(id = "en", label = "English Dub", language = "en", isDefault = false)
            )

            displayTracks.forEach { track ->
                val isSelected = currentAudioTrack?.id == track.id || (currentAudioTrack == null && track.isDefault)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceVariant else Color(0xFF141520))
                        .clickable { onSelectAudioTrack(track) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.label,
                        color = if (isSelected) AnimePrimary else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = AnimePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSelectBottomSheet(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSheetBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Playback Speed",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            speeds.forEach { speed ->
                val isSelected = currentSpeed == speed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceVariant else Color(0xFF141520))
                        .clickable { onSelectSpeed(speed) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (speed == 1.0f) "Normal (1.0x)" else "${speed}x",
                        color = if (isSelected) AnimePrimary else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = AnimePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsBottomSheet(
    autoSkipIntro: Boolean,
    autoSkipOutro: Boolean,
    onToggleAutoSkipIntro: (Boolean) -> Unit,
    onToggleAutoSkipOutro: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSheetBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Player Settings",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Auto-Skip Intro Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Skip Intro",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automatically jump past anime opening themes (+85s)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = autoSkipIntro,
                    onCheckedChange = onToggleAutoSkipIntro,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AnimePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto-Skip Outro Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Skip Outro",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automatically jump past anime ending credits (+90s)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = autoSkipOutro,
                    onCheckedChange = onToggleAutoSkipOutro,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AnimePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerEpisodesBottomSheet(
    episodes: List<Episode>,
    currentEpisodeNumber: Int,
    onSelectEpisode: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSheetBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Episodes",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(episodes) { ep ->
                    val isCurrent = ep.number == currentEpisodeNumber
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) DarkSurfaceVariant else Color(0xFF141520))
                            .border(
                                width = if (isCurrent) 1.dp else 0.5.dp,
                                color = if (isCurrent) AnimePrimary else DarkCardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectEpisode(ep.number) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EP ${ep.number}",
                            color = if (isCurrent) AnimePrimary else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = ep.title,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
private fun StreamingFoundationPlaceholder(
    anime: com.example.core.model.Anime?,
    episode: com.example.core.model.Episode?,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A10))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF13141F))
                .border(1.dp, Color(0xFF2B2D42), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1D2C))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Player Architecture Staged",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (episode?.thumbnail != null || anime?.coverUrl != null) {
                AsyncImage(
                    model = episode?.thumbnail ?: anime?.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF2B2D42), RoundedCornerShape(16.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1D2C))
                        .border(1.dp, Color(0xFF2B2D42), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = AnimePrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = anime?.title ?: "Anime Player Foundation",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Episode ${episode?.number ?: 1}: ${episode?.title ?: "Full Episode"}",
                color = AnimePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1D2C))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Foundation Architecture Ready",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The Clean Architecture domain, repositories, dependency injection, Room persistence, and navigation foundations are initialized. Media streaming engine will be activated in the next development phase.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Return to Details",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
