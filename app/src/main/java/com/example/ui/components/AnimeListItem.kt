package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Anime
import com.example.ui.theme.AnimeAccent
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeListItem(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("anime_list_item_${anime.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster Cover Image
        Box(
            modifier = Modifier
                .width(82.dp)
                .height(115.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
        ) {
            AsyncImage(
                model = anime.coverUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // Rating badge overlay on image bottom
            if (anime.score > 0.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = String.format("%.1f", anime.score),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Content details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Format & Status pills row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!anime.format.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AnimePrimary.copy(alpha = 0.15f))
                            .border(0.5.dp, AnimePrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = anime.format.uppercase(),
                            color = AnimePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!anime.status.isNullOrBlank()) {
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
                            text = if (isReleasing) "AIRING" else "FINISHED",
                            color = if (isReleasing) Color(0xFF34D399) else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if ((anime.totalEpisodes ?: 0) > 0) {
                    Text(
                        text = "${anime.totalEpisodes} eps",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Title
            Text(
                text = anime.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            // Romaji or studio
            val subText = anime.romajiTitle ?: anime.studio
            if (!subText.isNullOrBlank() && subText != anime.title) {
                Text(
                    text = subText,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Genre tags
            if (anime.genres.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 3
                ) {
                    anime.genres.take(3).forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = genre,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Forward arrow
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}
