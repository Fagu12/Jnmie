import sys

with open("app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt", "r") as f:
    content = f.read()

append = """    fun setAutoSyncAniList(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSyncAniList(enabled)
        }
    }
    
    fun setSubEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepository.setSubEnabled(enabled) } }
    fun setSubLanguage(language: String) { viewModelScope.launch { settingsRepository.setSubLanguage(language) } }
    fun setSubFontFamily(family: String) { viewModelScope.launch { settingsRepository.setSubFontFamily(family) } }
    fun setSubFontSize(size: String) { viewModelScope.launch { settingsRepository.setSubFontSize(size) } }
    fun setSubFontWeight(weight: String) { viewModelScope.launch { settingsRepository.setSubFontWeight(weight) } }
    fun setSubTextColor(color: String) { viewModelScope.launch { settingsRepository.setSubTextColor(color) } }
    fun setSubBackgroundStyle(style: String) { viewModelScope.launch { settingsRepository.setSubBackgroundStyle(style) } }
    fun setSubBackgroundOpacity(opacity: Float) { viewModelScope.launch { settingsRepository.setSubBackgroundOpacity(opacity) } }
    fun setSubOutlineStyle(style: String) { viewModelScope.launch { settingsRepository.setSubOutlineStyle(style) } }
    fun setSubPosition(position: String) { viewModelScope.launch { settingsRepository.setSubPosition(position) } }
}"""

content = content.replace("""    fun setAutoSyncAniList(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSyncAniList(enabled)
        }
    }
}""", append)

with open("app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt", "w") as f:
    f.write(content)
