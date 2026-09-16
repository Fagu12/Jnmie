package com.example.domain.usecase

import com.example.domain.model.AniListMediaListEntry
import com.example.domain.model.AniListUser
import com.example.domain.model.ConflictStrategy
import com.example.domain.model.SyncReport
import com.example.domain.model.SyncStatus
import com.example.domain.repository.AniListRepository
import kotlinx.coroutines.flow.Flow

class GetAniListUserUseCase(private val repository: AniListRepository) {
    operator fun invoke(): Flow<AniListUser?> =
        repository.currentUser
}

class GetAniListAuthStatusUseCase(private val repository: AniListRepository) {
    operator fun invoke(): Flow<Boolean> =
        repository.isAuthenticated
}

class GetAniListSyncStatusUseCase(private val repository: AniListRepository) {
    operator fun invoke(): Flow<SyncStatus> =
        repository.syncStatus
}

class GetPendingSyncCountUseCase(private val repository: AniListRepository) {
    operator fun invoke(): Flow<Int> =
        repository.pendingSyncCount
}

class GetCachedAniListUserListUseCase(private val repository: AniListRepository) {
    operator fun invoke(): Flow<List<AniListMediaListEntry>> =
        repository.cachedUserList
}

class AuthenticateAniListUseCase(private val repository: AniListRepository) {
    suspend operator fun invoke(token: String): Result<AniListUser> =
        repository.authenticateWithToken(token)
}

class LogoutAniListUseCase(private val repository: AniListRepository) {
    suspend operator fun invoke() =
        repository.logout()
}

class SyncAniListUserListsUseCase(private val repository: AniListRepository) {
    suspend operator fun invoke(strategy: ConflictStrategy = ConflictStrategy.HIGHEST_PROGRESS): Result<SyncReport> =
        repository.syncUserLists(strategy)
}

class GetUserAnimeListUseCase(private val repository: AniListRepository) {
    suspend operator fun invoke(status: String? = null): Result<List<AniListMediaListEntry>> =
        repository.getUserAnimeList(status)
}

class UpdateAniListProgressUseCase(private val repository: AniListRepository) {
    suspend operator fun invoke(animeId: String, episodeNumber: Int, completed: Boolean): Result<Unit> =
        repository.updateEpisodeProgress(animeId, episodeNumber, completed)
}

class SetAniListMediaStatusUseCase(private val repository: AniListRepository) {
    suspend operator fun invoke(animeId: String, status: String, score: Double? = null, progress: Int? = null): Result<Unit> =
        repository.setAnimeListStatus(animeId, status, score, progress)
}

class RetryPendingSyncUseCase(private val repository: AniListRepository) {
    suspend operator fun invoke(): Result<Int> =
        repository.retryPendingSync()
}

class ClearPendingSyncUseCase(private val repository: AniListRepository) {
    suspend operator fun invoke() =
        repository.clearPendingSync()
}

class HandleAniListOAuthCallbackUseCase(private val repository: AniListRepository) {
    suspend operator fun invoke(uriString: String): Result<AniListUser> =
        repository.handleOAuthCallback(uriString)
}


