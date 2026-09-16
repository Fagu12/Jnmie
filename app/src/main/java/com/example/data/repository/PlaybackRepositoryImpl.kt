package com.example.data.repository

import com.example.domain.model.PlaybackProgress
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.PlaybackRepository
import com.example.data.local.dao.PlaybackDao
import com.example.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackRepositoryImpl(
    private val playbackDao: PlaybackDao,
    private val aniListRepository: AniListRepository,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PlaybackRepository {

    init {
        externalScope.launch {
            val existing = playbackDao.getContinueWatching().firstOrNull()
            if (existing.isNullOrEmpty()) {
                // Seed initial history matching the user's initial setup
                val seed = listOf(
                    WatchHistoryEntity(
                        animeId = "151807",
                        episodeNumber = 8,
                        animeTitle = "Solo Leveling",
                        coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151807-m1gkuoiB8p8Y.png",
                        episodeTitle = "This Is Frustrating",
                        currentPositionMs = 285000L,
                        durationMs = 1420000L,
                        lastWatchedTimestamp = System.currentTimeMillis() - 1000 * 60 * 35,
                        completed = false
                    ),
                    WatchHistoryEntity(
                        animeId = "21",
                        episodeNumber = 1098,
                        animeTitle = "One Piece",
                        coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21-YCDoj1EkAxFn.jpg",
                        episodeTitle = "The Eccentric Dream",
                        currentPositionMs = 820000L,
                        durationMs = 1440000L,
                        lastWatchedTimestamp = System.currentTimeMillis() - 1000 * 60 * 180,
                        completed = false
                    ),
                    WatchHistoryEntity(
                        animeId = "154587",
                        episodeNumber = 7,
                        animeTitle = "Bleach: Thousand-Year Blood War",
                        coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-dC0qIsqW2m7S.jpg",
                        episodeTitle = "Born in the Dark",
                        currentPositionMs = 1410000L,
                        durationMs = 1420000L,
                        lastWatchedTimestamp = System.currentTimeMillis() - 1000 * 3600 * 24,
                        completed = true
                    )
                )
                for (item in seed) {
                    playbackDao.insertOrUpdate(item)
                }
            }
        }
    }

    override fun getContinueWatching(): Flow<List<PlaybackProgress>> {
        return playbackDao.getContinueWatching().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getWatchHistory(): Flow<List<PlaybackProgress>> {
        return playbackDao.getAllWatchHistory().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getWatchHistoryForAnime(animeId: String): Flow<List<PlaybackProgress>> {
        return playbackDao.getWatchHistoryForAnime(animeId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getCompletedEpisodesFlow(animeId: String): Flow<List<Int>> {
        return playbackDao.getCompletedEpisodesFlow(animeId)
    }

    override suspend fun getEpisodeProgress(animeId: String, episodeNumber: Int): PlaybackProgress? =
        withContext(Dispatchers.IO) {
            playbackDao.getEpisodeProgress(animeId, episodeNumber)?.toDomain()
        }

    override suspend fun getLatestProgressForAnime(animeId: String): PlaybackProgress? =
        withContext(Dispatchers.IO) {
            playbackDao.getLatestProgressForAnime(animeId)?.toDomain()
        }

    override suspend fun getCompletedEpisodeNumbers(animeId: String): List<Int> =
        withContext(Dispatchers.IO) {
            playbackDao.getCompletedEpisodeNumbers(animeId)
        }

    override suspend fun savePlaybackProgress(progress: PlaybackProgress) = withContext(Dispatchers.IO) {
        val entity = WatchHistoryEntity(
            animeId = progress.animeId,
            episodeNumber = progress.episodeNumber,
            animeTitle = progress.animeTitle,
            coverUrl = progress.coverUrl,
            episodeTitle = progress.episodeTitle,
            currentPositionMs = progress.currentPositionMs,
            durationMs = progress.durationMs,
            lastWatchedTimestamp = progress.lastWatchedTimestamp,
            completed = progress.completed
        )
        playbackDao.insertOrUpdate(entity)

        // Sync with AniList if 90%+ or completed
        if (progress.completed || (progress.durationMs > 0 && progress.currentPositionMs > (progress.durationMs * 0.9))) {
            aniListRepository.updateEpisodeProgress(
                animeId = progress.animeId,
                episodeNumber = progress.episodeNumber,
                completed = true
            )
        }
    }

    override suspend fun markEpisodeCompleted(animeId: String, episodeNumber: Int, completed: Boolean) =
        withContext(Dispatchers.IO) {
            playbackDao.markEpisodeCompleted(animeId, episodeNumber, completed)
            if (completed) {
                aniListRepository.updateEpisodeProgress(
                    animeId = animeId,
                    episodeNumber = episodeNumber,
                    completed = true
                )
            }
        }

    override suspend fun deleteHistoryForAnime(animeId: String) = withContext(Dispatchers.IO) {
        playbackDao.deleteHistoryForAnime(animeId)
    }

    override suspend fun deleteEpisodeHistory(animeId: String, episodeNumber: Int) = withContext(Dispatchers.IO) {
        playbackDao.deleteHistoryItem(animeId, episodeNumber)
    }

    override suspend fun clearHistory() = withContext(Dispatchers.IO) {
        playbackDao.clearHistory()
    }

    private fun WatchHistoryEntity.toDomain() = PlaybackProgress(
        animeId = animeId,
        animeTitle = animeTitle,
        coverUrl = coverUrl,
        episodeNumber = episodeNumber,
        episodeTitle = episodeTitle,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        lastWatchedTimestamp = lastWatchedTimestamp,
        completed = completed
    )
}

