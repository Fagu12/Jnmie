package com.example.domain.repository

import com.example.domain.model.AniListMediaListEntry
import com.example.domain.model.AniListUser
import com.example.domain.model.Anime
import com.example.domain.model.AppSettings
import com.example.domain.model.ConflictStrategy
import com.example.domain.model.Episode
import com.example.domain.model.LanguagePreference
import com.example.domain.model.PlaybackProgress
import com.example.domain.model.ProviderInfo
import com.example.domain.model.SyncReport
import com.example.domain.model.SyncStatus
import com.example.domain.model.VideoSource
import com.example.domain.provider.AnimeProvider
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {
    suspend fun getTrendingAnime(page: Int = 1): Result<List<Anime>>
    suspend fun getPopularThisSeason(page: Int = 1): Result<List<Anime>>
    suspend fun getUpcomingAnime(page: Int = 1): Result<List<Anime>>
    suspend fun getTopRated(page: Int = 1): Result<List<Anime>>
    suspend fun getRecentlyUpdated(page: Int = 1): Result<List<Anime>>
    suspend fun getAiringSchedule(page: Int = 1): Result<List<Anime>>
    suspend fun searchAnime(
        query: String,
        genre: String? = null,
        tag: String? = null,
        season: String? = null,
        seasonYear: Int? = null,
        format: String? = null,
        status: String? = null,
        sort: String? = null,
        page: Int = 1
    ): Result<List<Anime>>
    suspend fun getAnimeDetails(animeId: String): Result<Anime>
    suspend fun getEpisodes(animeId: String): Result<List<Episode>>
    fun getFavoriteAnimeList(): Flow<List<Anime>>
    fun isFavoriteFlow(animeId: String): Flow<Boolean>
    suspend fun isFavorite(animeId: String): Boolean
    suspend fun toggleFavorite(anime: Anime): Boolean
    suspend fun getCachedAnime(animeId: String): Anime?
}

interface AniListRepository {
    val isAuthenticated: Flow<Boolean>
    val currentUser: Flow<AniListUser?>
    val savedUsername: Flow<String?>
    val isSecureStorageEnabled: Boolean
    val syncStatus: Flow<SyncStatus>
    val pendingSyncCount: Flow<Int>
    val cachedUserList: Flow<List<AniListMediaListEntry>>
    suspend fun authenticateWithToken(token: String): Result<AniListUser>
    suspend fun handleOAuthCallback(uriString: String): Result<AniListUser>
    suspend fun logout()
    suspend fun syncUserLists(strategy: ConflictStrategy = ConflictStrategy.HIGHEST_PROGRESS): Result<SyncReport>
    suspend fun getUserAnimeList(status: String? = null): Result<List<AniListMediaListEntry>>
    suspend fun updateEpisodeProgress(animeId: String, episodeNumber: Int, completed: Boolean): Result<Unit>
    suspend fun setAnimeListStatus(animeId: String, status: String, score: Double? = null, progress: Int? = null): Result<Unit>
    suspend fun toggleFavorite(animeId: String): Result<Boolean>
    suspend fun retryPendingSync(): Result<Int>
    suspend fun clearPendingSync()
}

interface SearchRepository {
    suspend fun searchAnime(
        query: String,
        genre: String? = null,
        tag: String? = null,
        season: String? = null,
        seasonYear: Int? = null,
        format: String? = null,
        status: String? = null,
        sort: String? = null,
        page: Int = 1
    ): Result<List<Anime>>
    fun getPopularSearchTags(): List<String>
    fun getRecentSearches(): Flow<List<String>>
    suspend fun saveRecentSearch(query: String)
    suspend fun removeRecentSearch(query: String)
    suspend fun clearRecentSearches()
}

typealias ProviderManager = com.example.domain.provider.ProviderManager
typealias SourceResolver = com.example.domain.provider.SourceResolver

interface PlaybackRepository {
    fun getContinueWatching(): Flow<List<PlaybackProgress>>
    fun getWatchHistory(): Flow<List<PlaybackProgress>>
    fun getWatchHistoryForAnime(animeId: String): Flow<List<PlaybackProgress>>
    fun getCompletedEpisodesFlow(animeId: String): Flow<List<Int>>
    suspend fun getEpisodeProgress(animeId: String, episodeNumber: Int): PlaybackProgress?
    suspend fun getLatestProgressForAnime(animeId: String): PlaybackProgress?
    suspend fun getCompletedEpisodeNumbers(animeId: String): List<Int>
    suspend fun savePlaybackProgress(progress: PlaybackProgress)
    suspend fun markEpisodeCompleted(animeId: String, episodeNumber: Int, completed: Boolean)
    suspend fun deleteHistoryForAnime(animeId: String)
    suspend fun deleteEpisodeHistory(animeId: String, episodeNumber: Int)
    suspend fun clearHistory()
}

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
    suspend fun setAutoSkipIntro(enabled: Boolean)
    suspend fun setAutoSkipOutro(enabled: Boolean)
    suspend fun setAutoSkipRecap(enabled: Boolean)
    suspend fun setDefaultQuality(quality: String)
    suspend fun setAudioSubPreference(pref: String)
    suspend fun setSkipDurationSeconds(seconds: Int)
    suspend fun setPlayerGestures(enabled: Boolean)
    suspend fun setPlayerTheme(theme: String)
    suspend fun setDefaultPlaybackSpeed(speed: Float)
    suspend fun setAutoPlayNext(enabled: Boolean)
    suspend fun setBackgroundPlayback(enabled: Boolean)
    suspend fun setPipEnabled(enabled: Boolean)
    suspend fun setBufferCacheMb(mb: Int)
    suspend fun setHardwareAcceleration(enabled: Boolean)
    suspend fun setHideAdultContent(hide: Boolean)
    suspend fun setUnifiedLibrary(unified: Boolean)
    suspend fun setCardStyle(style: String)
    suspend fun setHistoryCardStyle(style: String)
    suspend fun setCarouselStyle(style: String)
    suspend fun setNavBarStyle(style: String)
    suspend fun setNavBarMargin(margin: Int)
    suspend fun setPreferredProvider(providerId: String)
    suspend fun setSyncConflictStrategy(strategy: String)
    suspend fun setAutoSyncAniList(enabled: Boolean)
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
}

