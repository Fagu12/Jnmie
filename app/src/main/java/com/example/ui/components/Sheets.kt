package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.model.AniListUser
import com.example.core.model.Episode
import com.example.core.model.VideoSource
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSheetBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    user: AniListUser?,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExtensions: () -> Unit,
    onChangeService: () -> Unit,
    onLoginWithAniList: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    onLogout: () -> Unit,
    onLoginWithToken: ((String) -> Unit)? = null
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            val isConnected = user != null && user.name.isNotBlank() && !user.name.equals("Guest", ignoreCase = true)
            val displayName = com.example.ui.util.getDisplayUsername(user)

            // User / Login Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) AnimePrimary.copy(alpha = 0.3f) else DarkSurfaceVariant)
                        .border(1.5.dp, if (isConnected) AnimePrimary else DarkCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnected && user?.avatarUrl != null) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.name,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Filled.Person else Icons.Filled.AccountCircle,
                            contentDescription = "User",
                            tint = if (isConnected) Color.White else AnimePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConnected) displayName else "AniList Account",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (isConnected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Connected",
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = "Not logged in",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                if (isConnected) {
                    IconButton(onClick = { onLogout() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "AniList Login",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connect your AniList account to sync your anime list and watching progress.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onLoginWithAniList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInBrowser,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Login with AniList", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onSyncNow()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        tint = AnimePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Now", color = AnimePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            // Menu List matching PDF page 4
            ProfileMenuItem(
                icon = Icons.Filled.Person,
                title = "View Profile",
                onClick = { /* View profile */ }
            )
            ProfileMenuItem(
                icon = Icons.Filled.Layers,
                title = "Change Service",
                onClick = {
                    onDismiss()
                    onChangeService()
                }
            )
            ProfileMenuItem(
                icon = Icons.Filled.Extension,
                title = "Extensions",
                onClick = {
                    onDismiss()
                    onNavigateToExtensions()
                }
            )
            ProfileMenuItem(
                icon = Icons.Filled.Download,
                title = "Downloads",
                onClick = { /* Downloads */ }
            )
            ProfileMenuItem(
                icon = Icons.Filled.Storage,
                title = "Local Media",
                onClick = { /* Local Media */ }
            )
            ProfileMenuItem(
                icon = Icons.Filled.Settings,
                title = "Settings",
                onClick = {
                    onDismiss()
                    onNavigateToSettings()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(13.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderBottomSheet(
    currentProvider: String,
    onSelectProvider: (String) -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Choose Provider",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select your preferred content source",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // AniList
            ProviderOptionItem(
                badge = "AL",
                title = "AniList",
                subtitle = "Track and sync your anime watching",
                isSelected = currentProvider == "core_anilist",
                onClick = {
                    onSelectProvider("core_anilist")
                    onDismiss()
                }
            )

            // MyAnimeList
            ProviderOptionItem(
                badge = "MAL",
                title = "MyAnimeList",
                subtitle = "Community anime catalog and database",
                isSelected = currentProvider == "mal",
                onClick = {
                    onSelectProvider("mal")
                    onDismiss()
                }
            )

            // Simkl
            ProviderOptionItem(
                badge = "S",
                title = "Simkl",
                subtitle = "For movies and series",
                isSelected = currentProvider == "simkl",
                onClick = {
                    onSelectProvider("simkl")
                    onDismiss()
                }
            )

            // Extensions
            ProviderOptionItem(
                badge = "🧩",
                title = "Extensions",
                subtitle = "Third-party plugins & community streams",
                isSelected = currentProvider == "extensions",
                onClick = {
                    onSelectProvider("extensions")
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProviderOptionItem(
    badge: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceVariant)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) AnimePrimary else DarkCardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1B1C2A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                color = AnimePrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectBottomSheet(
    episode: Episode,
    sources: List<VideoSource>,
    currentSource: VideoSource?,
    onSelectSource: (VideoSource) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rememberServer by remember { mutableStateOf(true) }

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
            // Episode header matching PDF page 20
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AnimePrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = AnimePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Episode ${episode.number}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = episode.title,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Remember Server container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = AnimePrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remember Server",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auto-select this server quality for this anime",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = rememberServer,
                    onCheckedChange = { rememberServer = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AnimePrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sources list matching PDF page 20
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sources) { src ->
                    val isSelected = src.id == currentSource?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) DarkSurfaceVariant else Color(0xFF141520))
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) AnimePrimary else DarkCardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onSelectSource(src)
                                onDismiss()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AnimePrimary else Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${src.serverName.uppercase()} ${src.quality} [${if (src.isDub) "DUB" else "SUB"}]",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "CC ${src.subtitles.size}",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
