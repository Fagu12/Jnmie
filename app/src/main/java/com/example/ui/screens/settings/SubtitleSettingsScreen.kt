package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import androidx.compose.ui.geometry.Offset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings ?: return
    
    val fontFamilies = listOf("Default", "Sans Serif", "Serif", "Monospace")
    val fontSizes = listOf("Small", "Medium", "Large", "Extra Large")
    val fontWeights = listOf("Normal", "Bold")
    val textColors = listOf("White", "Yellow", "Cyan", "Green", "Magenta", "Red", "Blue")
    val backgroundStyles = listOf("None", "Semi-transparent", "Solid")
    val outlineStyles = listOf("None", "Outline", "Shadow")
    val positions = listOf("Bottom", "Slightly above bottom")
    
    val textStyle = remember(settings) {
        val family = when (settings.subFontFamily) {
            "Sans Serif" -> FontFamily.SansSerif
            "Serif" -> FontFamily.Serif
            "Monospace" -> FontFamily.Monospace
            else -> FontFamily.Default
        }
        val size = when (settings.subFontSize) {
            "Small" -> 12.sp
            "Large" -> 20.sp
            "Extra Large" -> 24.sp
            else -> 16.sp
        }
        val weight = if (settings.subFontWeight == "Bold") FontWeight.Bold else FontWeight.Normal
        val color = when (settings.subTextColor) {
            "Yellow" -> Color.Yellow
            "Cyan" -> Color.Cyan
            "Green" -> Color.Green
            "Magenta" -> Color.Magenta
            "Red" -> Color.Red
            "Blue" -> Color.Blue
            else -> Color.White
        }
        val shadow = when (settings.subOutlineStyle) {
            "Shadow" -> Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
            "Outline" -> Shadow(color = Color.Black, offset = Offset(0f, 0f), blurRadius = 8f) // simplistic outline
            else -> null
        }
        TextStyle(
            fontFamily = family,
            fontSize = size,
            fontWeight = weight,
            color = color,
            shadow = shadow
        )
    }
    
    val bgColor = when (settings.subBackgroundStyle) {
        "Semi-transparent" -> Color(0x80000000)
        "Solid" -> Color.Black
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Subtitle Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Preview Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(bgColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "This is a preview of the subtitle styling.",
                    style = textStyle
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Subtitles by Default", color = TextPrimary, fontSize = 16.sp)
                    Switch(
                        checked = settings.subEnabled,
                        onCheckedChange = { viewModel.setSubEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AnimePrimary, checkedTrackColor = AnimePrimary.copy(alpha = 0.5f))
                    )
                }
            }
            
            item { SubtitleDropdown("Language", settings.subLanguage, listOf("English", "Spanish", "French", "German", "Japanese"), viewModel::setSubLanguage) }
            item { SubtitleDropdown("Font Family", settings.subFontFamily, fontFamilies, viewModel::setSubFontFamily) }
            item { SubtitleDropdown("Font Size", settings.subFontSize, fontSizes, viewModel::setSubFontSize) }
            item { SubtitleDropdown("Font Weight", settings.subFontWeight, fontWeights, viewModel::setSubFontWeight) }
            item { SubtitleDropdown("Text Color", settings.subTextColor, textColors, viewModel::setSubTextColor) }
            item { SubtitleDropdown("Background Style", settings.subBackgroundStyle, backgroundStyles, viewModel::setSubBackgroundStyle) }
            item { SubtitleDropdown("Outline Style", settings.subOutlineStyle, outlineStyles, viewModel::setSubOutlineStyle) }
            item { SubtitleDropdown("Position", settings.subPosition, positions, viewModel::setSubPosition) }
        }
    }
}

@Composable
fun SubtitleDropdown(
    title: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .clickable { expanded = true }
                .padding(16.dp)
        ) {
            Text(currentValue, color = TextPrimary, fontSize = 16.sp)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, color = TextPrimary) },
                        onClick = {
                            onSelect(opt)
                            expanded = false
                        },
                        trailingIcon = if (opt == currentValue) {
                            { Icon(Icons.Filled.Check, contentDescription = null, tint = AnimePrimary) }
                        } else null
                    )
                }
            }
        }
    }
}
