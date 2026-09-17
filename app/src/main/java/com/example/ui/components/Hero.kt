package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.domain.model.Anime
import com.example.ui.theme.AnimeAccent
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.StarGold
import kotlinx.coroutines.delay

/**
 * Reusable cinematic Hero component showcasing featured anime.
 * Features dynamic backdrop banner, subtle gradient readability fades,
 * genre badges, rating indicator, synopsis, and primary CTA buttons.
 */
@Composable
fun Hero(
    featuredAnime: List<Anime>,
    onAnimeClick: (Anime) -> Unit,
    modifier: Modifier = Modifier,
    onPlayClick: ((Anime) -> Unit)? = null,
    onDetailsClick: ((Anime) -> Unit)? = null,
    autoSlide: Boolean = true
) {
    if (featuredAnime.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    val currentAnime = featuredAnime[currentIndex % featuredAnime.size]

    // Auto-advance banner every 6 seconds if autoSlide enabled
    if (autoSlide && featuredAnime.size > 1) {
        LaunchedEffect(currentIndex) {
            delay(6000L)
            currentIndex = (currentIndex + 1) % featuredAnime.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(390.dp)
            .testTag("hero_component")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onAnimeClick(currentAnime)
            }
    ) {
        // Animated Backdrop Image
        AnimatedContent(
            targetState = currentAnime,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "hero_crossfade"
        ) { anime ->
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = anime.bannerUrl?.takeIf { it.isNotBlank() } ?: anime.coverUrl,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // High-contrast atmospheric gradients: top dark fade for status bar, bottom deep dark fade for text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent,
                                    Color(0xFF0D0E15).copy(alpha = 0.85f),
                                    Color(0xFF090A10)
                                ),
                                startY = 0f,
                                endY = 1100f
                            )
                        )
                )

                // Subtle side vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF090A10).copy(alpha = 0.7f),
                                    Color.Transparent,
                                    Color(0xFF090A10).copy(alpha = 0.4f)
                                )
                            )
                        )
                )
            }
        }

        // Hero Content Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Badges Row: "FEATURED" pill, Score badge, Format/Season
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                // Featured Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AnimePrimary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "FEATURED",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Format & Year tag
                if (!currentAnime.format.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${currentAnime.format} • ${currentAnime.seasonYear ?: 2024}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Score Badge
                if (currentAnime.score > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(0.5.dp, StarGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = StarGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.1f", currentAnime.score),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Anime Title
            Text(
                text = currentAnime.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 30.sp,
                modifier = Modifier.testTag("hero_title")
            )

            // Genres bullet list
            if (currentAnime.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentAnime.genres.take(3).joinToString("  •  "),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Synopsis description
            if (!currentAnime.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentAnime.description.replace(Regex("<.*?>"), ""),
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons & Pager Indicators Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary Play / Watch CTA
                    Button(
                        onClick = {
                            if (onPlayClick != null) onPlayClick(currentAnime)
                            else onAnimeClick(currentAnime)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AnimePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("hero_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Watch Now",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Secondary Details / Info CTA
                    OutlinedButton(
                        onClick = {
                            if (onDetailsClick != null) onDetailsClick(currentAnime)
                            else onAnimeClick(currentAnime)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f)))
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("hero_details_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Details",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Details",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Carousel Indicator Dots
                if (featuredAnime.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        featuredAnime.forEachIndexed { index, _ ->
                            val isSelected = index == currentIndex % featuredAnime.size
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = if (isSelected) 16.dp else 6.dp,
                                        height = 6.dp
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) AnimePrimary else Color.White.copy(alpha = 0.3f)
                                    )
                                    .clickable { currentIndex = index }
                            )
                        }
                    }
                }
            }
        }
    }
}
