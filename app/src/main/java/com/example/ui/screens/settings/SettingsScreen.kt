package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.config.AniListAuthConfig
import com.example.core.config.AppConfig
import com.example.core.model.Anime
import com.example.core.model.PlaybackProgress
import com.example.ui.components.AnimeCard
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.getDisplayUsername

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToExtensions: () -> Unit = {},
    onNavigateToSubtitleSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }
    var showOAuthDiagnosticsDialog by remember { mutableStateOf(false) }

    val previewAnime = remember {
        Anime(
            id = "preview_frieren",
            title = "Frieren: Beyond Journey's End",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-n6bLAcVakMmB.jpg",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-ivXNJ23SQ1cr.jpg",
            score = 9.3,
            format = "TV",
            status = "Finished",
            totalEpisodes = 28,
            genres = listOf("Adventure", "Drama", "Fantasy"),
            studio = "Madhouse"
        )
    }

    val previewProgress = remember {
        PlaybackProgress(
            animeId = "preview_frieren",
            animeTitle = "Frieren: Beyond Journey's End",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-n6bLAcVakMmB.jpg",
            episodeNumber = 12,
            episodeTitle = "A Real Mage",
            currentPositionMs = 14 * 60 * 1000L,
            durationMs = 24 * 60 * 1000L,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_scroll"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp, top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. ANILIST LOGIN / ACCOUNT SECTION
            item {
                val displayUser = getDisplayUsername(uiState.user, uiState.savedUsername)
                val isConnected = uiState.isAuthenticated || !uiState.savedUsername.isNullOrBlank()

                if (!isConnected) {
                    // Logged-out UI: "AniList Login"
                    SettingsSectionHeader("AniList Login", Icons.Filled.AccountCircle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DarkBackground)
                                    .border(1.5.dp, DarkCardBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    tint = AnimePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AniList Login",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Connect your AniList account to sync your anime list and watching progress.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Security & Privacy Note
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign in securely via AniList in your browser. Your credentials remain private with AniList.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.launchOAuthInBrowser(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_with_anilist_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInBrowser,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Login with AniList",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = { showOAuthDiagnosticsDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test OAuth Configuration",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (AniListAuthConfig.isPlaceholderClientId()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1E1E)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Developer Error: A real AniList OAuth Client ID must be configured in AniListAuthConfig.",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Logged-in UI: "AniList Account"
                    SettingsSectionHeader("AniList Account", Icons.Filled.AccountCircle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        // Header Row: Avatar, Username, Status: Connected, Logout button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(AnimePrimary.copy(alpha = 0.2f))
                                    .border(2.dp, AnimePrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.user?.avatarUrl != null) {
                                    AsyncImage(
                                        model = uiState.user?.avatarUrl,
                                        contentDescription = displayUser,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = AnimePrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayUser,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Connected",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.logout() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1619)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Logout", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Security & Encryption Indicator
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OAuth token encrypted via Android KeyStore (AES-256 GCM)",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Connected Stats Grid
                        uiState.user?.let { user ->
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${user.totalAnimeWatched}",
                                        color = AnimePrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Anime", color = TextMuted, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${user.totalEpisodesWatched}",
                                        color = AnimePrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Episodes", color = TextMuted, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "%.1f".format(user.daysWatched),
                                        color = AnimePrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Days", color = TextMuted, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (user.meanScore > 0) "%.1f".format(user.meanScore) else "-",
                                        color = AnimePrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Mean Score", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.syncAniList() },
                            enabled = !uiState.isSyncingAniList,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_now_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AnimePrimary,
                                disabledContainerColor = DarkSurfaceVariant
                            )
                        ) {
                            if (uiState.isSyncingAniList) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing with AniList...", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. PLAYER SECTION
            item {
                SettingsSectionHeader("Player", Icons.Filled.PlayCircle)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Default Quality
                    Column {
                        Text("Default Video Quality", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1080p", "720p", "480p", "Auto").forEach { quality ->
                                val isSel = uiState.settings.defaultQuality == quality
                                SelectionChip(
                                    label = quality,
                                    selected = isSel,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setDefaultQuality(quality) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))

                    // Audio / Subtitle Preference
                    Column {
                        Text("Audio Preference", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Sub" to "Subbed (Japanese)", "Dub" to "Dubbed (English)").forEach { (key, label) ->
                                val isSel = uiState.settings.audioSubPreference == key
                                SelectionChip(
                                    label = label,
                                    selected = isSel,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setAudioSubPreference(key) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))

                    // Playback Speed
                    Column {
                        Text("Default Playback Speed", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x", 1.5f to "1.5x", 2.0f to "2.0x").forEach { (speed, label) ->
                                val isSel = uiState.settings.defaultPlaybackSpeed == speed
                                SelectionChip(
                                    label = label,
                                    selected = isSel,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setDefaultPlaybackSpeed(speed) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))

                    // Skip duration
                    Column {
                        Text("Intro Skip Duration", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(60 to "60s", 85 to "85s (Anime Std)", 90 to "90s").forEach { (sec, label) ->
                                val isSel = uiState.settings.skipDurationSeconds == sec
                                SelectionChip(
                                    label = label,
                                    selected = isSel,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setSkipDuration(sec) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))

                    // Toggles
                    SettingToggleRow(
                        title = "Auto-Skip Opening",
                        subtitle = "Automatically advance through intro song chapters",
                        checked = uiState.settings.autoSkipIntro,
                        onCheckedChange = { viewModel.setAutoSkipIntro(it) }
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SettingToggleRow(
                        title = "Auto-Skip Ending",
                        subtitle = "Skip outro credits when timestamp chapter markers exist",
                        checked = uiState.settings.autoSkipOutro,
                        onCheckedChange = { viewModel.setAutoSkipOutro(it) }
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SettingToggleRow(
                        title = "Auto-Skip Recap",
                        subtitle = "Skip previous episode recap segments automatically",
                        checked = uiState.settings.autoSkipRecap,
                        onCheckedChange = { viewModel.setAutoSkipRecap(it) }
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SettingToggleRow(
                        title = "Player Gestures",
                        subtitle = "Double tap to seek, vertical swipe for volume and brightness",
                        checked = uiState.settings.playerGestures,
                        onCheckedChange = { viewModel.setPlayerGestures(it) }
                    )
                }
            }

            // 3. PLAYBACK SECTION
            item {
                SettingsSectionHeader("Playback", Icons.Filled.Tune)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Preferred Source
                    Column {
                        Text("Default Source Provider", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("core_anilist" to "AniList (Sync)", "ext_gogo" to "GogoAnime", "ext_zoro" to "Zoro/HiAnime").forEach { (id, label) ->
                                val isSel = uiState.settings.preferredProviderId == id
                                SelectionChip(
                                    label = label,
                                    selected = isSel,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setPreferredProvider(id) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))

                    SettingToggleRow(
                        title = "Auto-Play Next Episode",
                        subtitle = "Automatically load the next episode when current finishes",
                        checked = uiState.settings.autoPlayNext,
                        onCheckedChange = { viewModel.setAutoPlayNext(it) }
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SettingToggleRow(
                        title = "Background Playback",
                        subtitle = "Continue playing audio when app is minimized",
                        checked = uiState.settings.backgroundPlayback,
                        onCheckedChange = { viewModel.setBackgroundPlayback(it) }
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SettingToggleRow(
                        title = "Picture-in-Picture (PiP)",
                        subtitle = "Shrink player to floating window when leaving app",
                        checked = uiState.settings.pipEnabled,
                        onCheckedChange = { viewModel.setPipEnabled(it) }
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    Column {
                        Text("Buffer Cache Size", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(64 to "64 MB", 128 to "128 MB (Rec)", 256 to "256 MB").forEach { (mb, label) ->
                                val isSel = uiState.settings.bufferCacheMb == mb
                                SelectionChip(
                                    label = label,
                                    selected = isSel,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setBufferCacheMb(mb) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. AUTHORIZED STREAMING PROVIDERS SECTION
            item {
                SettingsSectionHeader("Authorized Streaming Providers", Icons.Filled.Source)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Native Providers & Media Extractors",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Authorized high-speed streaming resolvers built into the app engine. Video streams are resolved directly and played with hardware acceleration.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    uiState.availableProviders.forEach { prov ->
                        val isPreferred = uiState.settings.preferredProviderId == prov.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPreferred) AnimePrimary.copy(alpha = 0.12f) else DarkBackground)
                                .border(1.dp, if (isPreferred) AnimePrimary else DarkCardBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.setPreferredProvider(prov.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = prov.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isPreferred) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(Default)",
                                            color = AnimePrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    text = prov.description ?: prov.baseUrl,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            if (isPreferred) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = AnimePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. APPEARANCE SECTION
            item {
                SettingsSectionHeader("Appearance", Icons.Filled.Palette)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppTheme.colors.surfaceVariant)
                        .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // LIVE PREVIEW CARD
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppTheme.colors.surface)
                            .border(1.dp, AppTheme.colors.cardBorder.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = AnimePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Live Appearance Preview",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (AppTheme.colors.isAmoled) Color(0xFF00B894).copy(alpha = 0.2f)
                                        else AnimePrimary.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (AppTheme.colors.isAmoled) "OLED Pitch Black" else "Midnight Slate",
                                    color = if (AppTheme.colors.isAmoled) Color(0xFF00B894) else AnimePrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(modifier = Modifier.width(106.dp)) {
                                AnimeCard(
                                    anime = previewAnime,
                                    onClick = {},
                                    cardStyle = uiState.settings.cardStyle
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Card: ${uiState.settings.cardStyle}",
                                    color = AnimePrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "History: ${uiState.settings.historyCardStyle}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Carousel: ${uiState.settings.carouselStyle}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                ContinueWatchingCard(
                                    progress = previewProgress,
                                    onClick = {},
                                    style = uiState.settings.historyCardStyle,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = AppTheme.colors.cardBorder.copy(alpha = 0.4f))

                    SettingToggleRow(
                        title = "Pure Black (OLED)",
                        subtitle = "Uses true black (#000000) backgrounds to save battery on OLED screens",
                        checked = uiState.settings.amoledPureBlack,
                        onCheckedChange = { viewModel.setAmoledPureBlack(it) }
                    )

                    HorizontalDivider(color = AppTheme.colors.cardBorder.copy(alpha = 0.4f))

                    Text("Card Style", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Saikou", "Default", "Exotic", "Minimal").forEach { style ->
                            val isSel = uiState.settings.cardStyle.equals(style, ignoreCase = true) ||
                                    (style == "Minimal" && uiState.settings.cardStyle.startsWith("Minimal", ignoreCase = true))
                            SelectionChip(
                                label = style,
                                selected = isSel,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setCardStyle(if (style == "Minimal") "Minimal Exotic" else style) }
                            )
                        }
                    }

                    HorizontalDivider(color = AppTheme.colors.cardBorder.copy(alpha = 0.4f))

                    Text("Carousel Style", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Classic", "Portrait").forEach { style ->
                            val isSel = uiState.settings.carouselStyle.equals(style, ignoreCase = true)
                            SelectionChip(
                                label = style,
                                selected = isSel,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setCarouselStyle(style) }
                            )
                        }
                    }

                    HorizontalDivider(color = AppTheme.colors.cardBorder.copy(alpha = 0.4f))

                    Text("History Card Style", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Regular", "Frosted Glass", "Bootiful").forEach { style ->
                            val isSel = uiState.settings.historyCardStyle.equals(style, ignoreCase = true)
                            SelectionChip(
                                label = style,
                                selected = isSel,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setHistoryCardStyle(style) }
                            )
                        }
                    }

                    HorizontalDivider(color = AppTheme.colors.cardBorder.copy(alpha = 0.4f))

                    Text("Bottom Navigation Style", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Dynamic Pill", "Classic").forEach { style ->
                            val isSel = uiState.settings.navBarStyle.equals(style, ignoreCase = true)
                            SelectionChip(
                                label = style,
                                selected = isSel,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setNavBarStyle(style) }
                            )
                        }
                    }
                }
            }

            // 7. DATA & STORAGE SECTION
            item {
                SettingsSectionHeader("Data & Storage", Icons.Filled.Storage)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingActionRow(
                        title = "Clear Playback Cache",
                        subtitle = "Frees temporary video chunks and media cache",
                        actionLabel = "Clear",
                        onClick = { viewModel.clearPlaybackCache() }
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SettingActionRow(
                        title = "Clear Watch History",
                        subtitle = "Permanently deletes local episode playback history",
                        actionLabel = "Clear History",
                        isDestructive = true,
                        onClick = { showClearHistoryDialog = true }
                    )
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.4f))
                    SettingActionRow(
                        title = "Reset Application Settings",
                        subtitle = "Restores all preferences and toggles to default values",
                        actionLabel = "Reset",
                        isDestructive = true,
                        onClick = { showResetSettingsDialog = true }
                    )
                }
            }

            // 8. ABOUT SECTION
            item {
                SettingsSectionHeader("About", Icons.Filled.Info)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(AppConfig.APP_NAME, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Version ${AppConfig.APP_VERSION} • ${AppConfig.APP_BUILD_NAME}", color = AnimePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        AppConfig.APP_TAGLINE,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Clear History Confirmation
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = DarkSurfaceVariant,
            title = { Text("Clear Watch History?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all your watched episodes and progress locally.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearWatchHistory()
                    showClearHistoryDialog = false
                }) {
                    Text("Clear All", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showOAuthDiagnosticsDialog) {
        AlertDialog(
            onDismissRequest = { showOAuthDiagnosticsDialog = false },
            containerColor = DarkSurfaceVariant,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("OAuth Diagnostics") },
            text = {
                Column {
                    val clientId = AniListAuthConfig.CLIENT_ID
                    val isPlaceholder = AniListAuthConfig.isPlaceholderClientId()
                    val maskedClientId = if (isPlaceholder) "YOUR_ANILIST_CLIENT_ID" else if (clientId.length > 8) clientId.take(4) + "..." + clientId.takeLast(4) else "INVALID"
                    
                    Text("Client ID: $maskedClientId", fontSize = 13.sp)
                    Text("Redirect URI: ${AniListAuthConfig.REDIRECT_URI}", fontSize = 13.sp)
                    Text("Auth Endpoint: ${AniListAuthConfig.OAUTH_AUTHORIZE_URL}", fontSize = 13.sp)
                    Text("Token Endpoint: ${AniListAuthConfig.OAUTH_TOKEN_URL}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Deep-link handler registered: Yes (animex://anilist-auth)", fontSize = 13.sp)
                    Text("AI Studio Preview Mode: ${AniListAuthConfig.IS_AI_STUDIO_PREVIEW}", fontSize = 13.sp)
                    if (isPlaceholder) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Status: FAILED. Missing real Client ID.",
                            color = Color(0xFFEF5350),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (AniListAuthConfig.IS_AI_STUDIO_PREVIEW) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Status: AI Studio Preview. The browser emulator may not be able to redirect to the Android app. Test on a real device or local emulator.",
                            color = Color(0xFFFFB74D),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Status: READY.",
                            color = Color(0xFF81C784),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOAuthDiagnosticsDialog = false }) {
                    Text("Close", color = AnimePrimary)
                }
            }
        )
    }

    // Reset Settings Confirmation
    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            containerColor = DarkSurfaceVariant,
            title = { Text("Reset Settings?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("All playback preferences and UI appearance styles will be restored to default.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetSettings()
                    showResetSettingsDialog = false
                }) {
                    Text("Reset Defaults", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSettingsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = AnimePrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = AnimePrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SelectionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AnimePrimary else AppTheme.colors.surface)
            .border(1.dp, if (selected) AnimePrimary else AppTheme.colors.cardBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
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
private fun SettingActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDestructive) Color(0xFF2D1619) else Color(0xFF1B1D30)
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = actionLabel,
                color = if (isDestructive) Color(0xFFFF5252) else AnimePrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
