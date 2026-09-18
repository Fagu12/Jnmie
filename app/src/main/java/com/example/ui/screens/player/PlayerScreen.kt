package com.example.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.R
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.example.player.ResizeMode
import com.example.ui.theme.AnimePrimary
import kotlinx.coroutines.delay

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Main Landscape Video Player Screen.
 * Strictly adheres to:
 * - Always opens and remains in landscape mode (SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
 * - Restores to portrait upon exit
 * - Debounced back navigation
 * - Smooth auto-hiding controls with double-tap left/right seeking
 * - Custom subtitle styling & synchronization
 * - Floating side panels for quality, subtitles, sync, audio, shaders, and player settings
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val state by viewModel.state.collectAsState()
    val playerState = state.playerUiState

    // Track double tap seeking visual feedback
    var showDoubleTapSeekLeft by remember { mutableStateOf(false) }
    var showDoubleTapSeekRight by remember { mutableStateOf(false) }

    // Debounce back navigation to prevent accidental double pops
    var isBackTriggered by remember { mutableStateOf(false) }
    val handleSafeBack: () -> Unit = {
        if (!isBackTriggered) {
            isBackTriggered = true
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onBack()
        }
    }

    BackHandler {
        if (playerState.isLocked) {
            viewModel.toggleLock()
        } else if (state.showQualitySheet || state.showSubtitleSheet || state.showSyncSubtitlesSheet ||
            state.showAudioSheet || state.showEpisodesSheet || state.showSettingsSheet ||
            state.showShadersSheet || state.showServerSheet
        ) {
            viewModel.closeAllSheets()
        } else {
            handleSafeBack()
        }
    }

    // Strictly enforce landscape mode and immersive full screen for the video player
    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Auto-hide controls overlay after inactivity
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastInteractionTime, playerState.isPlaying, playerState.isControlsVisible, playerState.isLocked) {
        if (playerState.isControlsVisible && playerState.isPlaying && !playerState.isLocked) {
            delay(4500)
            viewModel.hideControls()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_root")
    ) {
        // --- 1. ExoPlayer Video Surface ---
        val playerInstance = state.playerController?.playerInstance ?: viewModel.getPlayerController()?.playerInstance
        if (playerInstance != null) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.player_view_layout, null) as PlayerView).apply {
                        player = playerInstance
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(AndroidColor.BLACK)
                    }
                },
                update = { view ->
                    if (view.player != playerInstance) {
                        view.player = playerInstance
                    }
                    view.resizeMode = when (playerState.resizeMode) {
                        ResizeMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        ResizeMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }

                    // Apply Subtitle Styling
                    state.settings?.let { settings ->
                        view.subtitleView?.let { subtitleView ->
                            val typeface = when (settings.subFontFamily) {
                                "Sans Serif" -> Typeface.SANS_SERIF
                                "Serif" -> Typeface.SERIF
                                "Monospace" -> Typeface.MONOSPACE
                                else -> Typeface.DEFAULT
                            }
                            val textStyle = if (settings.subFontWeight == "Bold") Typeface.BOLD else Typeface.NORMAL
                            val finalTypeface = Typeface.create(typeface, textStyle)

                            val textColor = when (settings.subTextColor) {
                                "Yellow" -> AndroidColor.YELLOW
                                "Cyan" -> AndroidColor.CYAN
                                "Green" -> AndroidColor.GREEN
                                "Magenta" -> AndroidColor.MAGENTA
                                "Red" -> AndroidColor.RED
                                else -> AndroidColor.WHITE
                            }

                            val edgeType = when (settings.subOutlineStyle) {
                                "Shadow" -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                                "Outline" -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                                else -> CaptionStyleCompat.EDGE_TYPE_NONE
                            }

                            val bgColor = when (settings.subBackgroundStyle) {
                                "Semi-transparent" -> AndroidColor.argb((255 * settings.subBackgroundOpacity).toInt(), 0, 0, 0)
                                "Solid" -> AndroidColor.BLACK
                                else -> AndroidColor.TRANSPARENT
                            }

                            val captionStyle = CaptionStyleCompat(
                                textColor,
                                bgColor,
                                AndroidColor.TRANSPARENT,
                                edgeType,
                                AndroidColor.BLACK,
                                finalTypeface
                            )
                            subtitleView.setStyle(captionStyle)

                            val sizeMultiplier = when (settings.subFontSize) {
                                "Small" -> 0.85f
                                "Large" -> 1.25f
                                "Extra Large" -> 1.55f
                                else -> 1.0f
                            }
                            subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 17f * sizeMultiplier)

                            val bottomFraction = if (settings.subPosition == "Slightly above bottom") 0.15f else 0.06f
                            subtitleView.setBottomPaddingFraction(bottomFraction)
                        }
                    }
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 2. Full-Screen Gestures Detector (Tap to toggle, double tap seek) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (!playerState.isLocked) {
                                lastInteractionTime = System.currentTimeMillis()
                                viewModel.toggleControls()
                            }
                        },
                        onDoubleTap = { offset ->
                            if (!playerState.isLocked) {
                                val width = size.width
                                val seekSeconds = playerState.doubleTapSeekSeconds
                                if (offset.x < width / 2) {
                                    // Rewind left
                                    viewModel.getPlayerController()?.seekBackward(seekSeconds * 1000L)
                                    showDoubleTapSeekLeft = true
                                    showDoubleTapSeekRight = false
                                } else {
                                    // Fast forward right
                                    viewModel.getPlayerController()?.seekForward(seekSeconds * 1000L)
                                    showDoubleTapSeekRight = true
                                    showDoubleTapSeekLeft = false
                                }
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        }
                    )
                }
        )

        // Double-Tap Seek Animations
        LaunchedEffect(showDoubleTapSeekLeft) {
            if (showDoubleTapSeekLeft) {
                delay(600)
                showDoubleTapSeekLeft = false
            }
        }
        LaunchedEffect(showDoubleTapSeekRight) {
            if (showDoubleTapSeekRight) {
                delay(600)
                showDoubleTapSeekRight = false
            }
        }

        if (showDoubleTapSeekLeft) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 64.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("-${playerState.doubleTapSeekSeconds}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (showDoubleTapSeekRight) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 64.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text("+${playerState.doubleTapSeekSeconds}s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // --- 3. Loading / Buffering Spinner ---
        if ((playerState.isBuffering || state.isResolvingSource) && playerState.error == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AnimePrimary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        // --- 4. Stream / Network Error Overlay ---
        if (playerState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD090A10)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Error",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to play this stream",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playerState.error ?: "Connection failed. Please check your network and retry.",
                        color = Color(0xFFB0B4C8),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { viewModel.retryPlayback() },
                            colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary)
                        ) {
                            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { viewModel.setQualitySheetVisible(true) }) {
                            Text("Change Server", color = Color.White)
                        }
                        OutlinedButton(onClick = { viewModel.setDiagnosticsSheetVisible(true) }) {
                            Text("Diagnostics", color = Color(0xFFFFB74D))
                        }
                        OutlinedButton(onClick = handleSafeBack) {
                            Text("Go Back", color = Color.White)
                        }
                    }
                }
            }
        }

        // --- 5. End of Episode Card ---
        AnimatedVisibility(
            visible = playerState.isEnded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD090A10)),
                contentAlignment = Alignment.Center
            ) {
                val nextEp = state.episodes.find { it.number == (state.episode?.number ?: 0) + 1 }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (nextEp != null) {
                        var countdown by remember { mutableIntStateOf(5) }
                        val autoPlay = state.settings?.autoPlayNext ?: true

                        LaunchedEffect(autoPlay, playerState.isEnded) {
                            if (autoPlay && playerState.isEnded) {
                                countdown = 5
                                while (countdown > 0) {
                                    delay(1000)
                                    countdown -= 1
                                }
                                viewModel.loadAnimeAndEpisode(nextEp.number)
                            }
                        }

                        Text("Up Next", color = Color.LightGray, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Episode ${nextEp.number}: ${nextEp.title ?: ""}",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        if (autoPlay) {
                            Text("Playing next in $countdown seconds...", color = AnimePrimary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = { viewModel.loadAnimeAndEpisode(nextEp.number) },
                                colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary)
                            ) {
                                Text("Play Now", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = handleSafeBack) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    } else {
                        Text("You've reached the end of available episodes.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = handleSafeBack, colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary)) {
                            Text("Return to Anime", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 6. Locked State Floating Unlock Overlay ---
        AnimatedVisibility(
            visible = playerState.isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .border(1.dp, AnimePrimary, CircleShape)
                        .clickable { viewModel.toggleLock() }
                        .testTag("player_unlock_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = "Unlock", tint = AnimePrimary)
                }
            }
        }

        // --- 6b. Floating Skip Pill when inside a known segment and controls are hidden ---
        AnimatedVisibility(
            visible = playerState.activeSkipSegment != null && !playerState.isLocked && !playerState.isControlsVisible && playerState.error == null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 24.dp, bottom = 48.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                PlayerSkipPill(
                    uiState = playerState,
                    onSkipClick = {
                        viewModel.skipActiveSegment()
                    }
                )
            }
        }

        // --- 7. Main Player Controls Overlay (Top Bar, Center, Skip Pill, Bottom Bar) ---
        AnimatedVisibility(
            visible = playerState.isControlsVisible && playerState.error == null && !playerState.isLocked,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    )
            ) {
                // Top Bar
                PlayerTopBar(
                    state = state,
                    uiState = playerState,
                    onBack = handleSafeBack,
                    onToggleLock = { viewModel.toggleLock() },
                    onToggleDecoder = { viewModel.toggleHardwareDecoder() },
                    onPipClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val params = android.app.PictureInPictureParams.Builder().build()
                            activity?.enterPictureInPictureMode(params)
                        }
                    },
                    onSettingsClick = { viewModel.setSettingsSheetVisible(true) },
                    onDiagnosticsClick = { viewModel.setDiagnosticsSheetVisible(true) },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Center Playback Controls
                val currentEpNum = state.episode?.number ?: 1
                val hasPrevious = state.episodes.any { it.number == currentEpNum - 1 }
                val hasNext = state.episodes.any { it.number == currentEpNum + 1 }

                PlayerCenterControls(
                    uiState = playerState,
                    hasPrevious = hasPrevious,
                    hasNext = hasNext,
                    onPrevious = {
                        val prev = state.episodes.find { it.number == currentEpNum - 1 }
                        if (prev != null) viewModel.loadAnimeAndEpisode(prev.number)
                    },
                    onPlayPause = {
                        lastInteractionTime = System.currentTimeMillis()
                        viewModel.getPlayerController()?.togglePlayPause()
                    },
                    onNext = {
                        val next = state.episodes.find { it.number == currentEpNum + 1 }
                        if (next != null) viewModel.loadAnimeAndEpisode(next.number)
                    },
                    modifier = Modifier.align(Alignment.Center)
                )

                // Floating Skip Pill (right above timeline seekbar)
                PlayerSkipPill(
                    uiState = playerState,
                    onSkipClick = {
                        if (playerState.activeSkipSegment != null) {
                            viewModel.skipActiveSegment()
                        } else {
                            viewModel.megaSkip()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 120.dp)
                )

                // Bottom Bar (Timeline + Actions)
                PlayerBottomBar(
                    uiState = playerState,
                    onSeek = { targetMs ->
                        lastInteractionTime = System.currentTimeMillis()
                        viewModel.getPlayerController()?.seekTo(targetMs)
                    },
                    onEpisodesClick = { viewModel.setEpisodesSheetVisible(true) },
                    onShadersClick = { viewModel.setShadersSheetVisible(true) },
                    onQualityClick = { viewModel.setQualitySheetVisible(true) },
                    onSubtitleClick = { viewModel.setSubtitleSheetVisible(true) },
                    onSyncClick = { viewModel.setSyncSubtitlesSheetVisible(true) },
                    onResizeClick = { viewModel.cycleResizeMode() },
                    onSpeedClick = { viewModel.setSpeedSheetVisible(true) },
                    onAudioClick = { viewModel.setAudioSheetVisible(true) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // --- 8. Sliding Landscape Settings Panels ---
        if (state.showQualitySheet) {
            QualitySettingsPanel(
                sources = playerState.availableSources,
                selectedSource = playerState.selectedSource,
                onSelectSource = { viewModel.selectSource(it) },
                onClose = { viewModel.closeAllSheets() }
            )
        }

        if (state.showSubtitleSheet) {
            SubtitleSettingsPanel(
                subtitles = playerState.selectedSource?.subtitles ?: emptyList(),
                selectedSubtitle = playerState.selectedSubtitle,
                settings = state.settings,
                onSelectSubtitle = { viewModel.selectSubtitle(it) },
                onFontChange = { viewModel.updateSubtitleFontFamily(it) },
                onSizeChange = { viewModel.updateSubtitleFontSize(it) },
                onColorChange = { viewModel.updateSubtitleTextColor(it) },
                onOutlineChange = { viewModel.updateSubtitleOutlineStyle(it) },
                onResetStyling = { viewModel.resetSubtitleStyling() },
                onClose = { viewModel.closeAllSheets() }
            )
        }

        if (state.showSyncSubtitlesSheet) {
            SubtitleSyncPanel(
                currentDelayMs = playerState.subtitleDelayMs,
                cues = playerState.currentSubtitleCues,
                currentPosMs = playerState.currentPositionMs,
                onAdjustDelay = { deltaMs -> viewModel.adjustSubtitleDelay(deltaMs) },
                onResetDelay = { viewModel.resetSubtitleDelay() },
                onClose = { viewModel.closeAllSheets() }
            )
        }

        if (state.showAudioSheet) {
            AudioSettingsPanel(
                tracks = playerState.selectedSource?.audioTracks ?: emptyList(),
                selectedTrack = playerState.selectedAudioTrack,
                selectedChannels = playerState.audioChannels,
                onSelectTrack = { viewModel.selectAudioTrack(it) },
                onSelectChannels = { viewModel.setAudioChannels(it) },
                onClose = { viewModel.closeAllSheets() }
            )
        }

        if (state.showSettingsSheet) {
            PlayerSettingsPanel(
                viewModel = viewModel,
                state = state,
                onClose = { viewModel.closeAllSheets() }
            )
        }

        if (state.showEpisodesSheet) {
            EpisodesListPanel(
                episodes = state.episodes,
                currentEpisode = state.episode,
                onSelectEpisode = { viewModel.loadAnimeAndEpisode(it) },
                onClose = { viewModel.closeAllSheets() }
            )
        }

        if (state.showShadersSheet) {
            ShadersPanel(
                shadersEnabled = playerState.shadersEnabled,
                currentProfile = playerState.shaderProfile,
                onToggleShaders = { viewModel.setShadersEnabled(it) },
                onSelectProfile = { viewModel.setShaderProfile(it) },
                onClose = { viewModel.closeAllSheets() }
            )
        }

        if (state.showSpeedSheet) {
            SpeedSelectionDialog(
                currentSpeed = playerState.playbackSpeed,
                onSelectSpeed = {
                    viewModel.setPlaybackSpeed(it)
                    viewModel.closeAllSheets()
                },
                onClose = { viewModel.closeAllSheets() }
            )
        }

        if (state.showDiagnosticsSheet) {
            PlaybackDiagnosticsPanel(
                onClose = { viewModel.closeAllSheets() },
                onRetry = { viewModel.retryPlayback() },
                onOpenServerSheet = { viewModel.setQualitySheetVisible(true) }
            )
        }
    }
}

/**
 * Quick Playback Speed selector dialog.
 */
@Composable
fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onClose: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    PlayerSidePanelContainer(
        title = "Playback Speed",
        onClose = onClose
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(speeds) { spd ->
                val isSelected = currentSpeed == spd
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AnimePrimary.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                        .clickable { onSelectSpeed(spd) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${spd}x", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (isSelected) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = AnimePrimary)
                    }
                }
            }
        }
    }
}
