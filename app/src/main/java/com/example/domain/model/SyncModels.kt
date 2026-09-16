package com.example.domain.model

/**
 * Synchronization state representations for AniList.
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    data class Syncing(val message: String = "Synchronizing with AniList...") : SyncStatus()
    data class Success(
        val lastSyncedTimestamp: Long = System.currentTimeMillis(),
        val pulledCount: Int = 0,
        val pushedCount: Int = 0,
        val conflictsResolved: Int = 0
    ) : SyncStatus()
    data class Error(
        val message: String,
        val lastSyncedTimestamp: Long = 0L
    ) : SyncStatus()
}

/**
 * Strategy for resolving conflicts when local progress differs from remote AniList progress.
 */
enum class ConflictStrategy(val displayName: String, val description: String) {
    HIGHEST_PROGRESS(
        displayName = "Highest Episode (Recommended)",
        description = "Whichever source has watched more episodes takes precedence."
    ),
    LATEST_TIMESTAMP(
        displayName = "Latest Watched Time",
        description = "The most recently updated watch session overwrites older progress."
    ),
    REMOTE_WINS(
        displayName = "AniList Always Wins",
        description = "Remote AniList watchlist always overrides local device progress."
    ),
    LOCAL_WINS(
        displayName = "Local Device Always Wins",
        description = "Local watch history always pushes and overrides AniList remote."
    )
}

/**
 * Report generated after full bidirectional synchronization.
 */
data class SyncReport(
    val pulledCount: Int = 0,
    val pushedCount: Int = 0,
    val conflictsResolved: Int = 0,
    val errors: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    val isSuccess: Boolean get() = errors.isEmpty()
}

/**
 * Action queued when offline or on transient network failure.
 */
data class PendingSyncItem(
    val id: Long = 0L,
    val animeId: String,
    val episodeNumber: Int,
    val status: String? = null,
    val score: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
