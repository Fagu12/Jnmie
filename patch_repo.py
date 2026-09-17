import sys

with open("app/src/main/java/com/example/domain/repository/Repositories.kt", "r") as f:
    content = f.read()

repo_append = """    suspend fun setAutoSyncAniList(enabled: Boolean)
    suspend fun resetSettings()
    suspend fun setSubEnabled(enabled: Boolean)
    suspend fun setSubLanguage(language: String)
    suspend fun setSubFontFamily(family: String)
    suspend fun setSubFontSize(size: String)
    suspend fun setSubFontWeight(weight: String)
    suspend fun setSubTextColor(color: String)
    suspend fun setSubBackgroundStyle(style: String)
    suspend fun setSubBackgroundOpacity(opacity: Float)
    suspend fun setSubOutlineStyle(style: String)
    suspend fun setSubPosition(position: String)
}"""

content = content.replace("""    suspend fun setAutoSyncAniList(enabled: Boolean)
    suspend fun resetSettings()
}""", repo_append)

with open("app/src/main/java/com/example/domain/repository/Repositories.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/data/repository/SettingsRepositoryImpl.kt", "r") as f:
    content2 = f.read()
    
impl_append = """    override suspend fun resetSettings() {
        preferencesDataStore.resetSettings()
    }
    
    override suspend fun setSubEnabled(enabled: Boolean) {
        preferencesDataStore.updateSettings { it.copy(subEnabled = enabled) }
    }
    override suspend fun setSubLanguage(language: String) {
        preferencesDataStore.updateSettings { it.copy(subLanguage = language) }
    }
    override suspend fun setSubFontFamily(family: String) {
        preferencesDataStore.updateSettings { it.copy(subFontFamily = family) }
    }
    override suspend fun setSubFontSize(size: String) {
        preferencesDataStore.updateSettings { it.copy(subFontSize = size) }
    }
    override suspend fun setSubFontWeight(weight: String) {
        preferencesDataStore.updateSettings { it.copy(subFontWeight = weight) }
    }
    override suspend fun setSubTextColor(color: String) {
        preferencesDataStore.updateSettings { it.copy(subTextColor = color) }
    }
    override suspend fun setSubBackgroundStyle(style: String) {
        preferencesDataStore.updateSettings { it.copy(subBackgroundStyle = style) }
    }
    override suspend fun setSubBackgroundOpacity(opacity: Float) {
        preferencesDataStore.updateSettings { it.copy(subBackgroundOpacity = opacity) }
    }
    override suspend fun setSubOutlineStyle(style: String) {
        preferencesDataStore.updateSettings { it.copy(subOutlineStyle = style) }
    }
    override suspend fun setSubPosition(position: String) {
        preferencesDataStore.updateSettings { it.copy(subPosition = position) }
    }
}"""

content2 = content2.replace("""    override suspend fun resetSettings() {
        preferencesDataStore.resetSettings()
    }
}""", impl_append)

with open("app/src/main/java/com/example/data/repository/SettingsRepositoryImpl.kt", "w") as f:
    f.write(content2)
