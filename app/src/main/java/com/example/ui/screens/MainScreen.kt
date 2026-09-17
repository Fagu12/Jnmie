package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.JustAnimeApp
import com.example.ui.screens.player.findActivity
import com.example.ui.components.JustAnimeBottomNav
import com.example.ui.components.NavItem
import com.example.ui.navigation.NavDestinations
import com.example.ui.screens.details.AnimeDetailsScreen
import com.example.ui.screens.details.AnimeDetailsViewModel
import com.example.ui.screens.explore.ExploreScreen
import com.example.ui.screens.explore.ExploreViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.library.LibraryViewModel
import com.example.ui.screens.player.PlayerScreen
import com.example.ui.screens.player.PlayerViewModel
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.search.SearchViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SubtitleSettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.DarkBackground

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as JustAnimeApp
    val container = app.container

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val settings by container.settingsRepository.settingsFlow.collectAsState(
        initial = com.example.core.interfaces.AppSettings()
    )

    // Determine current bottom navigation tab
    val currentTab = when (currentRoute) {
        NavDestinations.HOME -> NavItem.HOME
        NavDestinations.EXPLORE -> NavItem.EXPLORE
        NavDestinations.LIBRARY -> NavItem.LIBRARY
        NavDestinations.SETTINGS -> NavItem.SETTINGS
        else -> NavItem.HOME
    }

    // Hide bottom navigation on player, details, and search
    val showBottomNav = currentRoute in listOf(
        NavDestinations.HOME,
        NavDestinations.EXPLORE,
        NavDestinations.LIBRARY,
        NavDestinations.SETTINGS
    )

    // Strictly restore portrait orientation and system bars whenever leaving the player
    val isPlayerScreen = currentRoute?.startsWith("player") == true
    DisposableEffect(isPlayerScreen) {
        if (!isPlayerScreen) {
            val act = context.findActivity()
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val win = act?.window
            if (win != null) {
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        NavHost(
            navController = navController,
            startDestination = NavDestinations.HOME,
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. HOME SCREEN
            composable(NavDestinations.HOME) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(
                        container.animeRepository,
                        container.playbackRepository,
                        container.aniListRepository,
                        container.settingsRepository
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAnime = { animeId ->
                        navController.navigate(NavDestinations.detailsRoute(animeId))
                    },
                    onPlayEpisode = { animeId, epNum ->
                        navController.navigate(NavDestinations.playerRoute(animeId, epNum))
                    },
                    onNavigateToSettings = {
                        navController.navigate(NavDestinations.SETTINGS)
                    },
                    onNavigateToExtensions = {
                        navController.navigate(NavDestinations.EXTENSIONS)
                    },
                    onNavigateToSearch = {
                        navController.navigate(NavDestinations.SEARCH)
                    },
                    onNavigateToCatalog = {
                        navController.navigate(NavDestinations.EXPLORE)
                    }
                )
            }

            // 2. EXPLORE SCREEN
            composable(NavDestinations.EXPLORE) {
                val exploreViewModel: ExploreViewModel = viewModel(
                    factory = ExploreViewModel.Factory(
                        container.animeRepository,
                        container.aniListRepository,
                        container.settingsRepository
                    )
                )
                ExploreScreen(
                    viewModel = exploreViewModel,
                    onNavigateToAnime = { animeId ->
                        navController.navigate(NavDestinations.detailsRoute(animeId))
                    },
                    onNavigateToSearch = {
                        navController.navigate(NavDestinations.SEARCH)
                    }
                )
            }

            // 3. LIBRARY SCREEN
            composable(NavDestinations.LIBRARY) {
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(
                        container.animeRepository,
                        container.playbackRepository,
                        container.aniListRepository,
                        container.settingsRepository
                    )
                )
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onNavigateToAnime = { animeId ->
                        navController.navigate(NavDestinations.detailsRoute(animeId))
                    },
                    onPlayEpisode = { animeId, epNum ->
                        navController.navigate(NavDestinations.playerRoute(animeId, epNum))
                    },
                    onNavigateToSearch = {
                        navController.navigate(NavDestinations.SEARCH)
                    }
                )
            }

            // 5. SETTINGS SCREEN
            composable(NavDestinations.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        container.settingsRepository,
                        container.aniListRepository,
                        container.playbackRepository,
                        container.providerManager
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSubtitleSettings = {
                        navController.navigate(NavDestinations.SUBTITLE_SETTINGS)
                    }
                )
            }

            composable(NavDestinations.SUBTITLE_SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        container.settingsRepository,
                        container.aniListRepository,
                        container.playbackRepository,
                        container.providerManager
                    )
                )
                SubtitleSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 6. SEARCH SCREEN
            composable(NavDestinations.SEARCH) {
                val searchViewModel: SearchViewModel = viewModel(
                    factory = SearchViewModel.Factory(
                        container.searchRepository,
                        container.settingsRepository
                    )
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateToAnime = { animeId ->
                        navController.navigate(NavDestinations.detailsRoute(animeId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 7. ANIME DETAILS SCREEN
            composable(
                route = NavDestinations.DETAILS,
                arguments = listOf(navArgument("animeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                val detailsViewModel: AnimeDetailsViewModel = viewModel(
                    factory = AnimeDetailsViewModel.Factory(
                        animeId = animeId,
                        animeRepository = container.animeRepository,
                        playbackRepository = container.playbackRepository,
                        providerManager = container.providerManager,
                        settingsRepository = container.settingsRepository,
                        aniListRepository = container.aniListRepository,
                        sourceResolver = container.sourceResolver
                    )
                )
                AnimeDetailsScreen(
                    viewModel = detailsViewModel,
                    onBack = { navController.popBackStack() },
                    onPlayEpisode = { aId, epNum ->
                        navController.navigate(NavDestinations.playerRoute(aId, epNum))
                    },
                    onNavigateToAnime = { targetId ->
                        navController.navigate(NavDestinations.detailsRoute(targetId))
                    }
                )
            }

            // 8. PLAYER SCREEN
            composable(
                route = NavDestinations.PLAYER,
                arguments = listOf(
                    navArgument("animeId") { type = NavType.StringType },
                    navArgument("episodeNumber") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                val epNum = backStackEntry.arguments?.getInt("episodeNumber") ?: 1
                val playerViewModel: PlayerViewModel = viewModel(
                    factory = PlayerViewModel.Factory(
                        context = context,
                        animeId = animeId,
                        episodeNumber = epNum,
                        animeRepository = container.animeRepository,
                        sourceResolver = container.sourceResolver,
                        playbackRepository = container.playbackRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                PlayerScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Floating Bottom Navigation Bar
        AnimatedVisibility(
            visible = showBottomNav,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            JustAnimeBottomNav(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    val destination = when (tab) {
                        NavItem.HOME -> NavDestinations.HOME
                        NavItem.EXPLORE -> NavDestinations.EXPLORE
                        NavItem.LIBRARY -> NavDestinations.LIBRARY
                        NavItem.SETTINGS -> NavDestinations.SETTINGS
                    }
                    navController.navigate(destination) {
                        popUpTo(NavDestinations.HOME) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                style = settings.navBarStyle
            )
        }
    }
}
