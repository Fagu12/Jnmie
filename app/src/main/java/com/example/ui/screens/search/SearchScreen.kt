package com.example.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimeCard
import com.example.ui.components.AnimeListItem
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToAnime: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ==========================================
        // Top Search Bar Header
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("search_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = {
                    Text(
                        "Search anime, studios, genres...",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = AnimePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearQuery() },
                            modifier = Modifier.testTag("search_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AnimePrimary,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Grid / List View Toggle Button
            IconButton(
                onClick = { viewModel.toggleViewMode() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("search_view_toggle")
            ) {
                Icon(
                    imageVector = if (uiState.isListView) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = if (uiState.isListView) "Switch to Grid View" else "Switch to List View",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ==========================================
        // Genre Filters Bar
        // ==========================================
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.genres) { genre ->
                val isSelected = uiState.selectedGenre.equals(genre, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AnimePrimary else DarkSurfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AnimePrimary else DarkCardBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.onGenreSelect(genre) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("search_genre_chip_$genre")
                ) {
                    Text(
                        text = genre,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // ==========================================
        // Format Filters Bar (TV, Movie, OVA)
        // ==========================================
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(uiState.formats) { format ->
                val isSelected = uiState.selectedFormat.equals(format, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) AnimePrimary.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .border(
                            width = 0.8.dp,
                            color = if (isSelected) AnimePrimary else DarkCardBorder.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.onFormatSelect(format) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("search_format_chip_$format")
                ) {
                    Text(
                        text = format,
                        color = if (isSelected) AnimePrimary else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ==========================================
        // Main Content Area (States)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // 1. LOADING STATE
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .testTag("search_loading_state"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = AnimePrimary,
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Searching anime catalog...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            // 2. ERROR STATE
            else if (uiState.errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("search_error_state"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Search Error",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.errorMessage ?: "Something went wrong while searching.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("search_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Try Again",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // 3. EMPTY RESULTS STATE
            else if (uiState.results.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("search_empty_state"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (uiState.query.isNotBlank()) "No Results Found" else "No Anime Found",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (uiState.query.isNotBlank()) {
                            "We couldn't find any anime matching \"${uiState.query}\".\nTry checking the spelling or selecting another genre."
                        } else {
                            "No titles match the selected genre/format filters."
                        },
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            viewModel.clearQuery()
                            viewModel.onGenreSelect("All")
                            viewModel.onFormatSelect("All")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("search_reset_filters_button")
                    ) {
                        Text(
                            text = "Reset All Filters",
                            color = AnimePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            // 4. RESULTS DISPLAY (GRID OR LIST)
            else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header with Results Count & Search Suggestions when query is empty
                    if (uiState.query.isEmpty()) {
                        // Recent Searches section
                        if (uiState.recentSearches.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = AnimePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Recent Searches",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.onClearRecentSearches() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear All", color = TextMuted, fontSize = 11.sp)
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.recentSearches) { term ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(DarkSurfaceVariant)
                                            .clickable { viewModel.onRecentSearchClick(term) }
                                            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = term, color = TextPrimary, fontSize = 12.sp)
                                        IconButton(
                                            onClick = { viewModel.onRemoveRecentSearch(term) },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove",
                                                tint = TextMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Trending Searches section
                        if (uiState.popularTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Trending Searches",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.popularTags.take(6).forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(DarkCard)
                                            .border(0.8.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                                            .clickable { viewModel.onTagClick(tag) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Discover catalog title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Popular & Trending",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.results.size} titles",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // Results Count Header when user is searching
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Results for \"${uiState.query}\"",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.results.size} found",
                                color = AnimePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // ==========================================
                    // View Modes: List View vs Grid View
                    // ==========================================
                    if (uiState.isListView) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = uiState.results,
                                key = { it.id }
                            ) { anime ->
                                AnimeListItem(
                                    anime = anime,
                                    onClick = { onNavigateToAnime(anime.id) }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_grid"),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = uiState.results,
                                key = { it.id }
                            ) { anime ->
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
}
