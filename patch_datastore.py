import sys

with open("app/src/main/java/com/example/data/local/preferences/UserPreferencesDataStore.kt", "r") as f:
    content = f.read()

keys_append = """        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        
        val SUB_ENABLED = booleanPreferencesKey("sub_enabled")
        val SUB_LANGUAGE = stringPreferencesKey("sub_language")
        val SUB_FONT_FAMILY = stringPreferencesKey("sub_font_family")
        val SUB_FONT_SIZE = stringPreferencesKey("sub_font_size")
        val SUB_FONT_WEIGHT = stringPreferencesKey("sub_font_weight")
        val SUB_TEXT_COLOR = stringPreferencesKey("sub_text_color")
        val SUB_BACKGROUND_STYLE = stringPreferencesKey("sub_background_style")
        val SUB_BACKGROUND_OPACITY = floatPreferencesKey("sub_background_opacity")
        val SUB_OUTLINE_STYLE = stringPreferencesKey("sub_outline_style")
        val SUB_POSITION = stringPreferencesKey("sub_position")
    }"""

content = content.replace('        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")\n    }', keys_append)

map_append = """            autoSyncAniList = preferences[PreferencesKeys.AUTO_SYNC_ANILIST] ?: true,
            lastSyncedTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIME] ?: 0L,
            
            subEnabled = preferences[PreferencesKeys.SUB_ENABLED] ?: true,
            subLanguage = preferences[PreferencesKeys.SUB_LANGUAGE] ?: "English",
            subFontFamily = preferences[PreferencesKeys.SUB_FONT_FAMILY] ?: "Default",
            subFontSize = preferences[PreferencesKeys.SUB_FONT_SIZE] ?: "Medium",
            subFontWeight = preferences[PreferencesKeys.SUB_FONT_WEIGHT] ?: "Normal",
            subTextColor = preferences[PreferencesKeys.SUB_TEXT_COLOR] ?: "White",
            subBackgroundStyle = preferences[PreferencesKeys.SUB_BACKGROUND_STYLE] ?: "None",
            subBackgroundOpacity = preferences[PreferencesKeys.SUB_BACKGROUND_OPACITY] ?: 0.5f,
            subOutlineStyle = preferences[PreferencesKeys.SUB_OUTLINE_STYLE] ?: "Outline",
            subPosition = preferences[PreferencesKeys.SUB_POSITION] ?: "Bottom"
        )"""

content = content.replace("""            autoSyncAniList = preferences[PreferencesKeys.AUTO_SYNC_ANILIST] ?: true,
            lastSyncedTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIME] ?: 0L
        )""", map_append)
        
update_append = """            preferences[PreferencesKeys.AUTO_SYNC_ANILIST] = newSettings.autoSyncAniList
            preferences[PreferencesKeys.LAST_SYNC_TIME] = newSettings.lastSyncedTimestamp
            
            preferences[PreferencesKeys.SUB_ENABLED] = newSettings.subEnabled
            preferences[PreferencesKeys.SUB_LANGUAGE] = newSettings.subLanguage
            preferences[PreferencesKeys.SUB_FONT_FAMILY] = newSettings.subFontFamily
            preferences[PreferencesKeys.SUB_FONT_SIZE] = newSettings.subFontSize
            preferences[PreferencesKeys.SUB_FONT_WEIGHT] = newSettings.subFontWeight
            preferences[PreferencesKeys.SUB_TEXT_COLOR] = newSettings.subTextColor
            preferences[PreferencesKeys.SUB_BACKGROUND_STYLE] = newSettings.subBackgroundStyle
            preferences[PreferencesKeys.SUB_BACKGROUND_OPACITY] = newSettings.subBackgroundOpacity
            preferences[PreferencesKeys.SUB_OUTLINE_STYLE] = newSettings.subOutlineStyle
            preferences[PreferencesKeys.SUB_POSITION] = newSettings.subPosition
        }"""
        
content = content.replace("""            preferences[PreferencesKeys.AUTO_SYNC_ANILIST] = newSettings.autoSyncAniList
            preferences[PreferencesKeys.LAST_SYNC_TIME] = newSettings.lastSyncedTimestamp
        }""", update_append)

with open("app/src/main/java/com/example/data/local/preferences/UserPreferencesDataStore.kt", "w") as f:
    f.write(content)
