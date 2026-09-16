package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Anime
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reusable AnimeRow component for displaying horizontal scrolling anime lists.
 * Features customizable section header, "See All" action trigger, and smooth LazyRow of AnimeCards.
 */
@Composable
fun AnimeRow(
    title: String,
    animes: List<Anime>,
    onAnimeClick: (Anime) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = "See all",
    onActionClick: (() -> Unit)? = null,
    cardStyle: String = "Saikou",
    cardWidth: androidx.compose.ui.unit.Dp = 135.dp
) {
    if (animes.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("anime_row_${title.lowercase().replace(" ", "_")}")
    ) {
        // Section Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            if (onActionClick != null && !actionText.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onActionClick() }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .testTag("action_${title.lowercase().replace(" ", "_")}")
                ) {
                    Text(
                        text = actionText,
                        color = AnimePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = actionText,
                        tint = AnimePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Anime Cards Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = animes,
                key = { it.id }
            ) { anime ->
                AnimeCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime) },
                    cardStyle = cardStyle,
                    modifier = Modifier.width(cardWidth)
                )
            }
        }
    }
}
