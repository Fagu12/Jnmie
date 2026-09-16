package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AniListMediaEntryEntity
import com.example.data.local.entity.CachedAnimeEntity
import com.example.data.local.entity.CachedEpisodeEntity
import com.example.data.local.entity.ExtensionRepoEntity
import com.example.data.local.entity.FavoriteAnimeEntity
import com.example.data.local.entity.InstalledExtensionEntity
import com.example.data.local.entity.PendingSyncEntity
import com.example.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteAnimeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE animeId = :animeId)")
    fun isFavoriteFlow(animeId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE animeId = :animeId)")
    suspend fun isFavorite(animeId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteAnimeEntity)

    @Query("DELETE FROM favorites WHERE animeId = :animeId")
    suspend fun deleteFavorite(animeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedAnime(animeList: List<CachedAnimeEntity>)

    @Query("SELECT * FROM cached_anime WHERE id = :id")
    suspend fun getCachedAnime(id: String): CachedAnimeEntity?

    @Query("SELECT * FROM cached_anime WHERE id = :id")
    fun getCachedAnimeFlow(id: String): Flow<CachedAnimeEntity?>

    @Query("SELECT * FROM cached_anime ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getRecentCachedAnime(limit: Int): List<CachedAnimeEntity>

    @Query("DELETE FROM cached_anime WHERE cachedAt < :olderThan")
    suspend fun clearExpiredCachedAnime(olderThan: Long)

    @Query("DELETE FROM cached_anime")
    suspend fun clearCachedAnime()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedEpisodes(episodes: List<CachedEpisodeEntity>)

    @Query("SELECT * FROM cached_episodes WHERE animeId = :animeId ORDER BY number ASC")
    suspend fun getCachedEpisodes(animeId: String): List<CachedEpisodeEntity>

    @Query("SELECT * FROM cached_episodes WHERE animeId = :animeId ORDER BY number ASC")
    fun getCachedEpisodesFlow(animeId: String): Flow<List<CachedEpisodeEntity>>

    @Query("DELETE FROM cached_episodes WHERE animeId = :animeId")
    suspend fun clearCachedEpisodes(animeId: String)
}

@Dao
interface PlaybackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC LIMIT 20")
    fun getContinueWatching(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE animeId = :animeId ORDER BY episodeNumber ASC")
    fun getWatchHistoryForAnime(animeId: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE animeId = :animeId AND episodeNumber = :episodeNumber LIMIT 1")
    suspend fun getEpisodeProgress(animeId: String, episodeNumber: Int): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE animeId = :animeId AND episodeNumber = :episodeNumber LIMIT 1")
    fun getEpisodeProgressFlow(animeId: String, episodeNumber: Int): Flow<WatchHistoryEntity?>

    @Query("SELECT * FROM watch_history WHERE animeId = :animeId ORDER BY lastWatchedTimestamp DESC LIMIT 1")
    suspend fun getLatestProgressForAnime(animeId: String): WatchHistoryEntity?

    @Query("SELECT episodeNumber FROM watch_history WHERE animeId = :animeId AND completed = 1 ORDER BY episodeNumber ASC")
    suspend fun getCompletedEpisodeNumbers(animeId: String): List<Int>

    @Query("SELECT episodeNumber FROM watch_history WHERE animeId = :animeId AND completed = 1 ORDER BY episodeNumber ASC")
    fun getCompletedEpisodesFlow(animeId: String): Flow<List<Int>>

    @Query("UPDATE watch_history SET completed = :completed, lastWatchedTimestamp = :timestamp WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun markEpisodeCompleted(animeId: String, episodeNumber: Int, completed: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM watch_history WHERE animeId = :animeId AND episodeNumber = :episodeNumber")
    suspend fun deleteHistoryItem(animeId: String, episodeNumber: Int)

    @Query("DELETE FROM watch_history WHERE animeId = :animeId")
    suspend fun deleteHistoryForAnime(animeId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM extension_repos ORDER BY lastRefreshed DESC")
    fun getRepositories(): Flow<List<ExtensionRepoEntity>>

    @Query("SELECT * FROM extension_repos WHERE url = :url LIMIT 1")
    suspend fun getRepository(url: String): ExtensionRepoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repo: ExtensionRepoEntity)

    @Query("DELETE FROM extension_repos WHERE url = :url")
    suspend fun deleteRepository(url: String)

    @Query("SELECT * FROM installed_extensions")
    fun getInstalledExtensions(): Flow<List<InstalledExtensionEntity>>

    @Query("SELECT * FROM installed_extensions WHERE isEnabled = 1")
    suspend fun getEnabledExtensions(): List<InstalledExtensionEntity>

    @Query("SELECT * FROM installed_extensions WHERE isEnabled = 1")
    fun getEnabledExtensionsFlow(): Flow<List<InstalledExtensionEntity>>

    @Query("SELECT * FROM installed_extensions WHERE id = :id LIMIT 1")
    suspend fun getExtension(id: String): InstalledExtensionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: InstalledExtensionEntity)

    @Query("DELETE FROM installed_extensions WHERE id = :id")
    suspend fun deleteExtension(id: String)

    @Query("UPDATE installed_extensions SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setExtensionEnabled(id: String, isEnabled: Boolean)
}

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSync(entity: PendingSyncEntity): Long

    @Query("SELECT * FROM pending_sync ORDER BY timestamp ASC")
    suspend fun getAllPendingSync(): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun getPendingSyncCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync")
    suspend fun getPendingSyncCount(): Int

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deletePendingSync(id: Long)

    @Query("DELETE FROM pending_sync WHERE animeId = :animeId")
    suspend fun deletePendingSyncForAnime(animeId: String)

    @Query("DELETE FROM pending_sync")
    suspend fun clearPendingSync()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAniListEntries(entries: List<AniListMediaEntryEntity>)

    @Query("SELECT * FROM anilist_media_entries ORDER BY updatedAt DESC")
    fun getAllAniListEntriesFlow(): Flow<List<AniListMediaEntryEntity>>

    @Query("SELECT * FROM anilist_media_entries WHERE status = :status ORDER BY updatedAt DESC")
    fun getAniListEntriesByStatusFlow(status: String): Flow<List<AniListMediaEntryEntity>>

    @Query("SELECT * FROM anilist_media_entries WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getAniListEntry(mediaId: Int): AniListMediaEntryEntity?

    @Query("SELECT * FROM anilist_media_entries WHERE animeId = :animeId LIMIT 1")
    suspend fun getAniListEntryByAnimeId(animeId: String): AniListMediaEntryEntity?

    @Query("DELETE FROM anilist_media_entries")
    suspend fun clearAniListEntries()
}


