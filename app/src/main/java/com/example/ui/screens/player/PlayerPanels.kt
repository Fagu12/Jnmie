package com.example.ui.screens.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.diagnostics.PlaybackDiagnosticsManager
import com.example.core.diagnostics.PipelineStageItem
import com.example.core.diagnostics.StageStatus
import com.example.core.model.AudioTrack
import com.example.core.model.SubtitleTrack
import com.example.core.model.VideoSource
import com.example.player.ResizeMode
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.DarkCardBorder

/**
 * Common backdrop & slide-in container for all player panels in landscape.
 */
@Composable
fun PlayerSidePanelContainer(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            ),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(440.dp)
                .background(Color(0xF012131F))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Panel Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClose
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
    }
}

/**
 * 1. Quality Settings Panel (Servers, Inbuilt)
 */
@Composable
fun QualitySettingsPanel(
    sources: List<VideoSource>,
    selectedSource: VideoSource?,
    onSelectSource: (VideoSource) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Servers", "Inbuilt")

    PlayerSidePanelContainer(
        title = "Quality Settings",
        onClose = onClose
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(4.dp)
            ) {
                tabs.forEachIndexed { index, tabTitle ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) AnimePrimary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            color = if (isSelected) Color.Black else Color.LightGray,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sources) { src ->
                    val isSelected = selectedSource?.id == src.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AnimePrimary.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                            .border(
                                1.dp,
                                if (isSelected) AnimePrimary else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onSelectSource(src)
                                onClose()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Dns,
                            contentDescription = null,
                            tint = if (isSelected) AnimePrimary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${src.quality} - ${src.serverName ?: "Server"}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (src.isDub) "English Dub" else "Japanese [Sub]",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(AnimePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Subtitle Settings Panel (Source, Embedded, Styling, Preview)
 */
@Composable
fun SubtitleSettingsPanel(
    subtitles: List<SubtitleTrack>,
    selectedSubtitle: SubtitleTrack?,
    settings: com.example.domain.model.AppSettings?,
    onSelectSubtitle: (SubtitleTrack?) -> Unit,
    onFontChange: (String) -> Unit,
    onSizeChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onOutlineChange: (String) -> Unit,
    onResetStyling: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tracks", "Styling")

    PlayerSidePanelContainer(
        title = "Subtitle Settings",
        onClose = onClose
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(4.dp)
            ) {
                tabs.forEachIndexed { index, tabTitle ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) AnimePrimary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            color = if (isSelected) Color.Black else Color.LightGray,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // Subtitle tracks list
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Option to disable subtitles (None)
                    item {
                        val isNone = selectedSubtitle == null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isNone) AnimePrimary.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                                .clickable {
                                    onSelectSubtitle(null)
                                    onClose()
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("None (Subtitles Off)", color = Color.White, modifier = Modifier.weight(1f))
                            if (isNone) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = AnimePrimary)
                            }
                        }
                    }

                    items(subtitles) { sub ->
                        val isSelected = selectedSubtitle?.url == sub.url
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AnimePrimary.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                                .clickable {
                                    onSelectSubtitle(sub)
                                    onClose()
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sub.label.ifBlank { sub.language }, color = Color.White, modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = AnimePrimary)
                            }
                        }
                    }
                }
            } else {
                // Styling section
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live Subtitle Preview Box (like reference video)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33000000))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Subtitle Preview Text",
                            color = when (settings?.subTextColor) {
                                "Yellow" -> Color.Yellow
                                "Cyan" -> Color.Cyan
                                "Green" -> Color.Green
                                else -> Color.White
                            },
                            fontSize = when (settings?.subFontSize) {
                                "Small" -> 14.sp
                                "Large" -> 20.sp
                                "Extra Large" -> 24.sp
                                else -> 16.sp
                            },
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Font Family Selector
                    Text("Font Family", color = Color.LightGray, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Sans Serif", "Serif", "Monospace").forEach { font ->
                            val isSel = settings?.subFontFamily == font
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) AnimePrimary else Color(0x22FFFFFF))
                                    .clickable { onFontChange(font) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(font, color = if (isSel) Color.Black else Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    // Font Size Selector
                    Text("Font Size", color = Color.LightGray, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Small", "Normal", "Large", "Extra Large").forEach { size ->
                            val isSel = settings?.subFontSize == size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) AnimePrimary else Color(0x22FFFFFF))
                                    .clickable { onSizeChange(size) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(size, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    // Text Color Selector
                    Text("Subtitle Color", color = Color.LightGray, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("White", "Yellow", "Cyan", "Green").forEach { color ->
                            val isSel = settings?.subTextColor == color
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) AnimePrimary else Color(0x22FFFFFF))
                                    .clickable { onColorChange(color) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(color, color = if (isSel) Color.Black else Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    // Outline Style
                    Text("Outline Style", color = Color.LightGray, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("None", "Shadow", "Outline").forEach { outline ->
                            val isSel = settings?.subOutlineStyle == outline
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) AnimePrimary else Color(0x22FFFFFF))
                                    .clickable { onOutlineChange(outline) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(outline, color = if (isSel) Color.Black else Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    // Reset button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .clickable { onResetStyling() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Reset Subtitle Styling", color = Color.LightGray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * 3. Sync Subtitles Panel (Matching reference video 00:19)
 */
@Composable
fun SubtitleSyncPanel(
    currentDelayMs: Long,
    cues: List<com.example.player.SubtitleCue>,
    currentPosMs: Long,
    onAdjustDelay: (Long) -> Unit,
    onResetDelay: () -> Unit,
    onClose: () -> Unit
) {
    PlayerSidePanelContainer(
        title = "Sync Subtitles",
        onClose = onClose
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Big sync offset indicator
            val secondsOffset = currentDelayMs / 1000f
            val statusText = if (currentDelayMs == 0L) {
                "0.0s In sync"
            } else if (currentDelayMs > 0) {
                "+${String.format("%.1f", secondsOffset)}s Delayed"
            } else {
                "${String.format("%.1f", secondsOffset)}s Ahead"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33000000))
                    .border(1.dp, AnimePrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText,
                    color = AnimePrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Adjustment buttons: -0.5s, -0.1s, Reset (↺), +0.1s, +0.5s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SyncAdjustmentButton(label = "-0.5s", modifier = Modifier.weight(1f)) { onAdjustDelay(-500L) }
                SyncAdjustmentButton(label = "-0.1s", modifier = Modifier.weight(1f)) { onAdjustDelay(-100L) }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onResetDelay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reset offset",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                SyncAdjustmentButton(label = "+0.1s", modifier = Modifier.weight(1f)) { onAdjustDelay(100L) }
                SyncAdjustmentButton(label = "+0.5s", modifier = Modifier.weight(1f)) { onAdjustDelay(500L) }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Subtitle Timeline Cues", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle cues timeline list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cues) { cue ->
                    val effectivePos = currentPosMs + currentDelayMs
                    val isCurrent = effectivePos >= cue.timestampMs && effectivePos < (cue.timestampMs + 5000L)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) AnimePrimary.copy(alpha = 0.25f) else Color(0x1AFFFFFF))
                            .border(
                                1.dp,
                                if (isCurrent) AnimePrimary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(cue.timestampMs),
                            color = if (isCurrent) AnimePrimary else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = cue.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncAdjustmentButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

/**
 * 4. Audio Settings Panel (Tracks & Audio Channels/Layout)
 */
@Composable
fun AudioSettingsPanel(
    tracks: List<AudioTrack>,
    selectedTrack: AudioTrack?,
    selectedChannels: String,
    onSelectTrack: (AudioTrack?) -> Unit,
    onSelectChannels: (String) -> Unit,
    onClose: () -> Unit
) {
    PlayerSidePanelContainer(
        title = "Audio Settings",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("AUDIO TRACKS", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            // Auto (Default Audio)
            val isAuto = selectedTrack == null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isAuto) AnimePrimary.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                    .clickable {
                        onSelectTrack(null)
                        onClose()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto (Default Audio)", color = Color.White, modifier = Modifier.weight(1f))
                if (isAuto) Icon(Icons.Filled.Check, contentDescription = null, tint = AnimePrimary)
            }

            tracks.forEach { track ->
                val isSelected = selectedTrack?.id == track.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AnimePrimary.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                        .clickable {
                            onSelectTrack(track)
                            onClose()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${track.language} (${track.label})", color = Color.White, modifier = Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = AnimePrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("AUDIO CHANNELS / LAYOUT", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            val channelLayouts = listOf(
                "Mono (1 Channel)",
                "Stereo (2 Channels)",
                "5.1 Surround (6 Channels)",
                "7.1 Surround (8 Channels)"
            )

            channelLayouts.forEach { layout ->
                val isSelected = selectedChannels.startsWith(layout.split(" ")[0])
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AnimePrimary.copy(alpha = 0.2f) else Color(0x22FFFFFF))
                        .clickable { onSelectChannels(layout) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(layout, color = Color.White, modifier = Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = AnimePrimary)
                }
            }
        }
    }
}

/**
 * 5. Player & Experimental Settings Panel (Playback, Shaders, Experimental, Common)
 */
@Composable
fun PlayerSettingsPanel(
    viewModel: PlayerViewModel,
    state: PlayerScreenState,
    onClose: () -> Unit
) {
    val uiState = state.playerUiState

    PlayerSidePanelContainer(
        title = "Player Settings",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PLAYBACK SECTION
            Text("PLAYBACK", color = AnimePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            SettingSwitchRow(
                label = "Hardware Decoder (HW+)",
                subtitle = "Hardware accelerated MediaCodec queueing",
                checked = uiState.isHardwareDecoder,
                onCheckedChange = { viewModel.toggleHardwareDecoder() }
            )

            SettingValueRow(label = "Video Renderer", value = "Auto-select (Default)")
            SettingValueRow(label = "Audio Engine", value = "Android AudioTrack API")

            Spacer(modifier = Modifier.height(4.dp))

            // ANIME 4K ENHANCEMENT
            Text("ANIME 4K ENHANCEMENT", color = AnimePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            SettingSwitchRow(
                label = "Enable Shaders",
                subtitle = "Enhance anime lines and upscaling",
                checked = uiState.shadersEnabled,
                onCheckedChange = { viewModel.setShadersEnabled(it) }
            )

            SettingValueRow(label = "Shader Profile", value = uiState.shaderProfile)

            Spacer(modifier = Modifier.height(4.dp))

            // EXPERIMENTAL
            Text("EXPERIMENTAL", color = AnimePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            SettingSwitchRow(
                label = "Enable Experimental Settings",
                subtitle = "High performance buffer and frame tuning",
                checked = uiState.experimentalSettingsEnabled,
                onCheckedChange = { viewModel.setExperimentalSettings(enabled = it) }
            )

            SettingSwitchRow(
                label = "Audio Pitch Correction",
                subtitle = "Preserve audio pitch when altering speed",
                checked = uiState.audioPitchCorrection,
                onCheckedChange = { viewModel.setExperimentalSettings(pitchCorrection = it) }
            )

            // Cache slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cache Minutes", color = Color.White, fontSize = 13.sp)
                    Text("${uiState.cacheMinutes} min", color = AnimePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = uiState.cacheMinutes.toFloat(),
                    onValueChange = { viewModel.setExperimentalSettings(cacheMinutes = it.toInt()) },
                    valueRange = 10f..120f,
                    colors = SliderDefaults.colors(thumbColor = AnimePrimary, activeTrackColor = AnimePrimary)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // COMMON PREFERENCES
            Text("COMMON", color = AnimePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            SettingSwitchRow(
                label = "Auto Skip Intro",
                subtitle = "Automatically skips recognized openings",
                checked = state.autoSkipIntro,
                onCheckedChange = { viewModel.setAutoSkipIntro(it) }
            )

            SettingSwitchRow(
                label = "Auto Skip Outro",
                subtitle = "Automatically skips recognized endings",
                checked = state.autoSkipOutro,
                onCheckedChange = { viewModel.setAutoSkipOutro(it) }
            )

            SettingSwitchRow(
                label = "Auto Skip Recap",
                subtitle = "Automatically skips episode recaps",
                checked = state.autoSkipRecap,
                onCheckedChange = { viewModel.setAutoSkipRecap(it) }
            )

            // Double Tap Seek Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DoubleTap to Seek", color = Color.White, fontSize = 13.sp)
                    Text("${uiState.doubleTapSeekSeconds}s", color = AnimePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = uiState.doubleTapSeekSeconds.toFloat(),
                    onValueChange = { viewModel.setDoubleTapSeekSeconds(it.toInt()) },
                    valueRange = 5f..30f,
                    steps = 4,
                    colors = SliderDefaults.colors(thumbColor = AnimePrimary, activeTrackColor = AnimePrimary)
                )
            }

            // MegaSkip Duration Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("MegaSkip Duration", color = Color.White, fontSize = 13.sp)
                    Text("${uiState.megaSkipDurationSeconds}s", color = AnimePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = uiState.megaSkipDurationSeconds.toFloat(),
                    onValueChange = { viewModel.setMegaSkipDurationSeconds(it.toInt()) },
                    valueRange = 30f..120f,
                    colors = SliderDefaults.colors(thumbColor = AnimePrimary, activeTrackColor = AnimePrimary)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // DIAGNOSTICS & DEBUGGING
            Text("DIAGNOSTICS", color = AnimePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1AFFFFFF))
                    .clickable {
                        viewModel.setDiagnosticsSheetVisible(true)
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pipeline Diagnostics", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Inspect 15-stage resolver, headers & ExoPlayer state", color = Color.Gray, fontSize = 11.sp)
                }
                Icon(Icons.Filled.Tune, contentDescription = null, tint = AnimePrimary)
            }
        }
    }
}

/**
 * 6. Episodes List Drawer (Landscape Side Sheet)
 */
@Composable
fun EpisodesListPanel(
    episodes: List<com.example.core.model.Episode>,
    currentEpisode: com.example.core.model.Episode?,
    onSelectEpisode: (Int) -> Unit,
    onClose: () -> Unit
) {
    PlayerSidePanelContainer(
        title = "Episodes",
        onClose = onClose
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(episodes) { ep ->
                val isPlaying = currentEpisode?.number == ep.number
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPlaying) AnimePrimary.copy(alpha = 0.25f) else Color(0x1AFFFFFF))
                        .border(
                            1.dp,
                            if (isPlaying) AnimePrimary else Color(0x11FFFFFF),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            onSelectEpisode(ep.number)
                            onClose()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPlaying) AnimePrimary else Color(0x33FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "EP ${ep.number}",
                            color = if (isPlaying) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ep.title?.ifBlank { "Episode ${ep.number}" } ?: "Episode ${ep.number}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Playing",
                            tint = AnimePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 7. Color Profiles / Shaders Panel
 */
@Composable
fun ShadersPanel(
    shadersEnabled: Boolean,
    currentProfile: String,
    onToggleShaders: (Boolean) -> Unit,
    onSelectProfile: (String) -> Unit,
    onClose: () -> Unit
) {
    val profiles = listOf(
        "Anime4K: Mode A (Fast)",
        "Anime4K: Mode B (Upscale)",
        "Anime4K: Mode C (Heavy AA)",
        "Crisp Contrast",
        "Vibrant Color Boost"
    )

    PlayerSidePanelContainer(
        title = "Color Profiles & Shaders",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingSwitchRow(
                label = "Enable Shaders",
                subtitle = "Apply real-time visual sharpening and filtering",
                checked = shadersEnabled,
                onCheckedChange = onToggleShaders
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("PRESETS", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(profiles) { profile ->
                    val isSelected = currentProfile == profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AnimePrimary.copy(alpha = 0.2f) else Color(0x1AFFFFFF))
                            .clickable {
                                onSelectProfile(profile)
                                onToggleShaders(true)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(profile, color = Color.White, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = AnimePrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1AFFFFFF))
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AnimePrimary
            )
        )
    }
}

@Composable
fun SettingValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1AFFFFFF))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
        Text(value, color = Color.LightGray, fontSize = 12.sp)
    }
}

/**
 * 9. Developer Playback Diagnostics & Stream Pipeline Trace Panel
 */
@Composable
fun PlaybackDiagnosticsPanel(
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onOpenServerSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val diagnostics by PlaybackDiagnosticsManager.diagnostics.collectAsStateWithLifecycle()
    var expandedStageNumber by remember { mutableStateOf<Int?>(null) }
    var showCopyNotice by remember { mutableStateOf(false) }

    PlayerSidePanelContainer(
        title = "Pipeline Diagnostics",
        onClose = onClose,
        modifier = modifier.width(520.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val report = buildString {
                                appendLine("=== JUST ANIME STREAM PIPELINE REPORT ===")
                                appendLine("Extension ID: ${diagnostics.extensionId}")
                                appendLine("Extension Load: ${diagnostics.extensionLoadStatus}")
                                appendLine("Anime ID: ${diagnostics.animeId}")
                                appendLine("Episode ID: ${diagnostics.episodeId}")
                                appendLine("Resolver: ${diagnostics.resolverMethod}")
                                appendLine("Resolver Response: ${diagnostics.resolverResponse}")
                                appendLine("Detected Stream Type: ${diagnostics.detectedStreamType}")
                                appendLine("MediaItem URI: ${diagnostics.mediaItemUri}")
                                appendLine("ExoPlayer State: ${diagnostics.exoPlayerPrepState}")
                                appendLine("Playback State: ${diagnostics.actualPlaybackState}")
                                if (diagnostics.exceptionClass != null) {
                                    appendLine("Exception: ${diagnostics.exceptionClass}: ${diagnostics.exceptionMessage}")
                                    appendLine("StackTrace: ${diagnostics.exceptionStackTrace}")
                                }
                                appendLine("\n--- PIPELINE STAGES ---")
                                diagnostics.stages.forEach { stage ->
                                    appendLine("[${stage.formattedTime}] #${stage.stageNumber} ${stage.stageName} [${stage.status}]: ${stage.summary}")
                                    if (stage.details.isNotBlank()) appendLine("    Details: ${stage.details}")
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Just Anime Diagnostics", report)
                            clipboard?.setPrimaryClip(clip)
                            showCopyNotice = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AnimePrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (showCopyNotice) "Copied Report!" else "Copy Diagnostics", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Retry Pipeline", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Error Inspector Card (if error present)
            if (diagnostics.exceptionClass != null || diagnostics.actualPlaybackState.contains("Error", ignoreCase = true)) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33FF4444))
                            .border(1.dp, Color(0x66FF4444), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = Color(0xFFFF6666),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Exception: ${diagnostics.exceptionClass ?: "Playback Error"}",
                                color = Color(0xFFFF8888),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        if (diagnostics.httpStatusCode != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "HTTP Status Code: ${diagnostics.httpStatusCode}",
                                color = Color(0xFFFFB2B2),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val exMsg = diagnostics.exceptionMessage
                        if (!exMsg.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exMsg,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        val stack = diagnostics.exceptionStackTrace
                        if (!stack.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x66000000))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = stack.take(1200),
                                    color = Color(0xFFDDDDDD),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Overview Summary
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("STREAM OVERVIEW", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val extDisplay = if (diagnostics.extensionId.isBlank()) "None" else diagnostics.extensionId
                    val streamDisplay = if (diagnostics.detectedStreamType.isBlank()) "Resolving..." else diagnostics.detectedStreamType
                    val mediaUriDisplay = if (diagnostics.mediaItemUri.isBlank()) "None" else diagnostics.mediaItemUri.takeLast(45)

                    SettingValueRow(label = "Extension ID", value = extDisplay)
                    SettingValueRow(label = "Stream Format", value = streamDisplay)
                    SettingValueRow(label = "Media URI", value = mediaUriDisplay)
                    SettingValueRow(label = "ExoPlayer State", value = diagnostics.exoPlayerPrepState)
                    SettingValueRow(label = "Playback State", value = diagnostics.actualPlaybackState)
                }
            }

            // HTTP Headers Inspector
            if (diagnostics.httpHeaders.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1AFFFFFF))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("HTTP REQUEST HEADERS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        diagnostics.httpHeaders.forEach { (k, v) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(k, color = Color(0xFFAAAAAA), fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text(v.take(40), color = Color.White, fontSize = 11.sp, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // Pipeline Stages List (1 to 15)
            item {
                Text(
                    text = "15-STAGE PIPELINE TRACE",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            items(diagnostics.stages, key = { it.stageNumber }) { stage ->
                val isExpanded = expandedStageNumber == stage.stageNumber
                val statusColor = when (stage.status) {
                    StageStatus.SUCCESS -> Color(0xFF4CAF50)
                    StageStatus.IN_PROGRESS -> Color(0xFFFFB300)
                    StageStatus.ERROR -> Color(0xFFFF5252)
                    StageStatus.WARNING -> Color(0xFFFF9800)
                    StageStatus.PENDING -> Color(0xFF757575)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x15FFFFFF))
                        .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable {
                            expandedStageNumber = if (isExpanded) null else stage.stageNumber
                        }
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "#${stage.stageNumber} ${stage.stageName}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stage.formattedTime,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stage.summary,
                        color = if (stage.status == StageStatus.ERROR) Color(0xFFFF8888) else Color(0xFFCCCCCC),
                        fontSize = 11.sp
                    )

                    if (isExpanded && stage.details.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33000000))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = stage.details,
                                color = Color(0xFFAAAAAA),
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
