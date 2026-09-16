package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.AppDatabase
import com.example.data.local.entity.WatchHistoryEntity
import com.example.data.local.preferences.UserPreferencesDataStore
import com.example.data.local.security.SecureTokenStorage
import com.example.data.remote.anilist.AniListGraphQL
import com.example.data.sync.AniListSyncManager
import com.example.domain.model.AniListMediaListEntry
import com.example.domain.model.AniListUser
import com.example.domain.model.Anime
import com.example.domain.model.ConflictStrategy
import com.example.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AniListSyncIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferencesDataStore

    private val sampleUser = AniListUser(
        id = 9999L,
        name = "OtakuTester",
        avatarUrl = null,
        totalAnimeWatched = 10,
        totalEpisodesWatched = 120,
        daysWatched = 2.0,
        meanScore = 8.5
    )

    private val sampleAnime = Anime(
        id = "21087",
        title = "One-Punch Man",
        romajiTitle = "One-Punch Man",
        coverUrl = "https://s4.anilist.co/cover.png",
        totalEpisodes = 12,
        format = "TV"
    )

    class FakeSyncSecureStorage(var currentToken: String? = "test_token_123") : SecureTokenStorage {
        private val _flow = MutableStateFlow(currentToken)
        override val tokenFlow: Flow<String?> = _flow.asStateFlow()
        override suspend fun getAccessToken(): String? = currentToken
        override suspend fun saveAccessToken(token: String?) {
            currentToken = token
            _flow.value = token
        }
        override suspend fun clearAccessToken() {
            currentToken = null
            _flow.value = null
        }
        override fun isStorageSecured(): Boolean = true
    }

    class MockAniListApi(
        private val user: AniListUser,
        private val anime: Anime
    ) : AniListGraphQL() {
        var remoteProgress = 5
        var remoteStatus = "CURRENT"
        var remoteScore = 8.0
        var updateProgressCalls = 0
        var lastUpdatedProgress: Int? = null
        var shouldFailNetwork: Boolean = false

        override suspend fun getViewer(token: String): AniListUser {
            if (shouldFailNetwork) throw RuntimeException("Network unreachable")
            return user
        }

        override suspend fun getUserAnimeList(
            token: String,
            userId: Long?,
            username: String?,
            status: String?
        ): List<AniListMediaListEntry> {
            if (shouldFailNetwork) throw RuntimeException("Network unreachable")
            return listOf(
                AniListMediaListEntry(
                    id = 101L,
                    mediaId = anime.id.toInt(),
                    status = remoteStatus,
                    progress = remoteProgress,
                    score = remoteScore,
                    updatedAt = 1700000000L,
                    anime = anime
                )
            )
        }

        override suspend fun updateMediaProgress(
            token: String,
            mediaId: Int,
            progress: Int,
            status: String?,
            score: Double?
        ): Boolean {
            if (shouldFailNetwork) throw RuntimeException("Network unreachable")
            updateProgressCalls++
            lastUpdatedProgress = progress
            remoteProgress = progress
            if (status != null) remoteStatus = status
            return true
        }

        override suspend fun setMediaListStatus(
            token: String,
            mediaId: Int,
            status: String,
            score: Double?,
            progress: Int?
        ): Boolean {
            if (shouldFailNetwork) throw RuntimeException("Network unreachable")
            remoteStatus = status
            if (score != null) remoteScore = score
            if (progress != null) remoteProgress = progress
            return true
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferencesDataStore(context)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testFullSyncPullsRemoteProgressWhenNoLocalHistory() = runTest(testDispatcher) {
        val mockApi = MockAniListApi(sampleUser, sampleAnime)
        mockApi.remoteProgress = 7
        mockApi.remoteStatus = "CURRENT"

        val syncManager = AniListSyncManager(
            aniListGraphQL = mockApi,
            syncDao = database.syncDao(),
            playbackDao = database.playbackDao(),
            animeDao = database.animeDao(),
            secureTokenStorage = FakeSyncSecureStorage(),
            userPreferencesDataStore = preferences,
            externalScope = this
        )

        advanceUntilIdle()

        val result = syncManager.syncFull(ConflictStrategy.HIGHEST_PROGRESS)
        assertTrue(result.isSuccess)
        val report = result.getOrNull()
        assertNotNull(report)
        assertEquals(1, report?.pulledCount)

        // Verify local DB was updated with remote progress
        val localHistory = database.playbackDao().getEpisodeProgress("21087", 7)
        assertNotNull(localHistory)
        assertEquals(7, localHistory?.episodeNumber)
        assertEquals("One-Punch Man", localHistory?.animeTitle)

        // Verify cached entries
        val cached = database.syncDao().getAniListEntry(21087)
        assertNotNull(cached)
        assertEquals(7, cached?.progress)
        assertEquals("CURRENT", cached?.status)
    }

    @Test
    fun testConflictResolutionHighestProgressPushesLocalWhenHigher() = runTest(testDispatcher) {
        val mockApi = MockAniListApi(sampleUser, sampleAnime)
        mockApi.remoteProgress = 3 // Remote has ep 3

        // Insert local progress at ep 8 (higher than remote)
        database.playbackDao().insertOrUpdate(
            WatchHistoryEntity(
                animeId = "21087",
                episodeNumber = 8,
                animeTitle = "One-Punch Man",
                coverUrl = "",
                episodeTitle = "Episode 8",
                currentPositionMs = 1200000L,
                durationMs = 1440000L,
                lastWatchedTimestamp = System.currentTimeMillis(),
                completed = false
            )
        )

        val syncManager = AniListSyncManager(
            aniListGraphQL = mockApi,
            syncDao = database.syncDao(),
            playbackDao = database.playbackDao(),
            animeDao = database.animeDao(),
            secureTokenStorage = FakeSyncSecureStorage(),
            userPreferencesDataStore = preferences,
            externalScope = this
        )

        advanceUntilIdle()

        val result = syncManager.syncFull(ConflictStrategy.HIGHEST_PROGRESS)
        assertTrue(result.isSuccess)
        val report = result.getOrNull()
        assertNotNull(report)
        assertEquals(1, report?.conflictsResolved)
        assertEquals(1, report?.pushedCount)
        assertEquals(8, mockApi.lastUpdatedProgress)
    }

    @Test
    fun testOfflineSyncQueueAndRetry() = runTest(testDispatcher) {
        val mockApi = MockAniListApi(sampleUser, sampleAnime)
        mockApi.shouldFailNetwork = true // Simulate offline / network failure

        val syncManager = AniListSyncManager(
            aniListGraphQL = mockApi,
            syncDao = database.syncDao(),
            playbackDao = database.playbackDao(),
            animeDao = database.animeDao(),
            secureTokenStorage = FakeSyncSecureStorage(),
            userPreferencesDataStore = preferences,
            externalScope = this
        )

        advanceUntilIdle()

        // Sync playback progress while offline
        val res = syncManager.syncPlaybackProgress(
            animeId = "21087",
            episodeNumber = 9,
            completed = true,
            immediate = true
        )
        assertTrue(res.isSuccess) // Graceful non-blocking success

        // Verify inserted into pending_sync queue
        val pendingCount = database.syncDao().getPendingSyncCountFlow().first()
        assertEquals(1, pendingCount)
        val pendingList = database.syncDao().getAllPendingSync()
        assertEquals(1, pendingList.size)
        assertEquals("21087", pendingList.first().animeId)
        assertEquals(9, pendingList.first().episodeNumber)

        // Reconnect network and retry
        mockApi.shouldFailNetwork = false
        val retryRes = syncManager.retryPendingSync()
        assertTrue(retryRes.isSuccess)
        assertEquals(1, retryRes.getOrDefault(0))

        // Verify pending queue is cleared
        val remainingPending = database.syncDao().getPendingSyncCountFlow().first()
        assertEquals(0, remainingPending)
        assertEquals(9, mockApi.lastUpdatedProgress)
    }

    @Test
    fun testDebouncingSuppressesDuplicateRapidSyncs() = runTest(testDispatcher) {
        val mockApi = MockAniListApi(sampleUser, sampleAnime)

        val syncManager = AniListSyncManager(
            aniListGraphQL = mockApi,
            syncDao = database.syncDao(),
            playbackDao = database.playbackDao(),
            animeDao = database.animeDao(),
            secureTokenStorage = FakeSyncSecureStorage(),
            userPreferencesDataStore = preferences,
            externalScope = this
        )

        advanceUntilIdle()

        // First call goes through
        syncManager.syncPlaybackProgress("21087", 4, false, immediate = false)
        // Rapid second call with same progress within debounce window
        syncManager.syncPlaybackProgress("21087", 4, false, immediate = false)

        advanceUntilIdle()

        assertEquals(1, mockApi.updateProgressCalls)
    }
}
