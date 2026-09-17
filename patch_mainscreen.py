import sys

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

import_statement = "import com.example.ui.screens.settings.SubtitleSettingsScreen\n"

if import_statement not in content:
    content = content.replace("import com.example.ui.screens.settings.SettingsScreen", "import com.example.ui.screens.settings.SettingsScreen\nimport com.example.ui.screens.settings.SubtitleSettingsScreen")

old_settings_composable = """                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToExtensions = {
                        navController.navigate(NavDestinations.EXTENSIONS)
                    }
                )
            }"""
            
new_settings_composable = """                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToExtensions = {
                        navController.navigate(NavDestinations.EXTENSIONS)
                    },
                    onNavigateToSubtitleSettings = {
                        navController.navigate(NavDestinations.SUBTITLE_SETTINGS)
                    }
                )
            }

            composable(NavDestinations.SUBTITLE_SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        container.settingsRepository,
                        container.aniListRepository,
                        container.playbackRepository,
                        container.extensionRepository
                    )
                )
                SubtitleSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }"""

content = content.replace(old_settings_composable, new_settings_composable)

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
