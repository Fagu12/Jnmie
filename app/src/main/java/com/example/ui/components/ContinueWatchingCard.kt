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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.core.model.PlaybackProgress
import com.example.ui.theme.AnimeAccent
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.AnimeSecondary
import com.example.ui.theme.AppTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ContinueWatchingCard(
    progress: PlaybackProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: String = "Frosted Glass" // "Regular", "Frosted Glass", "Bootiful"
) {
    val isBootiful = style.equals("Bootiful", ignoreCase = true)
    val isFrosted = style.equals("Frosted Glass", ignoreCase = true)
    val isRegular = !isBootiful && !isFrosted

    val cornerRadius = when {
        isBootiful -> 26.dp
        isFrosted -> 18.dp
        else -> 12.dp // Regular
    }

    val remainingMs = (progress.durationMs - progress.currentPositionMs).coerceAtLeast(0L)
    val remainingMinutes = remainingMs / (1000 * 60)
    val remainingText = if (remainingMinutes > 0) "$remainingMinutes min left" else "Almost done"

    Box(
        modifier = modifier
            .testTag("continue_watching_${progress.animeId}")
            .shadow(
                elevation = if (isBootiful) 14.dp else if (isFrosted) 10.dp else 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = if (isBootiful) AnimePrimary.copy(alpha = 0.4f) else Color.Black
            )
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                when {
                    isBootiful -> Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(AppTheme.colors.surfaceVariant, AppTheme.colors.surface)
                            )
                        )
                        .border(
                            1.6.dp,
                            Brush.horizontalGradient(listOf(AnimePrimary, AnimeAccent, AnimeSecondary)),
                            RoundedCornerShape(cornerRadius)
                        )
                    isFrosted -> Modifier
                        .background(AppTheme.colors.surfaceVariant.copy(alpha = 0.70f))
                        .border(
                            1.dp,
                            AnimePrimary.copy(alpha = 0.35f),
                            RoundedCornerShape(cornerRadius)
                        )
                    else -> Modifier
                        .background(AppTheme.colors.surface)
                        .border(
                            1.dp,
                            AppTheme.colors.cardBorder,
                            RoundedCornerShape(cornerRadius)
                        )
                }
            )
            .clickable { onClick() }
            .padding(if (isBootiful) 14.dp else 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with Play overlay
            Box(
                modifier = Modifier
                    .size(width = if (isBootiful) 92.dp else 84.dp, height = if (isBootiful) 80.dp else 74.dp)
                    .clip(RoundedCornerShape(if (isBootiful) 18.dp else 12.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = progress.coverUrl,
                    contentDescription = progress.animeTitle,
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
                            .size(if (isBootiful) 34.dp else 28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBootiful) Brush.linearGradient(listOf(AnimePrimary, AnimeAccent))
                                else Brush.linearGradient(listOf(AnimePrimary, AnimePrimary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(if (isBootiful) 20.dp else 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Episode pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isBootiful) AnimeAccent.copy(alpha = 0.25f)
                                else AnimePrimary.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Episode ${progress.episodeNumber}",
                            color = if (isBootiful) AnimeAccent else AnimePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isBootiful) {
                        Text(
                            text = remainingText,
                            color = AnimePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = progress.episodeTitle,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = progress.animeTitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress line
                LinearProgressIndicator(
                    progress = { progress.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isBootiful) 5.dp else 4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isBootiful) AnimeAccent else AnimePrimary,
                    trackColor = AppTheme.colors.cardBorder
                )

                if (!isBootiful) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = remainingText,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
