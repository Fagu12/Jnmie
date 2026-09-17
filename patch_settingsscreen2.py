import sys

with open("app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt", "r") as f:
    content = f.read()

old_signature = """@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToExtensions: () -> Unit,
    modifier: Modifier = Modifier
) {"""

new_signature = """@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToExtensions: () -> Unit,
    onNavigateToSubtitleSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {"""

content = content.replace(old_signature, new_signature)

old_player_section = """                SettingsSectionHeader("Player", Icons.Filled.PlayCircle)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                ) {"""
                
new_player_section = """                SettingsSectionHeader("Player", Icons.Filled.PlayCircle)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                ) {
                    Column {
                        SettingsActionRow(
                            title = "Subtitle Settings",
                            icon = Icons.Filled.Subtitles,
                            onClick = onNavigateToSubtitleSettings
                        )
                        HorizontalDivider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 16.dp))"""

if "import androidx.compose.material.icons.filled.Subtitles" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Settings", "import androidx.compose.material.icons.filled.Settings\nimport androidx.compose.material.icons.filled.Subtitles")

content = content.replace(old_player_section, new_player_section)

with open("app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt", "w") as f:
    f.write(content)
