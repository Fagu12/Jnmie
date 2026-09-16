package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.AppTheme
import com.example.ui.theme.StarGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CarouselBanner(
    items: List<Anime>,
    onAnimeClick: (Anime) -> Unit,
    modifier: Modifier = Modifier,
    carouselStyle: String = "Classic"
) {
    if (items.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentAnime = items[currentIndex % items.size]
    val isPortrait = carouselStyle.equals("Portrait", ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("carousel_banner")
    ) {
        // Hero Image Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isPortrait) 300.dp else 210.dp)
                .shadow(
                    elevation = if (isPortrait) 20.dp else 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = if (isPortrait) AnimePrimary.copy(alpha = 0.35f) else Color.Black
                )
                .clip(RoundedCornerShape(24.dp))
                .background(AppTheme.colors.surfaceVariant)
                .border(
                    width = if (isPortrait) 1.5.dp else 1.dp,
                    brush = if (isPortrait) Brush.linearGradient(listOf(AnimePrimary.copy(alpha = 0.8f), AnimeAccent.copy(alpha = 0.5f)))
                    else Brush.linearGradient(listOf(AppTheme.colors.cardBorder, AppTheme.colors.cardBorder)),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable { onAnimeClick(currentAnime) }
        ) {
            // Backdrop or Cover Image
            AsyncImage(
                model = if (isPortrait && currentAnime.bannerUrl.isNullOrBlank()) currentAnime.coverUrl
                else currentAnime.bannerUrl ?: currentAnime.coverUrl,
                contentDescription = currentAnime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF090A10).copy(alpha = if (isPortrait) 0.6f else 0.4f),
                                Color(0xFF090A10).copy(alpha = 0.96f)
                            ),
                            startY = if (isPortrait) 80f else 60f
                        )
                    )
            )

            // Content Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(if (isPortrait) 18.dp else 14.dp)
            ) {
                // Genres / Format pill
                if (isPortrait) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AnimePrimary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentAnime.format.ifBlank { "ANIME" },
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        if (currentAnime.genres.isNotEmpty()) {
                            Text(
                                text = currentAnime.genres.take(2).joinToString(" • "),
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentAnime.title,
                        color = Color.White,
                        fontSize = if (isPortrait) 20.sp else 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Rating Pill
                    if (currentAnime.score > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF161726).copy(alpha = 0.9f))
                                .border(0.5.dp, AppTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = StarGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", currentAnime.score),
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (isPortrait) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AnimePrimary)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Watch Now",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch Now",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!currentAnime.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentAnime.description,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View Details",
                            tint = AnimePrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pagination Indicator Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val count = items.size.coerceAtMost(6)
            for (i in 0 until count) {
                val isSelected = i == (currentIndex % count)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) AnimePrimary else Color.White.copy(alpha = 0.25f)
                        )
                        .clickable { currentIndex = i }
                )
            }
        }
    }
}
