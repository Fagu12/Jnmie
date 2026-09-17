import sys

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "r") as f:
    content = f.read()

center_controls = """                    IconButton(
                        onClick = { 
                            val prevEp = state.episodes.find { it.number == (state.episode?.number ?: 0) - 1 }
                            if (prevEp != null) {
                                viewModel.loadAnimeAndEpisode(prevEp.number)
                            }
                        },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x44FFFFFF))
                    ) {
                        Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous Episode", tint = Color.White)
                    }
                    IconButton(
                        onClick = { viewModel.getPlayerController()?.seekBackward() },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x44FFFFFF))
                    ) {"""

content = content.replace("""                    IconButton(
                        onClick = { viewModel.getPlayerController()?.seekBackward() },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x44FFFFFF))
                    ) {""", center_controls)
                    
bottom_icons = """                        IconButton(onClick = { viewModel.setServerSheetVisible(true) }) {
                            Icon(imageVector = Icons.Filled.Dns, contentDescription = "Servers", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setSubtitleSheetVisible(true) }) {
                            Icon(imageVector = Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setAudioSheetVisible(true) }) {
                            Icon(imageVector = Icons.Filled.Audiotrack, contentDescription = "Audio", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setQualitySheetVisible(true) }) {
                            Icon(imageVector = Icons.Filled.HighQuality, contentDescription = "Quality", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setSpeedSheetVisible(true) }) {
                            Icon(imageVector = Icons.Filled.Speed, contentDescription = "Speed", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.getPlayerController()?.cycleResizeMode() }) {
                            Icon(imageVector = Icons.Filled.AspectRatio, contentDescription = "Resize", tint = Color.White)
                        }"""

content = content.replace("""                        IconButton(onClick = { viewModel.setServerSheetVisible(true) }) {
                            Icon(imageVector = Icons.Filled.Dns, contentDescription = "Servers", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setSubtitleSheetVisible(true) }) {
                            Icon(imageVector = Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setAudioSheetVisible(true) }) {
                            Icon(imageVector = Icons.Filled.Audiotrack, contentDescription = "Audio", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setQualitySheetVisible(true) }) {
                            Icon(imageVector = Icons.Filled.HighQuality, contentDescription = "Quality", tint = Color.White)
                        }""", bottom_icons)

if "import androidx.compose.material.icons.filled.SkipPrevious" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.SkipNext", "import androidx.compose.material.icons.filled.SkipNext\nimport androidx.compose.material.icons.filled.SkipPrevious")
if "import androidx.compose.material.icons.filled.Speed" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.HighQuality", "import androidx.compose.material.icons.filled.HighQuality\nimport androidx.compose.material.icons.filled.Speed\nimport androidx.compose.material.icons.filled.AspectRatio")

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "w") as f:
    f.write(content)
