package com.example.domain.usecase

import com.example.domain.model.PlaybackProgress
import com.example.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow

class GetContinueWatchingUseCase(private val repository: PlaybackRepository) {
    operator fun invoke(): Flow<List<PlaybackProgress>> =
        repository.getContinueWatching()
}

class GetWatchHistoryUseCase(private val repository: PlaybackRepository) {
    operator fun invoke(): Flow<List<PlaybackProgress>> =
        repository.getWatchHistory()
}

class GetEpisodeProgressUseCase(private val repository: PlaybackRepository) {
    suspend operator fun invoke(animeId: String, episodeNumber: Int): PlaybackProgress? =
        repository.getEpisodeProgress(animeId, episodeNumber)
}

class SavePlaybackProgressUseCase(private val repository: PlaybackRepository) {
    suspend operator fun invoke(progress: PlaybackProgress) =
        repository.savePlaybackProgress(progress)
}

class ClearWatchHistoryUseCase(private val repository: PlaybackRepository) {
    suspend operator fun invoke() =
        repository.clearHistory()
}
