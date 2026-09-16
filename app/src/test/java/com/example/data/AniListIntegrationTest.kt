package com.example.data

import com.example.core.auth.AniListOAuthHelper
import com.example.core.auth.AniListOAuthResult
import com.example.data.local.preferences.UserPreferencesDataStore
import com.example.data.local.security.SecureTokenStorage
import com.example.data.remote.anilist.AniListGraphQL
import com.example.data.repository.AniListRepositoryImpl
import com.example.domain.model.AniListMediaListEntry
import com.example.domain.model.AniListUser
import com.example.domain.model.Anime
import com.example.domain.usecase.AuthenticateAniListUseCase
import com.example.domain.usecase.GetAniListAuthStatusUseCase
import com.example.domain.usecase.HandleAniListOAuthCallbackUseCase
import com.example.domain.usecase.LogoutAniListUseCase
import com.example.domain.usecase.SetAniListMediaStatusUseCase
import com.example.domain.usecase.SyncAniListUserListsUseCase
import com.example.domain.usecase.UpdateAniListProgressUseCase
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
import org.junit.Assert.assertFalse
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
class AniListIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleUser = AniListUser(
        id = 12345L,
        name = "KuroNeko",
        avatarUrl = "https://s4.anilist.co/user/avatar.png",
        bannerUrl = "https://s4.anilist.co/user/banner.png",
        totalAnimeWatched = 42,
        totalEpisodesWatched = 512,
        daysWatched = 8.5,
        meanScore = 8.4
    )

    private val sampleAnime = Anime(
        id = "16498",
        title = "Attack on Titan",
        romajiTitle = "Shingeki no Kyojin",
        nativeTitle = "進撃の巨人",
        score = 8.6,
        status = "FINISHED",
        format = "TV",
        totalEpisodes = 25,
        genres = listOf("Action", "Fantasy", "Drama")
    )

    class FakeSecureTokenStorage(initialToken: String? = null) : SecureTokenStorage {
        private val _tokenFlow = MutableStateFlow<String?>(initialToken)
        override val tokenFlow: Flow<String?> = _tokenFlow.asStateFlow()

        override suspend fun getAccessToken(): String? = _tokenFlow.value

        override suspend fun saveAccessToken(token: String?) {
            _tokenFlow.value = token
        }

        override suspend fun clearAccessToken() {
            _tokenFlow.value = null
        }

        override fun isStorageSecured(): Boolean = true
    }

    class FakeAniListGraphQL(private val user: AniListUser, private val anime: Anime) : AniListGraphQL() {
        var progressUpdatedMediaId: Int? = null
        var progressUpdatedVal: Int? = null
        var lastStatusSet: String? = null
        var shouldThrowError: Boolean = false

        override suspend fun getViewer(token: String): AniListUser {
            if (shouldThrowError) throw RuntimeException("Network error")
            return user
        }

        override suspend fun getUserAnimeList(
            token: String,
            userId: Long?,
            username: String?,
            status: String?
        ): List<AniListMediaListEntry> {
            if (shouldThrowError) throw RuntimeException("Network error")
            return listOf(
                AniListMediaListEntry(
                    id = 101L,
                    mediaId = anime.id.toInt(),
                    status = status ?: "CURRENT",
                    progress = 12,
                    score = 9.0,
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
            progressUpdatedMediaId = mediaId
            progressUpdatedVal = progress
            return true
        }

        override suspend fun setMediaListStatus(
            token: String,
            mediaId: Int,
            status: String,
            score: Double?,
            progress: Int?
        ): Boolean {
            lastStatusSet = status
            return true
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testOAuthHelperParseImplicitGrantSuccess() {
        val callbackUri = "animex://anilist-auth#access_token=secret_token_abc123&token_type=Bearer&expires_in=31536000"
        val result = AniListOAuthHelper.parseCallbackUri(callbackUri)

        assertTrue(result is AniListOAuthResult.TokenSuccess)
        val tokenResult = result as AniListOAuthResult.TokenSuccess
        assertEquals("secret_token_abc123", tokenResult.accessToken)
        assertEquals("Bearer", tokenResult.tokenType)
        assertEquals(31536000L, tokenResult.expiresInSeconds)
    }

    @Test
    fun testOAuthHelperParseJustAnimeScheme() {
        val callbackUri = "justanime://anilist-auth#access_token=justanime_token_789&token_type=Bearer"
        val result = AniListOAuthHelper.parseCallbackUri(callbackUri)

        assertTrue(result is AniListOAuthResult.TokenSuccess)
        val tokenResult = result as AniListOAuthResult.TokenSuccess
        assertEquals("justanime_token_789", tokenResult.accessToken)
    }

    @Test
    fun testOAuthHelperParseCodeGrant() {
        val callbackUri = "animex://anilist-auth?code=authorization_code_xyz"
        val result = AniListOAuthHelper.parseCallbackUri(callbackUri)

        assertTrue(result is AniListOAuthResult.CodeSuccess)
        val codeResult = result as AniListOAuthResult.CodeSuccess
        assertEquals("authorization_code_xyz", codeResult.authCode)
    }

    @Test
    fun testOAuthHelperParseError() {
        val callbackUri = "animex://anilist-auth?error=access_denied&error_description=User+denied+access"
        val result = AniListOAuthHelper.parseCallbackUri(callbackUri)

        assertTrue(result is AniListOAuthResult.OAuthError)
        val errorResult = result as AniListOAuthResult.OAuthError
        assertEquals("access_denied", errorResult.error)
        assertEquals("User denied access", errorResult.errorDescription)
    }

    @Test
    fun testOAuthHelperParseNonAuthUrl() {
        val callbackUri = "https://anilist.co/anime/16498"
        val result = AniListOAuthHelper.parseCallbackUri(callbackUri)

        assertTrue(result is AniListOAuthResult.NotOAuthCallback)
    }

    @Test
    fun testAuthenticateWithTokenSuccess() = runTest(testDispatcher) {
        val fakeApi = FakeAniListGraphQL(sampleUser, sampleAnime)
        val fakeContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val realPrefs = UserPreferencesDataStore(fakeContext)
        val fakeSecureStorage = FakeSecureTokenStorage()

        val repository = AniListRepositoryImpl(
            aniListApi = fakeApi,
            secureTokenStorage = fakeSecureStorage,
            preferencesDataStore = realPrefs,
            externalScope = this
        )

        advanceUntilIdle()

        val result = repository.authenticateWithToken("valid_token")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("KuroNeko", user?.name)
        assertEquals(42, user?.totalAnimeWatched)
        assertEquals(8.4, user?.meanScore ?: 0.0, 0.01)
        assertEquals("valid_token", fakeSecureStorage.getAccessToken())
    }

    @Test
    fun testHandleOAuthCallbackDeepLinkSuccess() = runTest(testDispatcher) {
        val fakeApi = FakeAniListGraphQL(sampleUser, sampleAnime)
        val fakeContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val realPrefs = UserPreferencesDataStore(fakeContext)
        val fakeSecureStorage = FakeSecureTokenStorage()

        val repository = AniListRepositoryImpl(
            aniListApi = fakeApi,
            secureTokenStorage = fakeSecureStorage,
            preferencesDataStore = realPrefs,
            externalScope = this
        )

        advanceUntilIdle()

        val callbackUri = "animex://anilist-auth#access_token=oauth_token_999&token_type=Bearer"
        val result = repository.handleOAuthCallback(callbackUri)

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("KuroNeko", user?.name)
        assertEquals("oauth_token_999", fakeSecureStorage.getAccessToken())
    }

    @Test
    fun testGetUserAnimeList() = runTest(testDispatcher) {
        val fakeApi = FakeAniListGraphQL(sampleUser, sampleAnime)
        val fakeContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val realPrefs = UserPreferencesDataStore(fakeContext)
        realPrefs.saveAniListUser(null, "KuroNeko")
        val fakeSecureStorage = FakeSecureTokenStorage("token_xyz")

        val repository = AniListRepositoryImpl(
            aniListApi = fakeApi,
            secureTokenStorage = fakeSecureStorage,
            preferencesDataStore = realPrefs,
            externalScope = this
        )

        advanceUntilIdle()

        val result = repository.getUserAnimeList("CURRENT")
        assertTrue(result.isSuccess)
        val entries = result.getOrNull()
        assertNotNull(entries)
        assertEquals(1, entries?.size)
        assertEquals(16498, entries?.first()?.mediaId)
        assertEquals("CURRENT", entries?.first()?.status)
        assertEquals(12, entries?.first()?.progress)
    }

    @Test
    fun testUpdateEpisodeProgress() = runTest(testDispatcher) {
        val fakeApi = FakeAniListGraphQL(sampleUser, sampleAnime)
        val fakeContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val realPrefs = UserPreferencesDataStore(fakeContext)
        realPrefs.saveAniListUser(null, "KuroNeko")
        val fakeSecureStorage = FakeSecureTokenStorage("token_xyz")

        val repository = AniListRepositoryImpl(
            aniListApi = fakeApi,
            secureTokenStorage = fakeSecureStorage,
            preferencesDataStore = realPrefs,
            externalScope = this
        )

        advanceUntilIdle()

        val result = repository.updateEpisodeProgress("16498", 14, false)
        assertTrue(result.isSuccess)
        assertEquals(16498, fakeApi.progressUpdatedMediaId)
        assertEquals(14, fakeApi.progressUpdatedVal)
    }

    @Test
    fun testAniListUseCasesIntegration() = runTest(testDispatcher) {
        val fakeApi = FakeAniListGraphQL(sampleUser, sampleAnime)
        val fakeContext = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val realPrefs = UserPreferencesDataStore(fakeContext)
        val fakeSecureStorage = FakeSecureTokenStorage()

        val repository = AniListRepositoryImpl(
            aniListApi = fakeApi,
            secureTokenStorage = fakeSecureStorage,
            preferencesDataStore = realPrefs,
            externalScope = this
        )

        advanceUntilIdle()

        val authUseCase = AuthenticateAniListUseCase(repository)
        val oauthCallbackUseCase = HandleAniListOAuthCallbackUseCase(repository)
        val getAuthStatusUseCase = GetAniListAuthStatusUseCase(repository)
        val syncListsUseCase = SyncAniListUserListsUseCase(repository)
        val updateProgressUseCase = UpdateAniListProgressUseCase(repository)
        val setStatusUseCase = SetAniListMediaStatusUseCase(repository)
        val logoutUseCase = LogoutAniListUseCase(repository)

        val authResult = authUseCase("sample_token")
        assertTrue(authResult.isSuccess)

        val isAuth = getAuthStatusUseCase().first()
        assertTrue(isAuth)

        val syncResult = syncListsUseCase()
        assertTrue(syncResult.isSuccess)

        val progressResult = updateProgressUseCase("16498", 5, false)
        assertTrue(progressResult.isSuccess)

        val statusResult = setStatusUseCase("16498", "COMPLETED", 9.5)
        assertTrue(statusResult.isSuccess)

        logoutUseCase()
        advanceUntilIdle()
        val afterLogoutAuth = getAuthStatusUseCase().first()
        assertFalse(afterLogoutAuth)

        // Test OAuth callback use case
        val callbackResult = oauthCallbackUseCase("animex://anilist-auth#access_token=new_oauth_token&token_type=Bearer")
        assertTrue(callbackResult.isSuccess)
        assertTrue(getAuthStatusUseCase().first())
    }
}
