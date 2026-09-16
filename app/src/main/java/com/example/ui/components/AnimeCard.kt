package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.model.Anime
import com.example.ui.theme.AnimeAccent
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.AnimeSecondary
import com.example.ui.theme.AppTheme
import com.example.ui.theme.StarGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AnimeCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardStyle: String = "Saikou",
    showEpisodePill: String? = null
) {
    val isMinimal = cardStyle.equals("Minimal", ignoreCase = true) || cardStyle.startsWith("Minimal", ignoreCase = true)
    val isExotic = cardStyle.equals("Exotic", ignoreCase = true)
    val isDefault = cardStyle.equals("Default", ignoreCase = true)
    val isSaikou = !isMinimal && !isExotic && !isDefault

    val cornerRadius = when {
        isDefault -> 8.dp
        isExotic -> 18.dp
        isMinimal -> 14.dp
        else -> 16.dp // Saikou
    }

    Column(
        modifier = modifier
            .testTag("anime_card_${anime.id}")
            .clickable { onClick() }
    ) {
        // Poster Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .shadow(
                    elevation = if (isExotic) 12.dp else 6.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    spotColor = if (isExotic) AnimePrimary.copy(alpha = 0.5f) else Color.Black
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(AppTheme.colors.surfaceVariant)
                .then(
                    when {
                        isExotic -> Modifier.border(
                            width = 1.8.dp,
                            brush = Brush.linearGradient(listOf(AnimePrimary, AnimeAccent, AnimeSecondary)),
                            shape = RoundedCornerShape(cornerRadius)
                        )
                        isDefault -> Modifier.border(
                            width = 1.dp,
                            color = AppTheme.colors.cardBorder,
                            shape = RoundedCornerShape(cornerRadius)
                        )
                        else -> Modifier.border(
                            width = 0.6.dp,
                            color = AppTheme.colors.cardBorder.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(cornerRadius)
                        )
                    }
                )
        ) {
            // Poster Image
            AsyncImage(
                model = anime.coverUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Minimal Style: full overlay gradient with embedded title & details
            if (isMinimal) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.92f)
                                ),
                                startY = 120f
                            )
                        ),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = anime.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = anime.format.ifBlank { anime.status },
                                color = AnimePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (anime.score > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = StarGold,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = String.format("%.1f", anime.score),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Default Style: top-left format badge
            if (isDefault && anime.format.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(0.5.dp, AppTheme.colors.cardBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = anime.format,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Exotic Style: Neon glow badge at top-left
            if (isExotic && anime.score > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(listOf(AnimePrimary, AnimeAccent))
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = Color.Black,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format("%.1f", anime.score),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Non-Minimal, Non-Exotic Rating Badge (bottom-end)
            if (!isMinimal && !isExotic && anime.score > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(if (isDefault) 4.dp else 10.dp))
                        .background(Color(0xFF13141F).copy(alpha = 0.88f))
                        .border(0.5.dp, AppTheme.colors.cardBorder, RoundedCornerShape(if (isDefault) 4.dp else 10.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = StarGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format("%.1f", anime.score),
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Title and Subtitle block (only for non-minimal styles)
        if (!isMinimal) {
            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isExotic) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AnimePrimary)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = anime.title,
                    color = TextPrimary,
                    fontSize = if (isDefault) 12.sp else 13.sp,
                    fontWeight = if (isExotic) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Subtitle or episode pill
            if (showEpisodePill != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(if (isDefault) 4.dp else 8.dp))
                        .background(AnimePrimary.copy(alpha = 0.25f))
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = showEpisodePill,
                        color = AnimePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (anime.format.isNotBlank() || !anime.studio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = anime.studio ?: anime.format,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
