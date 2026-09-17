package com.example.ui.navigation

object NavDestinations {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val LIBRARY = "library"
    const val EXTENSIONS = "extensions"
    const val SETTINGS = "settings"
    const val SUBTITLE_SETTINGS = "subtitle_settings"
    const val SEARCH = "search"
    const val DETAILS = "details/{animeId}"
    const val PLAYER = "player/{animeId}/{episodeNumber}"

    fun detailsRoute(animeId: String): String = "details/$animeId"
    fun playerRoute(animeId: String, episodeNumber: Int): String = "player/$animeId/$episodeNumber"
}
