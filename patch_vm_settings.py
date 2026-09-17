import sys

with open("app/src/main/java/com/example/ui/screens/player/PlayerViewModel.kt", "r") as f:
    content = f.read()

import_statement = "import com.example.domain.model.AppSettings\nimport com.example.core.model.Anime\n"
content = content.replace("import com.example.core.model.Anime\n", import_statement)

# Add settings: AppSettings? = null
content = content.replace(
"""    val autoSkipRecap: Boolean = true,
    val backgroundPlayback: Boolean = false
)""",
"""    val autoSkipRecap: Boolean = true,
    val backgroundPlayback: Boolean = false,
    val settings: AppSettings? = null
)"""
)

# In initializeSettingsAndData, update state with settings
content = content.replace(
"""            _state.update {
                it.copy(
                    autoSkipIntro = autoIntro,
                    autoSkipOutro = autoOutro,
                    autoSkipRecap = autoRecap,
                    backgroundPlayback = bgPlay
                )
            }""",
"""            _state.update {
                it.copy(
                    autoSkipIntro = autoIntro,
                    autoSkipOutro = autoOutro,
                    autoSkipRecap = autoRecap,
                    backgroundPlayback = bgPlay,
                    settings = settings
                )
            }"""
)

with open("app/src/main/java/com/example/ui/screens/player/PlayerViewModel.kt", "w") as f:
    f.write(content)
