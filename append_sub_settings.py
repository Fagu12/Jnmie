import sys

with open("app/src/main/java/com/example/domain/model/Models.kt", "r") as f:
    content = f.read()

# find data class AppSettings
old_appsettings = """    val autoSyncAniList: Boolean = true,
    val lastSyncedTimestamp: Long = 0L
)"""

new_appsettings = """    val autoSyncAniList: Boolean = true,
    val lastSyncedTimestamp: Long = 0L,
    val subEnabled: Boolean = true,
    val subLanguage: String = "English",
    val subFontFamily: String = "Default",
    val subFontSize: String = "Medium",
    val subFontWeight: String = "Normal",
    val subTextColor: String = "White",
    val subBackgroundStyle: String = "None",
    val subBackgroundOpacity: Float = 0.5f,
    val subOutlineStyle: String = "Outline",
    val subPosition: String = "Bottom"
)"""

content = content.replace(old_appsettings, new_appsettings)

with open("app/src/main/java/com/example/domain/model/Models.kt", "w") as f:
    f.write(content)
