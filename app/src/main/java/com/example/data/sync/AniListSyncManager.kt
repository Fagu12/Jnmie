package com.example.data.sync

import com.example.data.local.dao.AnimeDao
import com.example.data.local.dao.PlaybackDao
import com.example.data.local.dao.SyncDao
import com.example.data.local.entity.AniListMediaEntryEntity
import com.example.data.local.entity.PendingSyncEntity
import com.example.data.local.entity.WatchHistoryEntity
import com.example.data.local.preferences.UserPreferencesDataStore
import com.example.data.local.security.SecureTokenStorage
import com.example.data.remote.anilist.AniListGraphQL
import com.example.domain.model.AniListMediaListEntry
import com.example.domain.model.ConflictStrategy
import com.example.domain.model.SyncReport
import com.example.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AniListSyncManager(
    private val aniListGraphQL: AniListGraphQL,
    private val syncDao: SyncDao,
    private val playbackDao: PlaybackDao,
    private val animeDao: AnimeDao,
    private val secureTokenStorage: SecureTokenStorage,
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val pendingSyncCount: Flow<Int> = syncDao.getPendingSyncCountFlow()
    val cachedAniListEntries: Flow<List<AniListMediaEntryEntity>> = syncDao.getAllAniListEntriesFlow()

    private val syncMutex = Mutex()
    private val recentSyncTimestamps = ConcurrentHashMap<String, Long>()
    private val inFlightSyncs = ConcurrentHashMap<String, Boolean>()

    private val minSyncDebounceIntervalMs = 2500L
    private val maxRetryCount = 3

    init {
        // Automatically attempt to process pending queue on initialization if authenticated
        externalScope.launch {
            val token = secureTokenStorage.getAccessToken()
            if (!token.isNullOrBlank()) {
                val pending = syncDao.getAllPendingSync()
                if (pending.isNotEmpty()) {
                    retryPendingSync()
                }
            }
        }
    }

    /**
     * Synchronizes a single episode progress with AniList with debouncing and offline fallback queue.
     */
    suspend fun syncPlaybackProgress(
        animeId: String,
        episodeNumber: Int,
        completed: Boolean,
        immediate: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val token = secureTokenStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            // User not authenticated; progress is local only
            return@withContext Result.success(Unit)
        }

        val mediaId = animeId.toIntOrNull() ?: return@withContext Result.success(Unit)
        val dedupeKey = "${mediaId}_$episodeNumber"
        val now = System.currentTimeMillis()

        // Check deduplication / debounce
        val lastSyncTime = recentSyncTimestamps[dedupeKey] ?: 0L
        if (!immediate && (now - lastSyncTime < minSyncDebounceIntervalMs)) {
            return@withContext Result.success(Unit)
        }

        if (inFlightSyncs[dedupeKey] == true) {
            return@withContext Result.success(Unit)
        }

        inFlightSyncs[dedupeKey] = true
        recentSyncTimestamps[dedupeKey] = now

        try {
            val targetStatus = if (completed) "COMPLETED" else "CURRENT"
            val success = aniListGraphQL.updateMediaProgress(
                token = token,
                mediaId = mediaId,
                progress = episodeNumber,
                status = targetStatus
            )

            if (success) {
                // Clear any pending queue entries for this media
                syncDao.deletePendingSyncForAnime(animeId)
                
                // Update local cached anilist entry
                val cached = syncDao.getAniListEntry(mediaId)
                if (cached != null) {
                    syncDao.insertAniListEntries(listOf(cached.copy(progress = episodeNumber, status = targetStatus, updatedAt = now)))
                }
                Result.success(Unit)
            } else {
                // Queue for background retry
                queuePendingSync(animeId, episodeNumber, targetStatus, null, "API returned false")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // Queue offline sync
            queuePendingSync(animeId, episodeNumber, if (completed) "COMPLETED" else "CURRENT", null, e.message)
            Result.success(Unit) // Return success so UI/playback is not interrupted
        } finally {
            inFlightSyncs.remove(dedupeKey)
        }
    }

    /**
     * Updates an anime's status and score on AniList with immediate local update and background sync.
     */
    suspend fun syncMediaStatus(
        animeId: String,
        status: String,
        score: Double? = null,
        progress: Int? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val mediaId = animeId.toIntOrNull() ?: return@withContext Result.failure(IllegalArgumentException("Invalid anime ID"))
        val now = System.currentTimeMillis()

        // Update local AniList cache immediately
        val existing = syncDao.getAniListEntry(mediaId)
        val updatedEntry = existing?.copy(
            status = status,
            score = score ?: existing.score,
            progress = progress ?: existing.progress,
            updatedAt = now
        ) ?: AniListMediaEntryEntity(
            mediaId = mediaId,
            animeId = animeId,
            title = "Anime #$animeId",
            status = status,
            progress = progress ?: 0,
            score = score ?: 0.0,
            updatedAt = now
        )
        syncDao.insertAniListEntries(listOf(updatedEntry))

        val token = secureTokenStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.success(Unit)
        }

        try {
            val success = aniListGraphQL.setMediaListStatus(
                token = token,
                mediaId = mediaId,
                status = status,
                score = score,
                progress = progress
            )

            if (success) {
                syncDao.deletePendingSyncForAnime(animeId)
                Result.success(Unit)
            } else {
                queuePendingSync(animeId, progress ?: 0, status, score, "Status update rejected")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            queuePendingSync(animeId, progress ?: 0, status, score, e.message)
            Result.success(Unit)
        }
    }

    /**
     * Performs full bidirectional synchronization with conflict resolution.
     */
    suspend fun syncFull(strategy: ConflictStrategy = ConflictStrategy.HIGHEST_PROGRESS): Result<SyncReport> = withContext(Dispatchers.IO) {
        val token = secureTokenStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            val err = "AniList account not connected. Please log in first."
            _syncStatus.value = SyncStatus.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        if (!syncMutex.tryLock()) {
            return@withContext Result.failure(IllegalStateException("Sync already in progress"))
        }

        _syncStatus.value = SyncStatus.Syncing("Synchronizing watchlist & progress...")

        try {
            // 1. Get current authenticated user
            val user = aniListGraphQL.getViewer(token)
            val username = user.name.ifBlank { userPreferencesDataStore.aniListUsernameFlow.firstOrNull() }

            // 2. Fetch remote AniList entries (all lists)
            val remoteEntries = aniListGraphQL.getUserAnimeList(
                token = token,
                userId = user.id,
                username = username,
                status = null
            )

            var pulledCount = 0
            var pushedCount = 0
            var conflictsResolved = 0
            val errors = mutableListOf<String>()

            // 3. Map remote entries to DB cache entities
            val cachedEntities = remoteEntries.map { entry ->
                AniListMediaEntryEntity(
                    mediaId = entry.mediaId,
                    animeId = entry.anime.id,
                    title = entry.anime.title,
                    romajiTitle = entry.anime.romajiTitle,
                    coverUrl = entry.anime.coverUrl,
                    bannerUrl = entry.anime.bannerUrl,
                    status = entry.status,
                    progress = entry.progress,
                    score = entry.score,
                    totalEpisodes = entry.anime.totalEpisodes,
                    format = entry.anime.format,
                    genresJson = entry.anime.genres.joinToString(","),
                    updatedAt = if (entry.updatedAt > 0) entry.updatedAt * 1000L else System.currentTimeMillis()
                )
            }
            syncDao.insertAniListEntries(cachedEntities)

            // 4. Resolve conflicts between local watch history and remote AniList progress
            for (remote in remoteEntries) {
                val animeId = remote.anime.id
                val localProgress = playbackDao.getEpisodeProgress(animeId, remote.progress)
                    ?: playbackDao.getLatestProgressForAnime(animeId)

                val remoteEpisode = remote.progress
                val remoteUpdatedMs = if (remote.updatedAt > 0) remote.updatedAt * 1000L else 0L

                if (localProgress == null) {
                    // Item in remote AniList with progress but no local watch history -> Pull to local DB
                    if (remoteEpisode > 0 || remote.status == "COMPLETED") {
                        val isCompleted = remote.status == "COMPLETED"
                        playbackDao.insertOrUpdate(
                            WatchHistoryEntity(
                                animeId = animeId,
                                episodeNumber = if (remoteEpisode > 0) remoteEpisode else 1,
                                animeTitle = remote.anime.title,
                                coverUrl = remote.anime.coverUrl,
                                episodeTitle = "Episode $remoteEpisode",
                                currentPositionMs = if (isCompleted) 1440000L else 720000L,
                                durationMs = 1440000L,
                                lastWatchedTimestamp = if (remoteUpdatedMs > 0) remoteUpdatedMs else System.currentTimeMillis(),
                                completed = isCompleted
                            )
                        )
                        pulledCount++
                    }
                } else {
                    // Both exist: check conflict
                    val localEpisode = localProgress.episodeNumber
                    val localUpdatedMs = localProgress.lastWatchedTimestamp
                    val localCompleted = localProgress.completed

                    if (remoteEpisode != localEpisode || (remote.status == "COMPLETED" && !localCompleted)) {
                        conflictsResolved++
                        val chooseRemote = when (strategy) {
                            ConflictStrategy.HIGHEST_PROGRESS -> remoteEpisode >= localEpisode
                            ConflictStrategy.LATEST_TIMESTAMP -> remoteUpdatedMs >= localUpdatedMs
                            ConflictStrategy.REMOTE_WINS -> true
                            ConflictStrategy.LOCAL_WINS -> false
                        }

                        if (chooseRemote) {
                            // Update local
                            val isCompleted = remote.status == "COMPLETED" || (remote.anime.totalEpisodes != null && remoteEpisode >= remote.anime.totalEpisodes)
                            playbackDao.insertOrUpdate(
                                localProgress.copy(
                                    episodeNumber = remoteEpisode,
                                    completed = isCompleted,
                                    lastWatchedTimestamp = if (remoteUpdatedMs > 0) remoteUpdatedMs else System.currentTimeMillis()
                                )
                            )
                            pulledCount++
                        } else {
                            // Push local to remote
                            try {
                                val pushStatus = if (localCompleted) "COMPLETED" else "CURRENT"
                                val ok = aniListGraphQL.updateMediaProgress(
                                    token = token,
                                    mediaId = remote.mediaId,
                                    progress = localEpisode,
                                    status = pushStatus
                                )
                                if (ok) pushedCount++
                            } catch (e: Exception) {
                                errors.add("Failed to push anime ${remote.anime.title}: ${e.message}")
                            }
                        }
                    }
                }
            }

            // 5. Process pending offline sync queue
            val pendingRetried = processPendingQueueInternal(token)
            pushedCount += pendingRetried

            // 6. Record last synced timestamp
            val syncTimestamp = System.currentTimeMillis()
            userPreferencesDataStore.updateSettings {
                it.copy(lastSyncedTimestamp = syncTimestamp)
            }

            val report = SyncReport(
                pulledCount = pulledCount,
                pushedCount = pushedCount,
                conflictsResolved = conflictsResolved,
                errors = errors,
                timestamp = syncTimestamp
            )

            _syncStatus.value = SyncStatus.Success(
                lastSyncedTimestamp = syncTimestamp,
                pulledCount = pulledCount,
                pushedCount = pushedCount,
                conflictsResolved = conflictsResolved
            )

            Result.success(report)
        } catch (e: Exception) {
            val errMsg = e.message ?: "Failed to sync with AniList"
            _syncStatus.value = SyncStatus.Error(errMsg)
            Result.failure(e)
        } finally {
            syncMutex.unlock()
        }
    }

    /**
     * Retries any pending sync queue items.
     */
    suspend fun retryPendingSync(): Result<Int> = withContext(Dispatchers.IO) {
        val token = secureTokenStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("AniList token not available"))
        }
        val count = processPendingQueueInternal(token)
        Result.success(count)
    }

    suspend fun clearPendingSync() = withContext(Dispatchers.IO) {
        syncDao.clearPendingSync()
    }

    private suspend fun queuePendingSync(
        animeId: String,
        episodeNumber: Int,
        status: String?,
        score: Double?,
        error: String?
    ) {
        val entity = PendingSyncEntity(
            animeId = animeId,
            episodeNumber = episodeNumber,
            status = status,
            score = score,
            timestamp = System.currentTimeMillis(),
            lastError = error
        )
        syncDao.insertPendingSync(entity)
    }

    private suspend fun processPendingQueueInternal(token: String): Int {
        val pendingItems = syncDao.getAllPendingSync()
        var successCount = 0

        for (item in pendingItems) {
            val mediaId = item.animeId.toIntOrNull() ?: continue
            try {
                val ok = if (item.status != null && item.score != null) {
                    aniListGraphQL.setMediaListStatus(
                        token = token,
                        mediaId = mediaId,
                        status = item.status,
                        score = item.score,
                        progress = item.episodeNumber
                    )
                } else {
                    aniListGraphQL.updateMediaProgress(
                        token = token,
                        mediaId = mediaId,
                        progress = item.episodeNumber,
                        status = item.status
                    )
                }

                if (ok) {
                    syncDao.deletePendingSync(item.id)
                    successCount++
                } else {
                    if (item.retryCount >= maxRetryCount) {
                        syncDao.deletePendingSync(item.id)
                    } else {
                        syncDao.insertPendingSync(item.copy(retryCount = item.retryCount + 1))
                    }
                }
            } catch (e: Exception) {
                if (item.retryCount >= maxRetryCount) {
                    syncDao.deletePendingSync(item.id)
                } else {
                    syncDao.insertPendingSync(item.copy(retryCount = item.retryCount + 1, lastError = e.message))
                }
            }
        }
        return successCount
    }
}
