package com.example.data.mock

import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.model.PlaybackProgress

/**
 * High-fidelity curated mock and sample dataset structured around domain models.
 * Used for instant first-load rendering, offline development, and fallback.
 */
object MockAnimeData {

    val featuredAnimeList: List<Anime> = listOf(
        Anime(
            id = "171018",
            title = "DAN DA DAN",
            romajiTitle = "Dandadan",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/171018-8m58zV7iW3iM.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx171018-n3fPj40sL7z8.jpg",
            description = "Momo Ayase strikes up an unusual friendship with her school's UFO fanatic, whom she nicknames Okarun. While Momo believes in spirits, she thinks aliens are utter nonsense.",
            score = 8.7,
            status = "RELEASING",
            format = "TV",
            studio = "Science SARU",
            season = "FALL",
            seasonYear = 2024,
            genres = listOf("Action", "Comedy", "Sci-Fi", "Supernatural"),
            totalEpisodes = 12,
            currentEpisodeCount = 8
        ),
        Anime(
            id = "151807",
            title = "Solo Leveling",
            romajiTitle = "Ore dake Level Up na Ken",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/151807-35t4ZgB5j2H1.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151807-m1gkuoiB8p8Y.png",
            description = "They say whatever doesn't kill you makes you stronger, but that's not the case for Sung Jinwoo. When a perilous dual dungeon opens, his destiny changes forever.",
            score = 8.5,
            status = "FINISHED",
            format = "TV",
            studio = "A-1 Pictures",
            season = "WINTER",
            seasonYear = 2024,
            genres = listOf("Action", "Adventure", "Fantasy"),
            totalEpisodes = 12,
            currentEpisodeCount = 12
        ),
        Anime(
            id = "154587",
            title = "Bleach: Thousand-Year Blood War",
            romajiTitle = "Bleach: Sennen Kessen-hen",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-5z9718H6oD4L.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-dC0qIsqW2m7S.jpg",
            description = "The conflict between the Soul Reapers and the Quincies erupts in the final battle for the fate of Soul Society and the living world.",
            score = 8.9,
            status = "RELEASING",
            format = "TV",
            studio = "Pierrot",
            season = "FALL",
            seasonYear = 2024,
            genres = listOf("Action", "Adventure", "Supernatural"),
            totalEpisodes = 13,
            currentEpisodeCount = 9
        ),
        Anime(
            id = "154587_frieren",
            title = "Frieren: Beyond Journey's End",
            romajiTitle = "Sousou no Frieren",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-5z9718H6oD4L.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-n4WJmGZ6wZcR.png",
            description = "The adventure is over, but life goes on for an elf mage just beginning to learn what living actually means.",
            score = 9.2,
            status = "FINISHED",
            format = "TV",
            studio = "Madhouse",
            season = "FALL",
            seasonYear = 2023,
            genres = listOf("Adventure", "Drama", "Fantasy"),
            totalEpisodes = 28,
            currentEpisodeCount = 28
        ),
        Anime(
            id = "153288",
            title = "Kaiju No. 8",
            romajiTitle = "Kaijuu 8-gou",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/153288-p8yv8YxYn33e.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx153288-6pQkYd11E81u.png",
            description = "Kafka Hibino aspires to join the Defense Force to keep a promise with his childhood friend, despite monstrous odds.",
            score = 8.3,
            status = "FINISHED",
            format = "TV",
            studio = "Production I.G",
            season = "SPRING",
            seasonYear = 2024,
            genres = listOf("Action", "Sci-Fi"),
            totalEpisodes = 12,
            currentEpisodeCount = 12
        )
    )

    val continueWatchingList: List<PlaybackProgress> = listOf(
        PlaybackProgress(
            animeId = "171018",
            animeTitle = "DAN DA DAN",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/171018-8m58zV7iW3iM.jpg",
            episodeNumber = 6,
            episodeTitle = "Episode 6: Turbo Granny Strikes Back",
            currentPositionMs = 14 * 60 * 1000L + 25 * 1000L,
            durationMs = 23 * 60 * 1000L + 45 * 1000L,
            lastWatchedTimestamp = System.currentTimeMillis() - 1000 * 60 * 30
        ),
        PlaybackProgress(
            animeId = "151807",
            animeTitle = "Solo Leveling",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/151807-35t4ZgB5j2H1.jpg",
            episodeNumber = 8,
            episodeTitle = "Episode 8: This Is Frustrating",
            currentPositionMs = 18 * 60 * 1000L + 10 * 1000L,
            durationMs = 24 * 60 * 1000L,
            lastWatchedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3
        ),
        PlaybackProgress(
            animeId = "21",
            animeTitle = "One Piece",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJHsZl.jpg",
            episodeNumber = 1120,
            episodeTitle = "Episode 1120: The World Trembles! Five Elders Assemble",
            currentPositionMs = 7 * 60 * 1000L + 40 * 1000L,
            durationMs = 23 * 60 * 1000L + 50 * 1000L,
            lastWatchedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 24
        ),
        PlaybackProgress(
            animeId = "154587",
            animeTitle = "Bleach: Thousand-Year Blood War",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-5z9718H6oD4L.jpg",
            episodeNumber = 5,
            episodeTitle = "Episode 5: Against the Judgement",
            currentPositionMs = 21 * 60 * 1000L,
            durationMs = 24 * 60 * 1000L,
            lastWatchedTimestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48
        )
    )

    val trendingAnime: List<Anime> = listOf(
        Anime(
            id = "171018",
            title = "DAN DA DAN",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/171018-8m58zV7iW3iM.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx171018-n3fPj40sL7z8.jpg",
            description = "Momo Ayase strikes up an unusual friendship with her school's UFO fanatic, whom she nicknames Okarun.",
            score = 8.7,
            status = "RELEASING",
            format = "TV",
            studio = "Science SARU",
            season = "FALL",
            seasonYear = 2024,
            genres = listOf("Action", "Comedy", "Sci-Fi", "Supernatural"),
            totalEpisodes = 12
        ),
        Anime(
            id = "151807",
            title = "Solo Leveling",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/151807-35t4ZgB5j2H1.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151807-m1gkuoiB8p8Y.png",
            description = "Whatever doesn't kill you makes you stronger for Sung Jinwoo.",
            score = 8.5,
            status = "FINISHED",
            format = "TV",
            studio = "A-1 Pictures",
            season = "WINTER",
            seasonYear = 2024,
            genres = listOf("Action", "Adventure", "Fantasy"),
            totalEpisodes = 12
        ),
        Anime(
            id = "113415",
            title = "Jujutsu Kaisen Season 2",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/113415-jQBSkxWAAk83.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx113415-bbBWj4pCeJF培养.jpg",
            description = "The past comes to light as Satoru Gojo and Suguru Geto take on a mission that forever alters their futures.",
            score = 8.8,
            status = "FINISHED",
            format = "TV",
            studio = "MAPPA",
            season = "SUMMER",
            seasonYear = 2023,
            genres = listOf("Action", "Fantasy", "Supernatural"),
            totalEpisodes = 23
        ),
        Anime(
            id = "101922",
            title = "Demon Slayer: Hashira Training Arc",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/101922-bI2Xn3JmS9K9.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101922-P3N7B3gT9Qp1.jpg",
            description = "Tanjiro goes to see the Stone Hashira, Himejima, who intends to prepare him for the battles to come.",
            score = 8.4,
            status = "FINISHED",
            format = "TV",
            studio = "ufotable",
            season = "SPRING",
            seasonYear = 2024,
            genres = listOf("Action", "Fantasy", "Historical"),
            totalEpisodes = 8
        ),
        Anime(
            id = "127230",
            title = "Chainsaw Man",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/127230-T57E779183qW.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx127230-FloXNypyoFzA.png",
            description = "Denji is a young man living a poverty-stricken life, paying off his deceased father's debt by harvesting devil corpses.",
            score = 8.6,
            status = "FINISHED",
            format = "TV",
            studio = "MAPPA",
            season = "FALL",
            seasonYear = 2022,
            genres = listOf("Action", "Supernatural", "Gore"),
            totalEpisodes = 12
        ),
        Anime(
            id = "21",
            title = "One Piece",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJHsZl.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21-YCDoj1EkAxFn.jpg",
            description = "Monkey D. Luffy sets sail across the Grand Line in search of the legendary One Piece.",
            score = 8.8,
            status = "RELEASING",
            format = "TV",
            studio = "Toei Animation",
            season = "FALL",
            seasonYear = 1999,
            genres = listOf("Action", "Adventure", "Comedy", "Fantasy"),
            totalEpisodes = 1120
        )
    )

    val popularThisSeason: List<Anime> = listOf(
        Anime(
            id = "154587",
            title = "Bleach: Thousand-Year Blood War",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587-5z9718H6oD4L.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-dC0qIsqW2m7S.jpg",
            description = "The conflict between the Soul Reapers and the Quincies erupts in the final battle for Soul Society.",
            score = 8.9,
            status = "RELEASING",
            format = "TV",
            studio = "Pierrot",
            season = "FALL",
            seasonYear = 2024,
            genres = listOf("Action", "Adventure", "Supernatural"),
            totalEpisodes = 13
        ),
        Anime(
            id = "171018",
            title = "DAN DA DAN",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/171018-8m58zV7iW3iM.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx171018-n3fPj40sL7z8.jpg",
            description = "Momo Ayase and Okarun navigate strange encounters of ghosts and extraterrestrials.",
            score = 8.7,
            status = "RELEASING",
            format = "TV",
            studio = "Science SARU",
            season = "FALL",
            seasonYear = 2024,
            genres = listOf("Action", "Comedy", "Sci-Fi", "Supernatural"),
            totalEpisodes = 12
        ),
        Anime(
            id = "163132",
            title = "Re:ZERO -Starting Life in Another World- Season 3",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/163132-B8L682618KjE.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx163132-j76H5J8n7qK0.jpg",
            description = "Subaru Natsuki's grueling struggle continues in Priestella, the City of Watergates.",
            score = 8.6,
            status = "RELEASING",
            format = "TV",
            studio = "White Fox",
            season = "FALL",
            seasonYear = 2024,
            genres = listOf("Drama", "Fantasy", "Psychological", "Thriller"),
            totalEpisodes = 16
        ),
        Anime(
            id = "166531",
            title = "Blue Lock vs. U-20 JAPAN",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/166531-u799K1jM25oA.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx166531-W2XN65A5L0K1.jpg",
            description = "The decisive match between the Blue Lock project and the Japan U-20 National Team kicks off.",
            score = 8.2,
            status = "RELEASING",
            format = "TV",
            studio = "8bit",
            season = "FALL",
            seasonYear = 2024,
            genres = listOf("Action", "Sports"),
            totalEpisodes = 14
        ),
        Anime(
            id = "170942",
            title = "Ranma 1/2 (2024)",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/170942-A8N651K8H72.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx170942-892K105Lm10.jpg",
            description = "Ranma Saotome and Akane Tendo navigate martial arts, curses, and arranged marriages in this modern remake.",
            score = 7.9,
            status = "RELEASING",
            format = "TV",
            studio = "MAPPA",
            season = "FALL",
            seasonYear = 2024,
            genres = listOf("Action", "Comedy", "Romance"),
            totalEpisodes = 12
        )
    )

    val recentlyUpdated: List<Anime> = listOf(
        Anime(
            id = "146065",
            title = "Mushoku Tensei: Jobless Reincarnation Season 2",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/146065-YQ26rJt87w9G.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx146065-kZ34W4tY76Pj.jpg",
            description = "Rudeus Greyrat continues his journey across the Magic Continent.",
            score = 8.4,
            status = "FINISHED",
            format = "TV",
            studio = "Studio Bind",
            season = "SPRING",
            seasonYear = 2024,
            genres = listOf("Action", "Adventure", "Drama", "Fantasy"),
            totalEpisodes = 24
        ),
        Anime(
            id = "153288",
            title = "Kaiju No. 8",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/153288-p8yv8YxYn33e.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx153288-6pQkYd11E81u.png",
            description = "Kafka Hibino transforms to fight giant monsters threatening humanity.",
            score = 8.3,
            status = "FINISHED",
            format = "TV",
            studio = "Production I.G",
            season = "SPRING",
            seasonYear = 2024,
            genres = listOf("Action", "Sci-Fi"),
            totalEpisodes = 12
        ),
        Anime(
            id = "150672",
            title = "Oshi no Ko Season 2",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/150672-4y5N38k1H89.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx150672-882Km1L80K92.jpg",
            description = "Aqua and Ruby dive deeper into the showbiz labyrinth through the Tokyo Blade 2.5D stage play.",
            score = 8.5,
            status = "FINISHED",
            format = "TV",
            studio = "Doga Kobo",
            season = "SUMMER",
            seasonYear = 2024,
            genres = listOf("Drama", "Mystery", "Supernatural"),
            totalEpisodes = 13
        ),
        Anime(
            id = "129201",
            title = "Tokyo Revengers",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/129201-9U9fN3KjG8S7.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx129201-j7c1bFzKqQ2Y.jpg",
            description = "Takemichi Hanagaki leaps through time to save the friends he holds dear.",
            score = 7.7,
            status = "FINISHED",
            format = "TV",
            studio = "LIDENFILMS",
            season = "SPRING",
            seasonYear = 2021,
            genres = listOf("Action", "Drama", "Supernatural"),
            totalEpisodes = 24
        ),
        Anime(
            id = "140960",
            title = "Spy x Family",
            bannerUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/banner/140960-wY71O8269V.jpg",
            coverUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx140960-A10992381K.jpg",
            description = "A spy on an undercover mission marries a telepathic assassin, creating the world's most chaotic fake family.",
            score = 8.5,
            status = "FINISHED",
            format = "TV",
            studio = "WIT Studio & CloverWorks",
            season = "SPRING",
            seasonYear = 2022,
            genres = listOf("Action", "Comedy"),
            totalEpisodes = 25
        )
    )

    val browsingCategories: List<String> = listOf(
        "All",
        "Action",
        "Adventure",
        "Fantasy",
        "Sci-Fi",
        "Comedy",
        "Drama",
        "Romance",
        "Supernatural",
        "Mystery",
        "Sports"
    )

    val popularSearchTags: List<String> = listOf(
        "Solo Leveling",
        "DAN DA DAN",
        "Bleach",
        "One Piece",
        "Frieren",
        "Jujutsu Kaisen",
        "Demon Slayer",
        "Chainsaw Man",
        "Spy x Family"
    )

    val allCatalogAnime: List<Anime> by lazy {
        (featuredAnimeList + trendingAnime + popularThisSeason + recentlyUpdated)
            .distinctBy { it.id }
    }

    fun searchCatalog(
        query: String,
        genre: String? = null,
        format: String? = null
    ): List<Anime> {
        val cleanQuery = query.trim().lowercase()
        return allCatalogAnime.filter { anime ->
            val matchesQuery = cleanQuery.isEmpty() ||
                anime.title.lowercase().contains(cleanQuery) ||
                (anime.romajiTitle?.lowercase()?.contains(cleanQuery) == true) ||
                (anime.description?.lowercase()?.contains(cleanQuery) == true) ||
                (anime.studio?.lowercase()?.contains(cleanQuery) == true) ||
                anime.genres.any { it.lowercase().contains(cleanQuery) }

            val matchesGenre = genre == null || genre.equals("All", ignoreCase = true) ||
                anime.genres.any { it.equals(genre, ignoreCase = true) }

            val matchesFormat = format == null || format.equals("All", ignoreCase = true) ||
                anime.format?.equals(format, ignoreCase = true) == true

            matchesQuery && matchesGenre && matchesFormat
        }
    }
}
