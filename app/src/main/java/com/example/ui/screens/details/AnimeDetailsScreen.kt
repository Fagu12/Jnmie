package com.example.ui.screens.details
import androidx.compose.material.icons.filled.CloudOff

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Anime
import com.example.domain.model.AnimeCharacter
import com.example.domain.model.AnimeExternalLink
import com.example.domain.model.AnimeRecommendation
import com.example.domain.model.AnimeRelation
import com.example.domain.model.AnimeStaff
import com.example.domain.model.AnimeTag
import com.example.domain.model.Episode
import com.example.domain.model.ProviderInfo
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSheetBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val LIST_STATUSES = listOf(
    "CURRENT" to "Watching",
    "PLANNING" to "Plan to Watch",
    "COMPLETED" to "Completed",
    "DROPPED" to "Dropped",
    "PAUSED" to "Paused"
)

private fun formatTimeUntil(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeDetailsScreen(
    viewModel: AnimeDetailsViewModel,
    onBack: () -> Unit,
    onPlayEpisode: (String, Int) -> Unit,
    onNavigateToAnime: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSourceSheet by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (uiState.isLoading && uiState.anime == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("anime_details_loading"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AnimePrimary, strokeWidth = 3.dp)
            }
        } else if (uiState.error != null && uiState.anime == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.CloudOff, contentDescription = "Error", tint = TextMuted, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(uiState.error ?: "Failed to load", color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = { viewModel.loadAnime() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AnimePrimary)
                ) {
                    Text("Retry", color = Color.Black)
                }
            }
        } else {
            val anime = uiState.anime ?: return

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("anime_details_scroll"),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // ==========================================
                // 1. HERO BACKDROP & TOP BAR OVERLAY
                // ==========================================
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                    ) {
                        // Banner artwork
                        AsyncImage(
                            model = anime.bannerUrl ?: anime.coverUrl,
                            contentDescription = anime.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient fading
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color(0xFF090A10).copy(alpha = 0.55f),
                                            0.5f to Color(0xFF090A10).copy(alpha = 0.82f),
                                            1.0f to DarkBackground
                                        )
                                    )
                                )
                        )

                        // Top bar navigation controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.testTag("details_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Watchlist / Favorite toggle action
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite() },
                                        modifier = Modifier.testTag("details_favorite_button")
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = "Watchlist",
                                            tint = if (uiState.isFavorite) AnimePrimary else Color.White
                                        )
                                    }
                                }

                                // Share action
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, anime.title)
                                                putExtra(Intent.EXTRA_TEXT, "Check out ${anime.title} on Just Anime! https://anilist.co/anime/${anime.id}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Anime"))
                                        },
                                        modifier = Modifier.testTag("details_share_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Poster artwork & Primary Title header overlapping hero base
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Floating poster cover
                            Box(
                                modifier = Modifier
                                    .width(112.dp)
                                    .aspectRatio(0.70f)
                                    .shadow(16.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                            ) {
                                AsyncImage(
                                    model = anime.coverUrl,
                                    contentDescription = anime.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                                // Main Title
                                Text(
                                    text = anime.title,
                                    color = TextPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 23.sp
                                )

                                // Alternative titles
                                val alt = anime.englishTitle ?: anime.romajiTitle ?: anime.nativeTitle
                                if (!alt.isNullOrBlank() && alt != anime.title) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = alt,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Rating & Format & Status Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (anime.score > 0) {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF161726))
                                                .border(0.5.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = StarGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = String.format("%.1f", anime.score),
                                                color = TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    val isReleasing = anime.status.equals("RELEASING", ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isReleasing) Color(0xFF10B981).copy(alpha = 0.15f)
                                                else Color(0xFF6B7280).copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isReleasing) "AIRING" else anime.status,
                                            color = if (isReleasing) Color(0xFF34D399) else TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        text = anime.format,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Studio & Season/Year
                                val seasonInfo = buildString {
                                    if (!anime.season.isNullOrBlank()) append(anime.season.lowercase().replaceFirstChar { it.uppercase() })
                                    if (anime.seasonYear != null) append(" ${anime.seasonYear}")
                                    if (!anime.studio.isNullOrBlank()) {
                                        if (isNotEmpty()) append(" • ")
                                        append(anime.studio)
                                    }
                                }
                                if (seasonInfo.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = seasonInfo,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // NEXT AIRING EPISODE BADGE (if Releasing)
                // ==========================================
                val nextEp = anime.nextAiring ?: (anime.nextAiringEpisode?.let { ep ->
                    com.example.domain.model.NextAiringEpisode(
                        episode = ep,
                        airingAt = anime.nextAiringTime ?: 0L,
                        timeUntilAiring = if (anime.nextAiringTime != null) (anime.nextAiringTime - System.currentTimeMillis() / 1000).coerceAtLeast(0L) else 0L
                    )
                })

                if (nextEp != null) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F261E))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val countdown = if (nextEp.timeUntilAiring > 0) formatTimeUntil(nextEp.timeUntilAiring) else "soon"
                            Text(
                                text = "Episode ${nextEp.episode} airs in $countdown",
                                color = Color(0xFFE6FFFA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ==========================================
                // 2. PRIMARY ACTION BUTTONS (WATCH / STATUS)
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasHistory = uiState.continueWatching != null
                        val buttonText = if (hasHistory) {
                            "Continue Ep. ${uiState.continueWatching!!.episodeNumber}"
                        } else {
                            "Watch Episode 1"
                        }

                        Button(
                            onClick = {
                                onPlayEpisode(anime.id, uiState.nextEpisodeToWatch)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("details_watch_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buttonText,
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // AniList List Status selector button
                        OutlinedButton(
                            onClick = { showStatusSheet = true },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("details_list_status_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (uiState.userListStatus != null) AnimePrimary.copy(alpha = 0.15f) else DarkSurfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.userListStatus != null) AnimePrimary else DarkCardBorder
                            )
                        ) {
                            val statusLabel = LIST_STATUSES.find { it.first.equals(uiState.userListStatus, ignoreCase = true) }?.second
                                ?: if (uiState.isFavorite) "Saved" else "Add to List"
                            Text(
                                text = statusLabel,
                                color = if (uiState.userListStatus != null) AnimePrimary else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (uiState.userListStatus != null) AnimePrimary else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // 3. GENRES PILLS
                // ==========================================
                if (anime.genres.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(anime.genres) { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceVariant)
                                        .border(0.8.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 4. STREAMING EXTENSION / SOURCE SELECTOR
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                            .clickable { showSourceSheet = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("details_source_selector"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AnimePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = AnimePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = (uiState.selectedProvider?.name ?: "Authorized Provider").uppercase() + " • STREAM",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.isResolvingProvider) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        color = AnimePrimary,
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                }
                            }
                            Text(
                                text = uiState.providerResolverStatus ?: "Active provider • Tap to change",
                                color = if (uiState.isResolvingProvider) AnimePrimary else TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Select Source",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // ==========================================
                // 5. TAB ROW (Overview, Episodes, Characters & Staff, Relations, Recommendations, Links)
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    val tabs = listOf(
                        "Overview",
                        "Episodes (${uiState.episodes.size})",
                        "Characters & Staff",
                        "Relations (${anime.relations.size})",
                        "Recommendations (${anime.recommendations.size})",
                        "Links (${anime.externalLinks.size + anime.streamingLinks.size})"
                    )

                    ScrollableTabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = DarkBackground,
                        contentColor = AnimePrimary,
                        edgePadding = 20.dp,
                        indicator = { tabPositions ->
                            if (uiState.selectedTab < tabPositions.size) {
                                SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                                    color = AnimePrimary
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = uiState.selectedTab == index,
                                onClick = { viewModel.setTab(index) },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (uiState.selectedTab == index) AnimePrimary else TextSecondary,
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.testTag("details_tab_$index")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ==========================================
                // 6. TAB CONTENT
                // ==========================================
                when (uiState.selectedTab) {
                    0 -> {
                        // OVERVIEW TAB
                        item {
                            OverviewTabContent(
                                anime = anime,
                                isDescriptionExpanded = uiState.isDescriptionExpanded,
                                onToggleDescription = { viewModel.toggleDescriptionExpanded() },
                                onOpenTrailer = { url ->
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            )
                        }
                    }
                    1 -> {
                        // EPISODES TAB
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Episodes",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .clickable { viewModel.toggleEpisodeSort() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapVert,
                                        contentDescription = "Toggle Sort",
                                        tint = AnimePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (uiState.episodeSortAscending) "1 → ${uiState.episodes.size}" else "${uiState.episodes.size} → 1",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        items(
                            items = uiState.displayedEpisodes,
                            key = { it.id }
                        ) { episode ->
                            EpisodeItemCard(
                                episode = episode,
                                anime = anime,
                                onPlayClick = { onPlayEpisode(anime.id, episode.number) }
                            )
                        }
                    }
                    2 -> {
                        // CHARACTERS & STAFF TAB
                        item {
                            CharactersAndStaffTabContent(anime = anime)
                        }
                    }
                    3 -> {
                        // RELATIONS TAB
                        item {
                            RelationsTabContent(
                                relations = anime.relations,
                                onAnimeClick = { targetId -> onNavigateToAnime?.invoke(targetId) }
                            )
                        }
                    }
                    4 -> {
                        // RECOMMENDATIONS TAB
                        item {
                            RecommendationsTabContent(
                                recommendations = anime.recommendations,
                                onAnimeClick = { targetId -> onNavigateToAnime?.invoke(targetId) }
                            )
                        }
                    }
                    5 -> {
                        // LINKS TAB
                        item {
                            LinksTabContent(
                                externalLinks = anime.externalLinks,
                                streamingLinks = anime.streamingLinks,
                                onOpenUrl = { url ->
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Source Bottom Sheet
        if (showSourceSheet) {
            SelectAnimeSourceSheet(
                providers = uiState.availableProviders,
                selected = uiState.selectedProvider,
                onSelect = {
                    viewModel.selectProvider(it)
                    showSourceSheet = false
                },
                onDismiss = { showSourceSheet = false }
            )
        }

        // AniList Status Bottom Sheet
        if (showStatusSheet) {
            AniListStatusSheet(
                currentStatus = uiState.userListStatus,
                currentScore = uiState.userScore,
                onSelectStatus = { status, score ->
                    viewModel.setAniListStatus(status, score)
                    showStatusSheet = false
                },
                onDismiss = { showStatusSheet = false }
            )
        }
    }
}

@Composable
private fun OverviewTabContent(
    anime: Anime,
    isDescriptionExpanded: Boolean,
    onToggleDescription: () -> Unit,
    onOpenTrailer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Synopsis
        if (!anime.description.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
                    .animateContentSize()
            ) {
                Text(
                    text = "Synopsis",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = anime.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (anime.description.length > 140) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleDescription() }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDescriptionExpanded) "Show Less" else "Read More",
                            color = AnimePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (isDescriptionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = AnimePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Official Trailer Card (if available)
        val trailerUrl = anime.trailer?.watchUrl
        if (trailerUrl != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .clickable { onOpenTrailer(trailerUrl) }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Trailer",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Watch Video", color = AnimePrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AnimePrimary, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (anime.trailer.thumbnail != null) {
                        AsyncImage(
                            model = anime.trailer.thumbnail,
                            contentDescription = "Trailer preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // Tags Section
        if (anime.tags.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "AniList Community Tags",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(anime.tags.take(15)) { tag ->
                        TagChip(tag = tag)
                    }
                }
            }
        }

        // Detailed AniList Metadata Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Detailed Information",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            MetadataItem(label = "Format", value = anime.format)
            MetadataItem(label = "Status", value = anime.status)
            MetadataItem(label = "Episodes", value = "${anime.totalEpisodes ?: anime.currentEpisodeCount ?: "Ongoing"}")
            MetadataItem(label = "Duration", value = "${anime.episodeDuration ?: 24} minutes")
            MetadataItem(
                label = "Season",
                value = buildString {
                    if (!anime.season.isNullOrBlank()) append(anime.season)
                    if (anime.seasonYear != null) append(" ${anime.seasonYear}")
                }.ifBlank { "Unknown" }
            )
            if (!anime.studio.isNullOrBlank()) {
                MetadataItem(label = "Main Studio", value = anime.studio)
            }
            if (anime.studios.isNotEmpty()) {
                MetadataItem(label = "All Studios", value = anime.studios.joinToString(", "))
            }
            if (anime.producers.isNotEmpty()) {
                MetadataItem(label = "Producers", value = anime.producers.joinToString(", "))
            }
            if (!anime.source.isNullOrBlank()) {
                MetadataItem(label = "Source Material", value = anime.source.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
            }
            if (!anime.countryOfOrigin.isNullOrBlank()) {
                MetadataItem(label = "Origin Country", value = if (anime.countryOfOrigin == "JP") "Japan (JP)" else anime.countryOfOrigin)
            }
            val startStr = anime.startDate?.formatted()
            if (startStr != null) {
                MetadataItem(label = "Start Date", value = startStr)
            }
            val endStr = anime.endDate?.formatted()
            if (endStr != null) {
                MetadataItem(label = "End Date", value = endStr)
            }
            if (anime.score > 0.0) {
                MetadataItem(label = "Average Score", value = "${anime.score} / 10.0")
            }
            if (anime.popularity != null && anime.popularity > 0) {
                MetadataItem(label = "Popularity", value = "%,d members".format(anime.popularity))
            }
            if (anime.favourites != null && anime.favourites > 0) {
                MetadataItem(label = "Favourites", value = "%,d users".format(anime.favourites))
            }
        }
    }
}

@Composable
private fun TagChip(tag: AnimeTag) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (tag.isMediaSpoiler) Color(0xFF3B1E1E) else DarkSurfaceVariant)
            .border(0.5.dp, if (tag.isMediaSpoiler) Color.Red.copy(alpha = 0.5f) else DarkCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tag.name,
                color = if (tag.isMediaSpoiler) Color(0xFFFF8B8B) else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            if (tag.rank != null && tag.rank > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${tag.rank}%",
                    color = AnimePrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CharactersAndStaffTabContent(anime: Anime) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Characters
        Text(
            text = "Characters & Voice Actors (${anime.characters.size})",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        if (anime.characters.isEmpty()) {
            Text("No character information available.", color = TextMuted, fontSize = 12.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                anime.characters.forEach { character ->
                    CharacterCard(character = character)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Staff
        Text(
            text = "Production Staff (${anime.staff.size})",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        if (anime.staff.isEmpty()) {
            Text("No staff information available.", color = TextMuted, fontSize = 12.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                anime.staff.forEach { staff ->
                    StaffCard(staff = staff)
                }
            }
        }
    }
}

@Composable
private fun CharacterCard(character: AnimeCharacter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(0.6.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Character info
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
            ) {
                if (character.imageUrl != null) {
                    AsyncImage(
                        model = character.imageUrl,
                        contentDescription = character.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = character.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = character.role.lowercase().replaceFirstChar { it.uppercase() },
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Voice Actor info (if available)
        if (!character.voiceActorName.isNullOrBlank()) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = character.voiceActorName,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = character.voiceActorLanguage ?: "Japanese",
                        color = AnimePrimary,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                ) {
                    if (character.voiceActorImageUrl != null) {
                        AsyncImage(
                            model = character.voiceActorImageUrl,
                            contentDescription = character.voiceActorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffCard(staff: AnimeStaff) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(0.6.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceVariant)
        ) {
            if (staff.imageUrl != null) {
                AsyncImage(
                    model = staff.imageUrl,
                    contentDescription = staff.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = staff.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = staff.role,
                color = AnimePrimary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RelationsTabContent(
    relations: List<AnimeRelation>,
    onAnimeClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Related Anime & Adaptations",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        if (relations.isEmpty()) {
            Text("No relations found on AniList.", color = TextMuted, fontSize = 12.sp)
        } else {
            relations.forEach { relation ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .border(0.6.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .clickable { onAnimeClick(relation.animeId) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .aspectRatio(0.70f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                    ) {
                        if (relation.coverUrl != null) {
                            AsyncImage(
                                model = relation.coverUrl,
                                contentDescription = relation.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AnimePrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = relation.relationType.replace("_", " "),
                                color = AnimePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = relation.title,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${relation.format ?: "TV"} • ${relation.status ?: "FINISHED"}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationsTabContent(
    recommendations: List<AnimeRecommendation>,
    onAnimeClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Community Recommendations",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        if (recommendations.isEmpty()) {
            Text("No recommendations recorded yet.", color = TextMuted, fontSize = 12.sp)
        } else {
            recommendations.forEach { rec ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .border(0.6.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .clickable { onAnimeClick(rec.animeId) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .aspectRatio(0.70f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                    ) {
                        if (rec.coverUrl != null) {
                            AsyncImage(
                                model = rec.coverUrl,
                                contentDescription = rec.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rec.title,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${rec.format ?: "Anime"} • ${(rec.score ?: 0.0)}★",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "👍 ${rec.rating} user recommendations",
                            color = AnimePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinksTabContent(
    externalLinks: List<AnimeExternalLink>,
    streamingLinks: List<AnimeExternalLink>,
    onOpenUrl: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Official Streaming Services
        if (streamingLinks.isNotEmpty()) {
            Text(
                text = "Official Streaming Services",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                streamingLinks.forEach { link ->
                    LinkRowItem(link = link, onOpenUrl = onOpenUrl)
                }
            }
        }

        // External & Social Links
        if (externalLinks.isNotEmpty()) {
            Text(
                text = "Official External Links",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                externalLinks.forEach { link ->
                    LinkRowItem(link = link, onOpenUrl = onOpenUrl)
                }
            }
        }

        if (streamingLinks.isEmpty() && externalLinks.isEmpty()) {
            Text("No official external links available on AniList.", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LinkRowItem(
    link: AnimeExternalLink,
    onOpenUrl: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(0.6.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .clickable { onOpenUrl(link.url) }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AnimePrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Tv,
                    contentDescription = null,
                    tint = AnimePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = link.site,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (link.type != null) {
                    Text(
                        text = link.type,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open Link",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun EpisodeItemCard(
    episode: Episode,
    anime: Anime,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(0.6.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .clickable { onPlayClick() }
            .padding(10.dp)
            .testTag("episode_item_${episode.number}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 92.dp, height = 62.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
        ) {
            AsyncImage(
                model = episode.thumbnail ?: anime.coverUrl,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AnimePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play Episode",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Episode ${episode.number}",
                    color = AnimePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${episode.durationSeconds / 60}m",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = episode.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (episode.watchedProgressMs > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val totalMs = episode.durationSeconds * 1000L
                val prog = (episode.watchedProgressMs.toFloat() / totalMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { prog },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = AnimePrimary,
                    trackColor = DarkCardBorder
                )
            }
        }
    }
}

@Composable
private fun MetadataItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AniListStatusSheet(
    currentStatus: String?,
    currentScore: Double?,
    onSelectStatus: (String, Double?) -> Unit,
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
                text = "Update AniList Status",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Synchronize your watching progress directly to your AniList account",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            LIST_STATUSES.forEach { (statusKey, statusLabel) ->
                val isSelected = currentStatus.equals(statusKey, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceVariant else Color(0xFF141522))
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) AnimePrimary else DarkCardBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectStatus(statusKey, currentScore) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusLabel,
                        color = if (isSelected) AnimePrimary else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = AnimePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectAnimeSourceSheet(
    providers: List<ProviderInfo>,
    selected: ProviderInfo?,
    onSelect: (ProviderInfo) -> Unit,
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
                text = "Select Streaming Provider",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose which authorized provider resolves streams for this anime",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(providers) { provider ->
                    val isSelected = provider.id == selected?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) DarkSurfaceVariant else Color(0xFF141522))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) AnimePrimary else DarkCardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(provider) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = provider.name,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AnimePrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "STREAM",
                                        color = AnimePrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = provider.description ?: provider.baseUrl,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
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
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
