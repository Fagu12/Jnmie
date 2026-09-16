package com.example.ui.screens.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToAnime: (String) -> Unit,
    onPlayEpisode: (String, Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        // Top Bar matching PDF page 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .border(1.5.dp, AnimePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.user?.avatarUrl != null) {
                        AsyncImage(
                            model = uiState.user?.avatarUrl,
                            contentDescription = uiState.user?.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Library",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "All your local collection",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Row {
                IconButton(onClick = onNavigateToSearch) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = TextPrimary
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "Filter",
                        tint = TextPrimary
                    )
                }
            }
        }

        // Segmented Tab Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryTab.entries.forEach { tab ->
                val isSelected = tab == uiState.selectedTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AnimePrimary else AppTheme.colors.surfaceVariant)
                        .clickable { viewModel.selectTab(tab) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = when (tab) {
                            LibraryTab.ANILIST -> "AniList (${uiState.aniListEntries.size})"
                            LibraryTab.ANIME -> "Anime (${uiState.libraryAnime.size})"
                            LibraryTab.HISTORY -> "History (${uiState.watchHistory.size})"
                            LibraryTab.FAVORITES -> "Favorites (${uiState.favorites.size})"
                        },
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (uiState.selectedTab) {
            LibraryTab.ANILIST -> {
                val filteredEntries = when (uiState.aniListFilterStatus) {
                    "CURRENT" -> uiState.aniListEntries.filter { it.status == "CURRENT" }
                    "COMPLETED" -> uiState.aniListEntries.filter { it.status == "COMPLETED" }
                    "PLANNING" -> uiState.aniListEntries.filter { it.status == "PLANNING" }
                    "PAUSED" -> uiState.aniListEntries.filter { it.status == "PAUSED" || it.status == "DROPPED" }
                    else -> uiState.aniListEntries
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Sub-filter status chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            null to "All",
                            "CURRENT" to "Watching",
                            "COMPLETED" to "Completed",
                            "PLANNING" to "Planning"
                        ).forEach { (statusKey, label) ->
                            val isSel = uiState.aniListFilterStatus == statusKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DarkSurfaceVariant else Color.Transparent)
                                    .border(1.dp, if (isSel) AnimePrimary else DarkCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setAniListFilterStatus(statusKey) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) AnimePrimary else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (filteredEntries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (!uiState.isAuthenticated) "AniList account not connected" else "No anime found in this category",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                if (uiState.isAuthenticated) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AnimePrimary)
                                            .clickable { viewModel.syncAniList() }
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Sync with AniList", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("anilist_grid"),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredEntries) { entry ->
                                AnimeCard(
                                    anime = entry.anime,
                                    onClick = { onNavigateToAnime(entry.anime.id) },
                                    cardStyle = uiState.cardStyle,
                                    showEpisodePill = "EP ${entry.progress}/${entry.anime.totalEpisodes ?: '?'}"
                                )
                            }
                        }
                    }
                }
            }

            LibraryTab.ANIME -> {
                // 2-Column Grid matching PDF page 3
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_grid"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.libraryAnime) { anime ->
                        AnimeCard(
                            anime = anime,
                            onClick = { onNavigateToAnime(anime.id) },
                            cardStyle = uiState.cardStyle,
                            showEpisodePill = "EPISODE ${anime.totalEpisodes ?: 12}"
                        )
                    }
                }
            }

            LibraryTab.HISTORY -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_list"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.watchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.clearHistory() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Clear History",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.watchHistory) { progress ->
                        ContinueWatchingCard(
                            progress = progress,
                            onClick = { onPlayEpisode(progress.animeId, progress.episodeNumber) },
                            style = uiState.historyCardStyle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            LibraryTab.FAVORITES -> {
                if (uiState.favorites.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No favorites yet.\nTap the bookmark on any anime!",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.favorites) { anime ->
                            AnimeCard(
                                anime = anime,
                                onClick = { onNavigateToAnime(anime.id) },
                                cardStyle = uiState.cardStyle
                            )
                        }
                    }
                }
            }
        }
    }
}
