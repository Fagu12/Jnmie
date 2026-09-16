package com.example.data.remote.anilist

import com.example.domain.model.AniListMediaListEntry
import com.example.domain.model.AniListUser
import com.example.domain.model.Anime
import com.example.domain.model.AnimeCharacter
import com.example.domain.model.AnimeExternalLink
import com.example.domain.model.AnimeRecommendation
import com.example.domain.model.AnimeRelation
import com.example.domain.model.AnimeStaff
import com.example.domain.model.AnimeTag
import com.example.domain.model.AnimeTrailer
import com.example.domain.model.FuzzyDate
import com.example.domain.model.NextAiringEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Production-grade AniList GraphQL API client for Android.
 * Communicates directly with the official AniList GraphQL endpoint (https://graphql.anilist.co).
 * Maps GraphQL responses cleanly into the internal domain model.
 */
open class AniListGraphQL(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val GRAPHQL_URL = "https://graphql.anilist.co"
        const val OAUTH_AUTHORIZE_URL = "https://anilist.co/api/v2/oauth/authorize"

        // Common media query fields for lists
        private val LIST_MEDIA_FIELDS = """
            id
            idMal
            title {
              romaji
              english
              native
              userPreferred
            }
            synonyms
            coverImage {
              extraLarge
              large
              medium
              color
            }
            bannerImage
            description(asHtml: false)
            averageScore
            meanScore
            popularity
            favourites
            status
            format
            studios(isMain: true) {
              nodes {
                id
                name
              }
            }
            season
            seasonYear
            genres
            episodes
            duration
            countryOfOrigin
            source
            nextAiringEpisode {
              episode
              airingAt
              timeUntilAiring
            }
        """.trimIndent()

        // Full detailed media fields including relations, recommendations, characters, staff, links, tags
        private val DETAIL_MEDIA_FIELDS = """
            id
            idMal
            title {
              romaji
              english
              native
              userPreferred
            }
            synonyms
            coverImage {
              extraLarge
              large
              medium
              color
            }
            bannerImage
            description(asHtml: false)
            averageScore
            meanScore
            popularity
            favourites
            status
            format
            studios {
              edges {
                isMain
                node {
                  id
                  name
                  isAnimationStudio
                  siteUrl
                }
              }
            }
            season
            seasonYear
            genres
            tags {
              id
              name
              description
              category
              rank
              isMediaSpoiler
            }
            episodes
            duration
            countryOfOrigin
            source
            startDate {
              year
              month
              day
            }
            endDate {
              year
              month
              day
            }
            nextAiringEpisode {
              episode
              airingAt
              timeUntilAiring
            }
            trailer {
              id
              site
              thumbnail
            }
            relations {
              edges {
                relationType
                node {
                  id
                  type
                  title {
                    romaji
                    english
                    native
                  }
                  format
                  status
                  coverImage {
                    large
                    medium
                  }
                  seasonYear
                  episodes
                }
              }
            }
            recommendations(sort: RATING_DESC, perPage: 10) {
              nodes {
                rating
                userRating
                mediaRecommendation {
                  id
                  type
                  title {
                    romaji
                    english
                    native
                  }
                  coverImage {
                    large
                    medium
                  }
                  format
                  status
                  averageScore
                  seasonYear
                  episodes
                }
              }
            }
            characters(sort: [ROLE, RELEVANCE, ID], perPage: 12) {
              edges {
                role
                node {
                  id
                  name {
                    full
                    native
                  }
                  image {
                    large
                    medium
                  }
                }
                voiceActors(language: JAPANESE, sort: [RELEVANCE, ID]) {
                  id
                  name {
                    full
                    native
                  }
                  image {
                    large
                    medium
                  }
                  languageV2
                }
              }
            }
            staff(sort: [RELEVANCE, ID], perPage: 12) {
              edges {
                role
                node {
                  id
                  name {
                    full
                    native
                  }
                  image {
                    large
                    medium
                  }
                }
              }
            }
            externalLinks {
              id
              url
              site
              type
              icon
              color
            }
            isFavourite
            mediaListEntry {
              id
              status
              score
              progress
            }
        """.trimIndent()
    }

    /**
     * Executes a raw GraphQL query or mutation against AniList with rate-limit handling.
     */
    open suspend fun executeQuery(
        query: String,
        variables: JSONObject = JSONObject(),
        token: String? = null,
        retryCount: Int = 0
    ): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }
        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val requestBuilder = Request.Builder()
            .url(GRAPHQL_URL)
            .post(body)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        val code = response.code
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response from AniList")

        // Handle AniList Rate Limits (HTTP 429) gracefully
        if (code == 429) {
            val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull() ?: 2L
            if (retryCount < 2) {
                delay(retryAfterSeconds.coerceIn(1L, 5L) * 1000L)
                return@withContext executeQuery(query, variables, token, retryCount + 1)
            } else {
                throw IllegalStateException("AniList API rate limit reached. Please wait a moment.")
            }
        }

        if (!response.isSuccessful) {
            throw IllegalStateException("AniList GraphQL error HTTP $code: $responseBody")
        }

        val json = JSONObject(responseBody)
        if (json.has("errors") && !json.isNull("errors")) {
            val errors = json.getJSONArray("errors")
            if (errors.length() > 0) {
                val errorMsg = errors.getJSONObject(0).optString("message", "AniList GraphQL Error")
                throw IllegalStateException(errorMsg)
            }
        }
        json
    }

    /**
     * Exchanges an OAuth authorization code for an access token.
     */
    open suspend fun exchangeOAuthCodeForToken(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): String = withContext(Dispatchers.IO) {
        val formBody = okhttp3.FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("redirect_uri", redirectUri)
            .add("code", code)
            
        if (clientSecret.isNotBlank() && clientSecret != "YOUR_ANILIST_CLIENT_SECRET") {
            formBody.add("client_secret", clientSecret)
        }

        val request = Request.Builder()
            .url(com.example.core.config.AniListAuthConfig.OAUTH_TOKEN_URL)
            .post(formBody.build())
            .addHeader("Accept", "application/json")
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IllegalStateException("OAuth token exchange failed HTTP ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        json.getString("access_token")
    }

    /**
     * Retrieves the authenticated viewer profile and statistics.
     */
    open suspend fun getViewer(token: String): AniListUser = withContext(Dispatchers.IO) {
        val query = """
            query {
              Viewer {
                id
                name
                avatar {
                  large
                  medium
                }
                bannerImage
                statistics {
                  anime {
                    count
                    episodesWatched
                    minutesWatched
                    meanScore
                  }
                }
              }
            }
        """.trimIndent()

        val json = executeQuery(query, token = token)
        val viewer = json.getJSONObject("data").getJSONObject("Viewer")
        val stats = viewer.optJSONObject("statistics")?.optJSONObject("anime")
        val minutesWatched = stats?.optLong("minutesWatched") ?: 0L
        val daysWatched = minutesWatched / (60.0 * 24.0)

        AniListUser(
            id = viewer.getLong("id"),
            name = viewer.getString("name"),
            avatarUrl = viewer.optJSONObject("avatar")?.optString("large")
                ?: viewer.optJSONObject("avatar")?.optString("medium"),
            bannerUrl = viewer.optString("bannerImage").takeIf { it.isNotBlank() },
            totalAnimeWatched = stats?.optInt("count") ?: 0,
            totalEpisodesWatched = stats?.optInt("episodesWatched") ?: 0,
            daysWatched = (daysWatched * 10).toInt() / 10.0,
            meanScore = stats?.optDouble("meanScore") ?: 0.0
        )
    }

    /**
     * Retrieves trending anime sorted by TRENDING_DESC.
     */
    open suspend fun getTrendingAnime(page: Int = 1, perPage: Int = 20): List<Anime> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page (page: ${'$'}page, perPage: ${'$'}perPage) {
                media (type: ANIME, sort: TRENDING_DESC, isAdult: false) {
                  $LIST_MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }
        val response = executeQuery(query, variables)
        parseMediaList(response)
    }

    /**
     * Retrieves popular anime sorted by POPULARITY_DESC.
     */
    open suspend fun getPopularAnime(page: Int = 1, perPage: Int = 20): List<Anime> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page (page: ${'$'}page, perPage: ${'$'}perPage) {
                media (type: ANIME, sort: POPULARITY_DESC, isAdult: false) {
                  $LIST_MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }
        val response = executeQuery(query, variables)
        parseMediaList(response)
    }

    /**
     * Retrieves popular anime for a specific season and year.
     */
    open suspend fun getPopularThisSeason(
        season: String,
        seasonYear: Int,
        page: Int = 1,
        perPage: Int = 20
    ): List<Anime> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}season: MediaSeason, ${'$'}seasonYear: Int, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page (page: ${'$'}page, perPage: ${'$'}perPage) {
                media (type: ANIME, season: ${'$'}season, seasonYear: ${'$'}seasonYear, sort: POPULARITY_DESC, isAdult: false) {
                  $LIST_MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("season", season)
            put("seasonYear", seasonYear)
            put("page", page)
            put("perPage", perPage)
        }
        val response = executeQuery(query, variables)
        parseMediaList(response)
    }

    /**
     * Retrieves upcoming anime for the upcoming season or NOT_YET_RELEASED status.
     */
    open suspend fun getUpcomingAnime(
        season: String? = null,
        seasonYear: Int? = null,
        page: Int = 1,
        perPage: Int = 20
    ): List<Anime> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}season: MediaSeason, ${'$'}seasonYear: Int, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page (page: ${'$'}page, perPage: ${'$'}perPage) {
                media (type: ANIME, status: NOT_YET_RELEASED, season: ${'$'}season, seasonYear: ${'$'}seasonYear, sort: POPULARITY_DESC, isAdult: false) {
                  $LIST_MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            if (!season.isNullOrBlank()) put("season", season)
            if (seasonYear != null) put("seasonYear", seasonYear)
            put("page", page)
            put("perPage", perPage)
        }
        val response = executeQuery(query, variables)
        parseMediaList(response)
    }

    /**
     * Retrieves top rated anime of all time sorted by SCORE_DESC.
     */
    open suspend fun getTopRatedAnime(page: Int = 1, perPage: Int = 20): List<Anime> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page (page: ${'$'}page, perPage: ${'$'}perPage) {
                media (type: ANIME, sort: SCORE_DESC, isAdult: false) {
                  $LIST_MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }
        val response = executeQuery(query, variables)
        parseMediaList(response)
    }

    /**
     * Retrieves airing schedule with upcoming episodes.
     */
    open suspend fun getAiringSchedule(
        page: Int = 1,
        perPage: Int = 20
    ): List<Anime> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page (page: ${'$'}page, perPage: ${'$'}perPage) {
                airingSchedules (notYetAired: true, sort: TIME) {
                  episode
                  airingAt
                  timeUntilAiring
                  media {
                    $LIST_MEDIA_FIELDS
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }
        val response = executeQuery(query, variables)
        val schedules = response.optJSONObject("data")
            ?.optJSONObject("Page")
            ?.optJSONArray("airingSchedules") ?: return@withContext emptyList()

        val list = mutableListOf<Anime>()
        for (i in 0 until schedules.length()) {
            val item = schedules.getJSONObject(i)
            val mediaObj = item.optJSONObject("media") ?: continue
            val anime = parseSingleMedia(mediaObj)
            val episodeNum = item.optInt("episode")
            val airingAt = item.optLong("airingAt")
            val timeUntilAiring = item.optLong("timeUntilAiring")
            val updated = anime.copy(
                nextAiringEpisode = episodeNum,
                nextAiringTime = airingAt,
                nextAiring = NextAiringEpisode(
                    episode = episodeNum,
                    airingAt = airingAt,
                    timeUntilAiring = timeUntilAiring
                )
            )
            list.add(updated)
        }
        list
    }

    /**
     * Searches and filters anime by query, genre, tag, season, year, format, status, and sort order.
     */
    open suspend fun searchAnime(
        queryText: String? = null,
        genre: String? = null,
        tag: String? = null,
        season: String? = null,
        seasonYear: Int? = null,
        format: String? = null,
        status: String? = null,
        sort: String? = null,
        page: Int = 1,
        perPage: Int = 20
    ): List<Anime> = withContext(Dispatchers.IO) {
        val query = """
            query (
              ${'$'}search: String,
              ${'$'}genre: String,
              ${'$'}tag: String,
              ${'$'}season: MediaSeason,
              ${'$'}seasonYear: Int,
              ${'$'}format: MediaFormat,
              ${'$'}status: MediaStatus,
              ${'$'}sort: [MediaSort],
              ${'$'}page: Int,
              ${'$'}perPage: Int
            ) {
              Page (page: ${'$'}page, perPage: ${'$'}perPage) {
                media (
                  search: ${'$'}search,
                  genre: ${'$'}genre,
                  tag: ${'$'}tag,
                  season: ${'$'}season,
                  seasonYear: ${'$'}seasonYear,
                  format: ${'$'}format,
                  status: ${'$'}status,
                  sort: ${'$'}sort,
                  type: ANIME,
                  isAdult: false
                ) {
                  $LIST_MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            if (!queryText.isNullOrBlank()) put("search", queryText.trim())
            if (!genre.isNullOrBlank() && !genre.equals("All", ignoreCase = true)) put("genre", genre)
            if (!tag.isNullOrBlank()) put("tag", tag)
            if (!season.isNullOrBlank() && !season.equals("All", ignoreCase = true)) put("season", season)
            if (seasonYear != null && seasonYear > 0) put("seasonYear", seasonYear)
            if (!format.isNullOrBlank() && !format.equals("All", ignoreCase = true)) put("format", format)
            if (!status.isNullOrBlank() && !status.equals("All", ignoreCase = true)) put("status", status)

            val sortArray = JSONArray()
            val sortField = when (sort?.uppercase()) {
                "POPULARITY", "POPULARITY_DESC" -> "POPULARITY_DESC"
                "SCORE", "SCORE_DESC" -> "SCORE_DESC"
                "TRENDING", "TRENDING_DESC" -> "TRENDING_DESC"
                "START_DATE", "START_DATE_DESC" -> "START_DATE_DESC"
                "TITLE", "TITLE_ROMAJI" -> "TITLE_ROMAJI"
                else -> if (!queryText.isNullOrBlank()) "SEARCH_MATCH" else "TRENDING_DESC"
            }
            sortArray.put(sortField)
            put("sort", sortArray)

            put("page", page)
            put("perPage", perPage)
        }

        val response = executeQuery(query, variables)
        parseMediaList(response)
    }

    /**
     * Retrieves comprehensive detailed anime metadata for a single anime ID.
     */
    open suspend fun getAnimeDetails(animeId: Int, token: String? = null): Anime = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}id: Int) {
              Media (id: ${'$'}id, type: ANIME) {
                $DETAIL_MEDIA_FIELDS
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("id", animeId) }
        val response = executeQuery(query, variables, token = token)
        val media = response.getJSONObject("data").getJSONObject("Media")
        parseSingleMedia(media)
    }

    /**
     * Retrieves a user's anime list entries from AniList.
     */
    open suspend fun getUserAnimeList(
        token: String,
        userId: Long? = null,
        username: String? = null,
        status: String? = null
    ): List<AniListMediaListEntry> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}userId: Int, ${'$'}userName: String, ${'$'}status: MediaListStatus) {
              MediaListCollection (userId: ${'$'}userId, userName: ${'$'}userName, type: ANIME, status: ${'$'}status) {
                lists {
                  name
                  isCustomList
                  isSplitCompletedList
                  status
                  entries {
                    id
                    mediaId
                    status
                    score
                    progress
                    updatedAt
                    media {
                      $LIST_MEDIA_FIELDS
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            if (userId != null && userId > 0) put("userId", userId)
            if (!username.isNullOrBlank()) put("userName", username)
            if (!status.isNullOrBlank()) put("status", status)
        }

        val response = executeQuery(query, variables, token = token)
        val listsArray = response.optJSONObject("data")
            ?.optJSONObject("MediaListCollection")
            ?.optJSONArray("lists") ?: return@withContext emptyList()

        val result = mutableListOf<AniListMediaListEntry>()
        for (i in 0 until listsArray.length()) {
            val listObj = listsArray.getJSONObject(i)
            val entries = listObj.optJSONArray("entries") ?: continue
            for (j in 0 until entries.length()) {
                val entryObj = entries.getJSONObject(j)
                val mediaObj = entryObj.optJSONObject("media") ?: continue
                val anime = parseSingleMedia(mediaObj)

                result.add(
                    AniListMediaListEntry(
                        id = entryObj.getLong("id"),
                        mediaId = entryObj.getInt("mediaId"),
                        status = entryObj.optString("status", "CURRENT"),
                        progress = entryObj.optInt("progress", 0),
                        score = entryObj.optDouble("score", 0.0),
                        updatedAt = entryObj.optLong("updatedAt", 0L),
                        anime = anime
                    )
                )
            }
        }
        result
    }

    /**
     * Toggles favourite anime status on AniList.
     */
    open suspend fun toggleFavourite(
        token: String,
        animeId: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val mutation = """
            mutation (${'$'}animeId: Int) {
              ToggleFavourite (animeId: ${'$'}animeId) {
                anime {
                  nodes {
                    id
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("animeId", animeId)
        }
        val response = executeQuery(mutation, variables, token = token)
        response.optJSONObject("data")?.optJSONObject("ToggleFavourite") != null
    }

    /**
     * Updates playback progress and status for an anime entry on AniList.
     */
    open suspend fun updateMediaProgress(
        token: String,
        mediaId: Int,
        progress: Int,
        status: String? = null,
        score: Double? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val mutation = """
            mutation (${'$'}mediaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}score: Float) {
              SaveMediaListEntry (mediaId: ${'$'}mediaId, progress: ${'$'}progress, status: ${'$'}status, score: ${'$'}score) {
                id
                progress
                status
                score
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("mediaId", mediaId)
            put("progress", progress)
            if (!status.isNullOrBlank()) put("status", status)
            if (score != null && score > 0.0) put("score", score)
        }
        val response = executeQuery(mutation, variables, token = token)
        response.optJSONObject("data")?.optJSONObject("SaveMediaListEntry") != null
    }

    /**
     * Sets anime list entry status on AniList (e.g. CURRENT, PLANNING, COMPLETED).
     */
    open suspend fun setMediaListStatus(
        token: String,
        mediaId: Int,
        status: String,
        score: Double? = null,
        progress: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val mutation = """
            mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}score: Float, ${'$'}progress: Int) {
              SaveMediaListEntry (mediaId: ${'$'}mediaId, status: ${'$'}status, score: ${'$'}score, progress: ${'$'}progress) {
                id
                status
                progress
                score
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("mediaId", mediaId)
            put("status", status)
            if (score != null && score > 0.0) put("score", score)
            if (progress != null && progress >= 0) put("progress", progress)
        }
        val response = executeQuery(mutation, variables, token = token)
        response.optJSONObject("data")?.optJSONObject("SaveMediaListEntry") != null
    }

    private fun parseMediaList(json: JSONObject): List<Anime> {
        val mediaArray = json.optJSONObject("data")
            ?.optJSONObject("Page")
            ?.optJSONArray("media") ?: return emptyList()

        val list = mutableListOf<Anime>()
        for (i in 0 until mediaArray.length()) {
            val item = mediaArray.getJSONObject(i)
            list.add(parseSingleMedia(item))
        }
        return list
    }

    private fun parseSingleMedia(media: JSONObject): Anime {
        val titles = media.optJSONObject("title")
        val englishTitle = titles?.optString("english").takeIf { !it.isNullOrBlank() }
        val romajiTitle = titles?.optString("romaji").takeIf { !it.isNullOrBlank() }
        val nativeTitle = titles?.optString("native").takeIf { !it.isNullOrBlank() }
        val userPreferred = titles?.optString("userPreferred").takeIf { !it.isNullOrBlank() }
        val displayTitle = englishTitle ?: romajiTitle ?: userPreferred ?: nativeTitle ?: "Untitled Anime"

        val synonymsList = mutableListOf<String>()
        val synonymsJson = media.optJSONArray("synonyms")
        if (synonymsJson != null) {
            for (s in 0 until synonymsJson.length()) {
                val syn = synonymsJson.optString(s)
                if (syn.isNotBlank()) synonymsList.add(syn)
            }
        }

        val covers = media.optJSONObject("coverImage")
        val coverUrl = covers?.optString("extraLarge").takeIf { !it.isNullOrBlank() }
            ?: covers?.optString("large")
            ?: covers?.optString("medium")
        val coverColor = covers?.optString("color").takeIf { !it.isNullOrBlank() }
        val bannerUrl = media.optString("bannerImage").takeIf { it.isNotBlank() }

        // Parse Studios and Producers
        val allStudios = mutableListOf<String>()
        val allProducers = mutableListOf<String>()
        var mainStudio: String? = null

        val studiosObj = media.optJSONObject("studios")
        if (studiosObj != null) {
            val edges = studiosObj.optJSONArray("edges")
            if (edges != null) {
                for (e in 0 until edges.length()) {
                    val edge = edges.getJSONObject(e)
                    val isMain = edge.optBoolean("isMain", false)
                    val node = edge.optJSONObject("node") ?: continue
                    val name = node.optString("name")
                    val isAnimStudio = node.optBoolean("isAnimationStudio", true)

                    if (name.isNotBlank()) {
                        if (isAnimStudio) {
                            allStudios.add(name)
                            if (isMain && mainStudio == null) {
                                mainStudio = name
                            }
                        } else {
                            allProducers.add(name)
                        }
                    }
                }
            }
            val nodes = studiosObj.optJSONArray("nodes")
            if (nodes != null && mainStudio == null && nodes.length() > 0) {
                mainStudio = nodes.getJSONObject(0).optString("name")
            }
        }
        if (mainStudio == null && allStudios.isNotEmpty()) {
            mainStudio = allStudios.first()
        }

        // Genres
        val genres = mutableListOf<String>()
        val genresJson = media.optJSONArray("genres")
        if (genresJson != null) {
            for (g in 0 until genresJson.length()) {
                genres.add(genresJson.getString(g))
            }
        }

        // Tags
        val tags = mutableListOf<AnimeTag>()
        val tagsJson = media.optJSONArray("tags")
        if (tagsJson != null) {
            for (t in 0 until tagsJson.length()) {
                val tagObj = tagsJson.getJSONObject(t)
                tags.add(
                    AnimeTag(
                        id = tagObj.optInt("id"),
                        name = tagObj.optString("name"),
                        description = tagObj.optString("description").takeIf { it.isNotBlank() },
                        category = tagObj.optString("category").takeIf { it.isNotBlank() },
                        rank = if (tagObj.has("rank") && !tagObj.isNull("rank")) tagObj.getInt("rank") else null,
                        isMediaSpoiler = tagObj.optBoolean("isMediaSpoiler", false)
                    )
                )
            }
        }

        // Scores
        val rawScore = media.optDouble("averageScore", 0.0)
        val score = if (rawScore > 0) (rawScore / 10.0 * 10).toInt() / 10.0 else 0.0
        val meanScore = if (media.has("meanScore") && !media.isNull("meanScore")) {
            val ms = media.optDouble("meanScore", 0.0)
            if (ms > 0) (ms / 10.0 * 10).toInt() / 10.0 else null
        } else null

        val popularity = if (media.has("popularity") && !media.isNull("popularity")) media.getInt("popularity") else null
        val favourites = if (media.has("favourites") && !media.isNull("favourites")) media.getInt("favourites") else null

        // Next Airing
        val nextAiringObj = media.optJSONObject("nextAiringEpisode")
        val nextAiring = if (nextAiringObj != null) {
            NextAiringEpisode(
                episode = nextAiringObj.optInt("episode"),
                airingAt = nextAiringObj.optLong("airingAt"),
                timeUntilAiring = nextAiringObj.optLong("timeUntilAiring", 0L)
            )
        } else null

        // Start & End Dates
        val startDateObj = media.optJSONObject("startDate")
        val startDate = if (startDateObj != null && (!startDateObj.isNull("year") && startDateObj.optInt("year") > 0)) {
            FuzzyDate(
                year = startDateObj.optInt("year").takeIf { it > 0 },
                month = startDateObj.optInt("month").takeIf { it > 0 },
                day = startDateObj.optInt("day").takeIf { it > 0 }
            )
        } else null

        val endDateObj = media.optJSONObject("endDate")
        val endDate = if (endDateObj != null && (!endDateObj.isNull("year") && endDateObj.optInt("year") > 0)) {
            FuzzyDate(
                year = endDateObj.optInt("year").takeIf { it > 0 },
                month = endDateObj.optInt("month").takeIf { it > 0 },
                day = endDateObj.optInt("day").takeIf { it > 0 }
            )
        } else null

        // Trailer
        val trailerObj = media.optJSONObject("trailer")
        val trailer = if (trailerObj != null) {
            AnimeTrailer(
                id = trailerObj.optString("id").takeIf { it.isNotBlank() },
                site = trailerObj.optString("site").takeIf { it.isNotBlank() },
                thumbnail = trailerObj.optString("thumbnail").takeIf { it.isNotBlank() }
            )
        } else null

        // Relations (Anime only, no manga)
        val relations = mutableListOf<AnimeRelation>()
        val relationsObj = media.optJSONObject("relations")
        val relationEdges = relationsObj?.optJSONArray("edges")
        if (relationEdges != null) {
            for (r in 0 until relationEdges.length()) {
                val edge = relationEdges.getJSONObject(r)
                val node = edge.optJSONObject("node") ?: continue
                val type = node.optString("type")
                if (!type.equals("ANIME", ignoreCase = true)) continue // Filter out manga

                val nodeTitleObj = node.optJSONObject("title")
                val nodeTitle = nodeTitleObj?.optString("english").takeIf { !it.isNullOrBlank() }
                    ?: nodeTitleObj?.optString("romaji")
                    ?: nodeTitleObj?.optString("native")
                    ?: "Untitled"

                val nodeCoverObj = node.optJSONObject("coverImage")
                val nodeCover = nodeCoverObj?.optString("large") ?: nodeCoverObj?.optString("medium")

                relations.add(
                    AnimeRelation(
                        relationType = edge.optString("relationType", "OTHER"),
                        animeId = node.getInt("id").toString(),
                        title = nodeTitle,
                        format = node.optString("format"),
                        status = node.optString("status"),
                        coverUrl = nodeCover,
                        seasonYear = if (node.has("seasonYear") && !node.isNull("seasonYear")) node.getInt("seasonYear") else null,
                        totalEpisodes = if (node.has("episodes") && !node.isNull("episodes")) node.getInt("episodes") else null
                    )
                )
            }
        }

        // Recommendations (Anime only)
        val recommendations = mutableListOf<AnimeRecommendation>()
        val recObj = media.optJSONObject("recommendations")
        val recNodes = recObj?.optJSONArray("nodes")
        if (recNodes != null) {
            for (rc in 0 until recNodes.length()) {
                val node = recNodes.getJSONObject(rc)
                val recMedia = node.optJSONObject("mediaRecommendation") ?: continue
                val recType = recMedia.optString("type")
                if (!recType.equals("ANIME", ignoreCase = true)) continue // Filter out manga

                val recTitleObj = recMedia.optJSONObject("title")
                val recTitle = recTitleObj?.optString("english").takeIf { !it.isNullOrBlank() }
                    ?: recTitleObj?.optString("romaji")
                    ?: recTitleObj?.optString("native")
                    ?: "Untitled"

                val recCoverObj = recMedia.optJSONObject("coverImage")
                val recCover = recCoverObj?.optString("large") ?: recCoverObj?.optString("medium")

                val avgScore = recMedia.optDouble("averageScore", 0.0)

                recommendations.add(
                    AnimeRecommendation(
                        animeId = recMedia.getInt("id").toString(),
                        title = recTitle,
                        coverUrl = recCover,
                        format = recMedia.optString("format"),
                        status = recMedia.optString("status"),
                        score = if (avgScore > 0) (avgScore / 10.0 * 10).toInt() / 10.0 else null,
                        rating = node.optInt("rating", 0)
                    )
                )
            }
        }

        // Characters & Voice Actors
        val characters = mutableListOf<AnimeCharacter>()
        val charObj = media.optJSONObject("characters")
        val charEdges = charObj?.optJSONArray("edges")
        if (charEdges != null) {
            for (c in 0 until charEdges.length()) {
                val edge = charEdges.getJSONObject(c)
                val node = edge.optJSONObject("node") ?: continue
                val charId = node.getInt("id")
                val charNameObj = node.optJSONObject("name")
                val charName = charNameObj?.optString("full") ?: "Unknown"
                val charNative = charNameObj?.optString("native").takeIf { !it.isNullOrBlank() }
                val charImg = node.optJSONObject("image")?.optString("large")
                    ?: node.optJSONObject("image")?.optString("medium")
                val role = edge.optString("role", "MAIN")

                // Voice actors
                val vaArray = edge.optJSONArray("voiceActors")
                var vaName: String? = null
                var vaNative: String? = null
                var vaImg: String? = null
                var vaLang: String? = null

                if (vaArray != null && vaArray.length() > 0) {
                    val vaObj = vaArray.getJSONObject(0)
                    val vaNameObj = vaObj.optJSONObject("name")
                    vaName = vaNameObj?.optString("full")
                    vaNative = vaNameObj?.optString("native").takeIf { !it.isNullOrBlank() }
                    vaImg = vaObj.optJSONObject("image")?.optString("large")
                        ?: vaObj.optJSONObject("image")?.optString("medium")
                    vaLang = vaObj.optString("languageV2", "Japanese")
                }

                characters.add(
                    AnimeCharacter(
                        id = charId,
                        name = charName,
                        nativeName = charNative,
                        role = role,
                        imageUrl = charImg,
                        voiceActorName = vaName,
                        voiceActorNativeName = vaNative,
                        voiceActorImageUrl = vaImg,
                        voiceActorLanguage = vaLang
                    )
                )
            }
        }

        // Staff
        val staffList = mutableListOf<AnimeStaff>()
        val staffObj = media.optJSONObject("staff")
        val staffEdges = staffObj?.optJSONArray("edges")
        if (staffEdges != null) {
            for (s in 0 until staffEdges.length()) {
                val edge = staffEdges.getJSONObject(s)
                val node = edge.optJSONObject("node") ?: continue
                val staffId = node.getInt("id")
                val staffNameObj = node.optJSONObject("name")
                val staffName = staffNameObj?.optString("full") ?: "Unknown"
                val staffNative = staffNameObj?.optString("native").takeIf { !it.isNullOrBlank() }
                val role = edge.optString("role", "Staff")
                val staffImg = node.optJSONObject("image")?.optString("large")
                    ?: node.optJSONObject("image")?.optString("medium")

                staffList.add(
                    AnimeStaff(
                        id = staffId,
                        name = staffName,
                        nativeName = staffNative,
                        role = role,
                        imageUrl = staffImg
                    )
                )
            }
        }

        // External Links and Streaming Links
        val externalLinks = mutableListOf<AnimeExternalLink>()
        val streamingLinks = mutableListOf<AnimeExternalLink>()
        val linksArray = media.optJSONArray("externalLinks")
        if (linksArray != null) {
            for (l in 0 until linksArray.length()) {
                val linkObj = linksArray.getJSONObject(l)
                val link = AnimeExternalLink(
                    id = linkObj.optInt("id"),
                    url = linkObj.optString("url"),
                    site = linkObj.optString("site"),
                    type = linkObj.optString("type"),
                    iconUrl = linkObj.optString("icon").takeIf { it.isNotBlank() },
                    color = linkObj.optString("color").takeIf { it.isNotBlank() }
                )
                externalLinks.add(link)
                if (link.type.equals("STREAMING", ignoreCase = true) ||
                    link.site.contains("Crunchyroll", ignoreCase = true) ||
                    link.site.contains("Netflix", ignoreCase = true) ||
                    link.site.contains("Hulu", ignoreCase = true) ||
                    link.site.contains("Disney", ignoreCase = true) ||
                    link.site.contains("HIDIVE", ignoreCase = true) ||
                    link.site.contains("YouTube", ignoreCase = true)
                ) {
                    streamingLinks.add(link)
                }
            }
        }

        // Clean HTML description
        val rawDescription = media.optString("description", "")
        val cleanDescription = rawDescription
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()

        val isFav = media.optBoolean("isFavourite", false)
        val userEntry = media.optJSONObject("mediaListEntry")
        val userStatus = userEntry?.optString("status")
        val userProgress = userEntry?.optInt("progress")
        val userScore = userEntry?.optDouble("score")

        return Anime(
            id = media.getInt("id").toString(),
            title = displayTitle,
            nativeTitle = nativeTitle,
            romajiTitle = romajiTitle,
            englishTitle = englishTitle,
            synonyms = synonymsList,
            bannerUrl = bannerUrl,
            coverUrl = coverUrl,
            coverColor = coverColor,
            description = cleanDescription.ifBlank { null },
            score = score,
            meanScore = meanScore,
            popularity = popularity,
            favourites = favourites,
            status = media.optString("status", "RELEASING"),
            format = media.optString("format", "TV"),
            studio = mainStudio,
            studios = allStudios,
            producers = allProducers,
            source = media.optString("source").takeIf { it.isNotBlank() },
            countryOfOrigin = media.optString("countryOfOrigin").takeIf { it.isNotBlank() },
            season = media.optString("season").takeIf { it.isNotBlank() },
            seasonYear = if (media.has("seasonYear") && !media.isNull("seasonYear")) media.getInt("seasonYear") else null,
            genres = genres,
            tags = tags,
            totalEpisodes = if (media.has("episodes") && !media.isNull("episodes")) media.getInt("episodes") else null,
            episodeDuration = if (media.has("duration") && !media.isNull("duration")) media.getInt("duration") else null,
            startDate = startDate,
            endDate = endDate,
            nextAiring = nextAiring,
            nextAiringEpisode = nextAiring?.episode,
            nextAiringTime = nextAiring?.airingAt,
            trailer = trailer,
            relations = relations,
            recommendations = recommendations,
            characters = characters,
            staff = staffList,
            externalLinks = externalLinks,
            streamingLinks = streamingLinks,
            isFavorite = isFav,
            userListStatus = userStatus,
            userProgress = userProgress,
            userScore = userScore
        )
    }
}
