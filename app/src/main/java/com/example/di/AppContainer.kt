package com.example.di

import android.content.Context
import com.example.core.config.EnvironmentConfig
import com.example.core.dispatcher.DefaultDispatcherProvider
import com.example.core.dispatcher.DispatcherProvider
import com.example.data.local.db.AppDatabase
import com.example.data.local.preferences.UserPreferencesDataStore
import com.example.data.local.security.AndroidKeyStoreSecureTokenStorage
import com.example.data.local.security.SecureTokenStorage
import com.example.data.remote.anilist.AniListGraphQL
import com.example.data.remote.extension.SafeExtensionEngine
import com.example.data.repository.AniListRepositoryImpl
import com.example.data.repository.AnimeRepositoryImpl
import com.example.data.repository.ExtensionManagerImpl
import com.example.data.repository.ExtensionRepositoryImpl
import com.example.data.repository.PlaybackRepositoryImpl
import com.example.data.repository.SearchRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.data.repository.SourceResolverImpl
import com.example.domain.repository.AniListRepository
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.ExtensionManager
import com.example.domain.repository.ExtensionRepository
import com.example.domain.repository.PlaybackRepository
import com.example.domain.repository.SearchRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SourceResolver
import com.example.domain.usecase.AddRepositoryUseCase
import com.example.domain.usecase.AuthenticateAniListUseCase
import com.example.domain.usecase.ClearWatchHistoryUseCase
import com.example.domain.usecase.GetAniListUserUseCase
import com.example.domain.usecase.GetAnimeDetailsUseCase
import com.example.domain.usecase.GetContinueWatchingUseCase
import com.example.domain.usecase.GetEpisodesUseCase
import com.example.domain.usecase.GetFavoriteAnimeUseCase
import com.example.domain.usecase.GetInstalledExtensionsUseCase
import com.example.domain.usecase.GetPopularSeasonAnimeUseCase
import com.example.domain.usecase.GetRecentlyUpdatedAnimeUseCase
import com.example.domain.usecase.GetRepositoriesUseCase
import com.example.domain.usecase.GetSettingsUseCase
import com.example.domain.usecase.GetTrendingAnimeUseCase
import com.example.domain.usecase.GetWatchHistoryUseCase
import com.example.domain.usecase.SavePlaybackProgressUseCase
import com.example.domain.usecase.SearchAnimeUseCase
import com.example.domain.usecase.SyncAniListUserListsUseCase
import com.example.domain.usecase.ToggleFavoriteUseCase
import com.example.domain.usecase.UpdateSettingsUseCase

/**
 * Dependency Injection Container Interface.
 * Acts as the centralized composition root for the application foundation,
 * providing access to data stores, database, repositories, use cases, and dispatchers.
 */
interface AppContainer {
    val dispatchers: DispatcherProvider
    val environmentConfig: EnvironmentConfig
    val userPreferencesDataStore: UserPreferencesDataStore
    val secureTokenStorage: SecureTokenStorage
    val database: AppDatabase

    // Domain Repositories
    val animeRepository: AnimeRepository
    val aniListRepository: AniListRepository
    val extensionRepository: ExtensionRepository
    val extensionManager: ExtensionManager
    val sourceResolver: SourceResolver
    val playbackRepository: PlaybackRepository
    val searchRepository: SearchRepository
    val settingsRepository: SettingsRepository

    // Domain Use Cases
    val getTrendingAnimeUseCase: GetTrendingAnimeUseCase
    val getPopularSeasonAnimeUseCase: GetPopularSeasonAnimeUseCase
    val getRecentlyUpdatedAnimeUseCase: GetRecentlyUpdatedAnimeUseCase
    val searchAnimeUseCase: SearchAnimeUseCase
    val getAnimeDetailsUseCase: GetAnimeDetailsUseCase
    val getEpisodesUseCase: GetEpisodesUseCase
    val getFavoriteAnimeUseCase: GetFavoriteAnimeUseCase
    val toggleFavoriteUseCase: ToggleFavoriteUseCase

    val getContinueWatchingUseCase: GetContinueWatchingUseCase
    val getWatchHistoryUseCase: GetWatchHistoryUseCase
    val savePlaybackProgressUseCase: SavePlaybackProgressUseCase
    val clearWatchHistoryUseCase: ClearWatchHistoryUseCase

    val getSettingsUseCase: GetSettingsUseCase
    val updateSettingsUseCase: UpdateSettingsUseCase

    val getRepositoriesUseCase: GetRepositoriesUseCase
    val addRepositoryUseCase: AddRepositoryUseCase
    val getInstalledExtensionsUseCase: GetInstalledExtensionsUseCase

    val getAniListUserUseCase: GetAniListUserUseCase
    val authenticateAniListUseCase: AuthenticateAniListUseCase
    val handleAniListOAuthCallbackUseCase: com.example.domain.usecase.HandleAniListOAuthCallbackUseCase
    val syncAniListUserListsUseCase: SyncAniListUserListsUseCase
    val getUserAnimeListUseCase: com.example.domain.usecase.GetUserAnimeListUseCase
    val updateAniListProgressUseCase: com.example.domain.usecase.UpdateAniListProgressUseCase
    val setAniListMediaStatusUseCase: com.example.domain.usecase.SetAniListMediaStatusUseCase
    val getAniListSyncStatusUseCase: com.example.domain.usecase.GetAniListSyncStatusUseCase
    val getPendingSyncCountUseCase: com.example.domain.usecase.GetPendingSyncCountUseCase
    val retryPendingSyncUseCase: com.example.domain.usecase.RetryPendingSyncUseCase
    val clearPendingSyncUseCase: com.example.domain.usecase.ClearPendingSyncUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val dispatchers: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    override val environmentConfig: EnvironmentConfig by lazy {
        EnvironmentConfig.default()
    }

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val userPreferencesDataStore: UserPreferencesDataStore by lazy {
        UserPreferencesDataStore(context)
    }

    override val secureTokenStorage: SecureTokenStorage by lazy {
        AndroidKeyStoreSecureTokenStorage(context)
    }

    private val aniListGraphQL: AniListGraphQL by lazy {
        AniListGraphQL()
    }

    private val safeExtensionEngine: SafeExtensionEngine by lazy {
        SafeExtensionEngine()
    }

    val aniListSyncManager: com.example.data.sync.AniListSyncManager by lazy {
        com.example.data.sync.AniListSyncManager(
            aniListGraphQL = aniListGraphQL,
            syncDao = database.syncDao(),
            playbackDao = database.playbackDao(),
            animeDao = database.animeDao(),
            secureTokenStorage = secureTokenStorage,
            userPreferencesDataStore = userPreferencesDataStore
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(userPreferencesDataStore)
    }

    override val aniListRepository: AniListRepository by lazy {
        AniListRepositoryImpl(
            aniListApi = aniListGraphQL,
            secureTokenStorage = secureTokenStorage,
            preferencesDataStore = userPreferencesDataStore,
            syncManager = aniListSyncManager
        )
    }

    override val animeRepository: AnimeRepository by lazy {
        AnimeRepositoryImpl(aniListGraphQL, database.animeDao(), database.playbackDao())
    }

    override val extensionRepository: ExtensionRepository by lazy {
        ExtensionRepositoryImpl(database.extensionDao(), safeExtensionEngine)
    }

    override val extensionManager: ExtensionManager by lazy {
        ExtensionManagerImpl(database.extensionDao(), safeExtensionEngine)
    }

    override val sourceResolver: SourceResolver by lazy {
        SourceResolverImpl(extensionManager, safeExtensionEngine)
    }

    override val playbackRepository: PlaybackRepository by lazy {
        PlaybackRepositoryImpl(database.playbackDao(), aniListRepository)
    }

    override val searchRepository: SearchRepository by lazy {
        SearchRepositoryImpl(animeRepository)
    }

    // Lazy Use Cases
    override val getTrendingAnimeUseCase: GetTrendingAnimeUseCase by lazy {
        GetTrendingAnimeUseCase(animeRepository)
    }
    override val getPopularSeasonAnimeUseCase: GetPopularSeasonAnimeUseCase by lazy {
        GetPopularSeasonAnimeUseCase(animeRepository)
    }
    override val getRecentlyUpdatedAnimeUseCase: GetRecentlyUpdatedAnimeUseCase by lazy {
        GetRecentlyUpdatedAnimeUseCase(animeRepository)
    }
    override val searchAnimeUseCase: SearchAnimeUseCase by lazy {
        SearchAnimeUseCase(animeRepository)
    }
    override val getAnimeDetailsUseCase: GetAnimeDetailsUseCase by lazy {
        GetAnimeDetailsUseCase(animeRepository)
    }
    override val getEpisodesUseCase: GetEpisodesUseCase by lazy {
        GetEpisodesUseCase(animeRepository)
    }
    override val getFavoriteAnimeUseCase: GetFavoriteAnimeUseCase by lazy {
        GetFavoriteAnimeUseCase(animeRepository)
    }
    override val toggleFavoriteUseCase: ToggleFavoriteUseCase by lazy {
        ToggleFavoriteUseCase(animeRepository)
    }

    override val getContinueWatchingUseCase: GetContinueWatchingUseCase by lazy {
        GetContinueWatchingUseCase(playbackRepository)
    }
    override val getWatchHistoryUseCase: GetWatchHistoryUseCase by lazy {
        GetWatchHistoryUseCase(playbackRepository)
    }
    override val savePlaybackProgressUseCase: SavePlaybackProgressUseCase by lazy {
        SavePlaybackProgressUseCase(playbackRepository)
    }
    override val clearWatchHistoryUseCase: ClearWatchHistoryUseCase by lazy {
        ClearWatchHistoryUseCase(playbackRepository)
    }

    override val getSettingsUseCase: GetSettingsUseCase by lazy {
        GetSettingsUseCase(settingsRepository)
    }
    override val updateSettingsUseCase: UpdateSettingsUseCase by lazy {
        UpdateSettingsUseCase(settingsRepository)
    }

    override val getRepositoriesUseCase: GetRepositoriesUseCase by lazy {
        GetRepositoriesUseCase(extensionRepository)
    }
    override val addRepositoryUseCase: AddRepositoryUseCase by lazy {
        AddRepositoryUseCase(extensionRepository)
    }
    override val getInstalledExtensionsUseCase: GetInstalledExtensionsUseCase by lazy {
        GetInstalledExtensionsUseCase(extensionManager)
    }

    override val getAniListUserUseCase: GetAniListUserUseCase by lazy {
        GetAniListUserUseCase(aniListRepository)
    }
    override val authenticateAniListUseCase: AuthenticateAniListUseCase by lazy {
        AuthenticateAniListUseCase(aniListRepository)
    }
    override val handleAniListOAuthCallbackUseCase: com.example.domain.usecase.HandleAniListOAuthCallbackUseCase by lazy {
        com.example.domain.usecase.HandleAniListOAuthCallbackUseCase(aniListRepository)
    }
    override val syncAniListUserListsUseCase: SyncAniListUserListsUseCase by lazy {
        SyncAniListUserListsUseCase(aniListRepository)
    }
    override val getUserAnimeListUseCase: com.example.domain.usecase.GetUserAnimeListUseCase by lazy {
        com.example.domain.usecase.GetUserAnimeListUseCase(aniListRepository)
    }
    override val updateAniListProgressUseCase: com.example.domain.usecase.UpdateAniListProgressUseCase by lazy {
        com.example.domain.usecase.UpdateAniListProgressUseCase(aniListRepository)
    }
    override val setAniListMediaStatusUseCase: com.example.domain.usecase.SetAniListMediaStatusUseCase by lazy {
        com.example.domain.usecase.SetAniListMediaStatusUseCase(aniListRepository)
    }
    override val getAniListSyncStatusUseCase: com.example.domain.usecase.GetAniListSyncStatusUseCase by lazy {
        com.example.domain.usecase.GetAniListSyncStatusUseCase(aniListRepository)
    }
    override val getPendingSyncCountUseCase: com.example.domain.usecase.GetPendingSyncCountUseCase by lazy {
        com.example.domain.usecase.GetPendingSyncCountUseCase(aniListRepository)
    }
    override val retryPendingSyncUseCase: com.example.domain.usecase.RetryPendingSyncUseCase by lazy {
        com.example.domain.usecase.RetryPendingSyncUseCase(aniListRepository)
    }
    override val clearPendingSyncUseCase: com.example.domain.usecase.ClearPendingSyncUseCase by lazy {
        com.example.domain.usecase.ClearPendingSyncUseCase(aniListRepository)
    }
}
