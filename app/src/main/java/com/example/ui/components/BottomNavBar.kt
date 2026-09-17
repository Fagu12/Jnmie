package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.NavPillActive
import com.example.ui.theme.NavPillActiveText
import com.example.ui.theme.NavPillBackground
import com.example.ui.theme.NavPillInactiveText

enum class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    EXPLORE("Anime", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline, "nav_explore"),
    LIBRARY("Library", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_library"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
}

@Composable
fun JustAnimeBottomNav(
    currentTab: NavItem,
    onTabSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier,
    style: String = "Dynamic Pill", // "Dynamic Pill" or "Classic"
    horizontalMargin: Int = 24
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = horizontalMargin.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(32.dp))
                .background(NavPillBackground.copy(alpha = 0.94f))
                .border(1.dp, DarkCardBorder.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem.entries.forEach { item ->
                val isSelected = item == currentTab
                val interactionSource = remember { MutableInteractionSource() }

                if (style == "Dynamic Pill") {
                    // Dynamic pill expands active tab with label, others show icon
                    val bgAlpha by animateColorAsState(
                        targetValue = if (isSelected) NavPillActive else Color.Transparent,
                        animationSpec = spring(),
                        label = "pill_bg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) NavPillActiveText else NavPillInactiveText,
                        label = "pill_content"
                    )

                    Row(
                        modifier = Modifier
                            .testTag(item.tag)
                            .clip(CircleShape)
                            .background(bgAlpha)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onTabSelected(item) }
                            .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        AnimatedVisibility(visible = isSelected) {
                            Row {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.title,
                                    color = contentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    // Classic layout: icon + label stacked
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) AnimePrimary else NavPillInactiveText,
                        label = "classic_content"
                    )
                    Column(
                        modifier = Modifier
                            .testTag(item.tag)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onTabSelected(item) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.title,
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
