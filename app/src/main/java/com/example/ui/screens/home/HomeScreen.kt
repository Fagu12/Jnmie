package com.example.ui.screens.home
import androidx.compose.material.icons.filled.CloudOff

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.auth.AniListOAuthHelper
import com.example.core.config.AppConfig
import com.example.domain.model.Anime
import com.example.domain.model.PlaybackProgress
import com.example.ui.components.AnimeCard
import com.example.ui.components.AnimeRow
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.Hero
import com.example.ui.components.ProfileBottomSheet
import com.example.ui.components.ProviderBottomSheet
import com.example.ui.theme.AnimeAccent
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.getDisplayUsername
import com.example.ui.util.getGreetingText

/**
 * Polished Home screen implementing featured Hero banner, browsing categories,
 * continue watching tray, and modular horizontal anime rows.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAnime: (String) -> Unit,
    onPlayEpisode: (String, Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExtensions: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCatalog: () -> Unit = onNavigateToSearch,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showProfileSheet by remember { mutableStateOf(false) }
    var showProviderSheet by remember { mutableStateOf(false) }
    val displayName = getDisplayUsername(uiState.user, uiState.savedUsername)

    // Filter anime lists based on category
    val isCategoryFiltered = uiState.selectedCategory != "All"
    val filterPredicate: (Anime) -> Boolean = { anime ->
        if (!isCategoryFiltered) true
        else anime.genres.any { it.equals(uiState.selectedCategory, ignoreCase = true) }
    }

    val displayTrending = remember(uiState.trendingAnime, uiState.selectedCategory) {
        if (!isCategoryFiltered) uiState.trendingAnime
        else uiState.trendingAnime.filter(filterPredicate).ifEmpty { uiState.trendingAnime }
    }

    val displayPopular = remember(uiState.popularSeasonAnime, uiState.selectedCategory) {
        if (!isCategoryFiltered) uiState.popularSeasonAnime
        else uiState.popularSeasonAnime.filter(filterPredicate).ifEmpty { uiState.popularSeasonAnime }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (uiState.isLoading && uiState.featuredAnime.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AnimePrimary)
            }
        } else if (uiState.error != null && uiState.featuredAnime.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.CloudOff, contentDescription = "Error", tint = TextMuted, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(uiState.error ?: "An error occurred", color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = { viewModel.loadData(true) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AnimePrimary)
                ) {
                    Text("Retry", color = Color.Black)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_screen_scroll"),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                // 1. TOP FLOATING BRAND BAR
                item {
                    val greeting = getGreetingText(uiState.user, uiState.savedUsername)
                    HomeTopBar(
                        userName = displayName,
                        greeting = greeting,
                        avatarUrl = uiState.user?.avatarUrl,
                        onSearchClick = onNavigateToSearch,
                        onAvatarClick = { showProfileSheet = true }
                    )
                }

                // 2. FEATURED / HERO COMPONENT
                if (uiState.featuredAnime.isNotEmpty()) {
                    item {
                        Hero(
                            featuredAnime = uiState.featuredAnime,
                            onAnimeClick = { anime -> onNavigateToAnime(anime.id) },
                            onPlayClick = { anime -> onPlayEpisode(anime.id, 1) },
                            onDetailsClick = { anime -> onNavigateToAnime(anime.id) }
                        )
                    }
                }

                // 3. BROWSING CATEGORIES HORIZONTAL ROW
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    BrowsingCategoriesRow(
                        categories = uiState.categories,
                        selectedCategory = uiState.selectedCategory,
                        onSelectCategory = { category ->
                            viewModel.selectCategory(category)
                        }
                    )
                }

                // 4. CONTINUE WATCHING SECTION
                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(
                            title = "Continue Watching",
                            actionText = "${uiState.continueWatching.size} items",
                            onActionClick = {}
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = uiState.continueWatching,
                                key = { "${it.animeId}_${it.episodeNumber}" }
                            ) { progress ->
                                ContinueWatchingCard(
                                    progress = progress,
                                    onClick = {
                                        onPlayEpisode(progress.animeId, progress.episodeNumber)
                                    },
                                    style = uiState.historyCardStyle,
                                    modifier = Modifier.width(280.dp)
                                )
                            }
                        }
                    }
                }

                // 5. CURRENTLY WATCHING ON ANILIST (if authenticated and entries exist)
                if (uiState.aniListWatchingList.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(
                            title = "Currently Watching (AniList)",
                            actionText = "${uiState.aniListWatchingList.size} series",
                            onActionClick = onNavigateToCatalog
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = uiState.aniListWatchingList,
                                key = { "anilist_${it.mediaId}" }
                            ) { entry ->
                                AnimeCard(
                                    anime = entry.anime,
                                    onClick = { onNavigateToAnime(entry.anime.id) },
                                    cardStyle = uiState.cardStyle,
                                    modifier = Modifier.width(135.dp)
                                )
                            }
                        }
                    }
                }

                // 6. AIRING SOON / UPCOMING EPISODES ROW (with next airing countdown)
                if (uiState.airingSchedule.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(26.dp))
                        AnimeRow(
                            title = "Airing Soon",
                            subtitle = "Next episodes releasing this week",
                            animes = uiState.airingSchedule,
                            onAnimeClick = { anime -> onNavigateToAnime(anime.id) },
                            onActionClick = onNavigateToCatalog,
                            cardStyle = uiState.cardStyle
                        )
                    }
                }

                // 7. TRENDING ANIME ROW
                if (displayTrending.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(26.dp))
                        AnimeRow(
                            title = if (isCategoryFiltered) "Trending in ${uiState.selectedCategory}" else "Trending on AniList",
                            subtitle = "Most popular in the community",
                            animes = displayTrending,
                            onAnimeClick = { anime -> onNavigateToAnime(anime.id) },
                            onActionClick = onNavigateToCatalog,
                            cardStyle = uiState.cardStyle
                        )
                    }
                }

                // 8. POPULAR THIS SEASON ROW
                if (displayPopular.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(26.dp))
                        AnimeRow(
                            title = "Popular This Season",
                            subtitle = "Top broadcast hits right now",
                            animes = displayPopular,
                            onAnimeClick = { anime -> onNavigateToAnime(anime.id) },
                            onActionClick = onNavigateToCatalog,
                            cardStyle = uiState.cardStyle
                        )
                    }
                }

                // 9. TOP RATED ANIME ROW
                if (uiState.topRatedAnime.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(26.dp))
                        AnimeRow(
                            title = "Top Rated of All Time",
                            subtitle = "Highest rated masterpieces on AniList",
                            animes = uiState.topRatedAnime,
                            onAnimeClick = { anime -> onNavigateToAnime(anime.id) },
                            onActionClick = onNavigateToCatalog,
                            cardStyle = uiState.cardStyle
                        )
                    }
                }

                // 10. UPCOMING ANIME ROW
                if (uiState.upcomingAnime.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(26.dp))
                        AnimeRow(
                            title = "Anticipated Upcoming",
                            subtitle = "Next season's most awaited titles",
                            animes = uiState.upcomingAnime,
                            onAnimeClick = { anime -> onNavigateToAnime(anime.id) },
                            onActionClick = onNavigateToCatalog,
                            cardStyle = uiState.cardStyle
                        )
                    }
                }

                // 11. FAVORITES / WATCHLIST SECTION (if user has favorites)
                if (uiState.favorites.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(26.dp))
                        AnimeRow(
                            title = "My Watchlist",
                            subtitle = "${uiState.favorites.size} saved titles",
                            animes = uiState.favorites,
                            onAnimeClick = { anime -> onNavigateToAnime(anime.id) },
                            onActionClick = onNavigateToCatalog,
                            cardStyle = uiState.cardStyle
                        )
                    }
                }

                // 9. QUICK CATALOG DISCOVERY BANNER
                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    CatalogDiscoveryBanner(
                        onExploreClick = onNavigateToCatalog,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        // Profile Bottom Sheet
        if (showProfileSheet) {
            val context = LocalContext.current
            ProfileBottomSheet(
                user = uiState.user,
                onDismiss = { showProfileSheet = false },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToExtensions = onNavigateToExtensions,
                onChangeService = { showProviderSheet = true },
                onLoginWithAniList = {
                    showProfileSheet = false
                    if (com.example.core.config.AniListAuthConfig.isPlaceholderClientId()) {
                        android.widget.Toast.makeText(context, "Developer Error: A real AniList Client ID must be configured in secrets before login.", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        AniListOAuthHelper.launchOAuthBrowser(context)
                    }
                },
                onSyncNow = {
                    viewModel.syncAniList()
                },
                onLogout = { viewModel.logout() }
            )
        }

        // Provider Bottom Sheet
        if (showProviderSheet) {
            ProviderBottomSheet(
                currentProvider = "core_anilist",
                onSelectProvider = { /* change provider */ },
                onDismiss = { showProviderSheet = false }
            )
        }
    }
}

/**
 * Top brand header bar with app branding, greeting, search quick-access, and profile avatar.
 */
@Composable
private fun HomeTopBar(
    userName: String,
    greeting: String,
    avatarUrl: String?,
    onSearchClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Logo & Brand Title + Greeting
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .testTag("home_brand_title")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AnimePrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = AppConfig.APP_NAME,
                    color = AnimePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = greeting,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Actions: Search & Profile Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Icon Button
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, CircleShape)
                    .testTag("home_search_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search Anime",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // User Avatar Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                    .clickable { onAvatarClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("home_avatar_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AnimePrimary.copy(alpha = 0.2f))
                        .border(1.dp, AnimePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = userName,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = userName,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Horizontal browsing categories row with filter chips.
 */
@Composable
private fun BrowsingCategoriesRow(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("browsing_categories_row")
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) AnimePrimary else DarkSurfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AnimePrimary else DarkCardBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelectCategory(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("category_chip_${category.lowercase()}")
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Section Header for grouping rows.
 */
@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.2).sp
        )

        if (actionText != null) {
            Row(
                modifier = Modifier.clickable { onActionClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    color = AnimePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = AnimePrimary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

/**
 * Atmospheric banner at the bottom of the feed directing users to full catalog and filters.
 */
@Composable
private fun CatalogDiscoveryBanner(
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1B182B),
                        Color(0xFF131422)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        AnimePrimary.copy(alpha = 0.5f),
                        AnimeAccent.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onExploreClick() }
            .padding(18.dp)
            .testTag("catalog_discovery_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Discover Full Catalog",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Filter by genres, seasons, studios, and scores",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AnimePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Explore",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
