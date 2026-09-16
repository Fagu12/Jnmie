package com.example.data.repository

import com.example.data.local.dao.AnimeDao
import com.example.data.local.dao.PlaybackDao
import com.example.data.local.entity.CachedAnimeEntity
import com.example.data.local.entity.CachedEpisodeEntity
import com.example.data.local.entity.FavoriteAnimeEntity
import com.example.data.remote.anilist.AniListGraphQL
import com.example.domain.model.Anime
import com.example.domain.model.AnimeCharacter
import com.example.domain.model.AnimeExternalLink
import com.example.domain.model.AnimeRecommendation
import com.example.domain.model.AnimeRelation
import com.example.domain.model.AnimeStaff
import com.example.domain.model.AnimeTag
import com.example.domain.model.AnimeTrailer
import com.example.domain.model.Episode
import com.example.domain.model.FuzzyDate
import com.example.domain.model.NextAiringEpisode
import com.example.domain.repository.AnimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class AnimeRepositoryImpl(
    private val aniListApi: AniListGraphQL,
    private val animeDao: AnimeDao,
    private val playbackDao: PlaybackDao
) : AnimeRepository {

    private fun getCurrentSeason(): Pair<String, Int> {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) // 0-indexed: 0 = Jan, 11 = Dec
        val year = cal.get(Calendar.YEAR)
        val season = when (month) {
            0, 1, 2 -> "WINTER"
            3, 4, 5 -> "SPRING"
            6, 7, 8 -> "SUMMER"
            else -> "FALL"
        }
        return Pair(season, year)
    }

    private fun getNextSeason(): Pair<String, Int> {
        val (season, year) = getCurrentSeason()
        return when (season) {
            "WINTER" -> Pair("SPRING", year)
            "SPRING" -> Pair("SUMMER", year)
            "SUMMER" -> Pair("FALL", year)
            "FALL" -> Pair("WINTER", year + 1)
            else -> Pair("SPRING", year)
        }
    }

    override suspend fun getTrendingAnime(page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val list = aniListApi.getTrendingAnime(page = page, perPage = 20)
            if (list.isNotEmpty()) {
                cacheAnimeList(list)
                Result.success(list)
            } else {
                val cached = getCachedAnimeList()
                Result.success(cached)
            }
        } catch (e: Exception) {
            val cached = getCachedAnimeList()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPopularThisSeason(page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val (season, year) = getCurrentSeason()
            val list = aniListApi.getPopularThisSeason(season = season, seasonYear = year, page = page, perPage = 20)
            if (list.isNotEmpty()) {
                cacheAnimeList(list)
                Result.success(list)
            } else {
                val fallback = aniListApi.getPopularAnime(page = page, perPage = 20)
                if (fallback.isNotEmpty()) {
                    cacheAnimeList(fallback)
                    Result.success(fallback)
                } else {
                    Result.success(getCachedAnimeList())
                }
            }
        } catch (e: Exception) {
            val cached = getCachedAnimeList()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getUpcomingAnime(page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val (nextSeason, nextYear) = getNextSeason()
            val list = aniListApi.getUpcomingAnime(season = nextSeason, seasonYear = nextYear, page = page, perPage = 20)
            if (list.isNotEmpty()) {
                cacheAnimeList(list)
                Result.success(list)
            } else {
                Result.success(getCachedAnimeList())
            }
        } catch (e: Exception) {
            val cached = getCachedAnimeList()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getTopRated(page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val list = aniListApi.getTopRatedAnime(page = page, perPage = 20)
            if (list.isNotEmpty()) {
                cacheAnimeList(list)
                Result.success(list)
            } else {
                Result.success(getCachedAnimeList())
            }
        } catch (e: Exception) {
            val cached = getCachedAnimeList()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getRecentlyUpdated(page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val list = aniListApi.getTrendingAnime(page = page, perPage = 15)
            Result.success(list)
        } catch (e: Exception) {
            val cached = getCachedAnimeList()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getAiringSchedule(page: Int): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val list = aniListApi.getAiringSchedule(page = page, perPage = 20)
            if (list.isNotEmpty()) {
                cacheAnimeList(list)
                Result.success(list)
            } else {
                Result.success(getCachedAnimeList())
            }
        } catch (e: Exception) {
            val cached = getCachedAnimeList()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchAnime(
        query: String,
        genre: String?,
        tag: String?,
        season: String?,
        seasonYear: Int?,
        format: String?,
        status: String?,
        sort: String?,
        page: Int
    ): Result<List<Anime>> = withContext(Dispatchers.IO) {
        try {
            val list = aniListApi.searchAnime(
                queryText = query.takeIf { it.isNotBlank() },
                genre = genre,
                tag = tag,
                season = season,
                seasonYear = seasonYear,
                format = format,
                status = status,
                sort = sort,
                page = page,
                perPage = 24
            )
            Result.success(list)
        } catch (e: Exception) {
            val cached = getCachedAnimeList().filter {
                (query.isBlank() || it.title.contains(query, ignoreCase = true) || it.englishTitle?.contains(query, ignoreCase = true) == true) &&
                        (genre.isNullOrBlank() || genre == "All" || it.genres.any { g -> g.equals(genre, ignoreCase = true) })
            }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getAnimeDetails(animeId: String): Result<Anime> = withContext(Dispatchers.IO) {
        try {
            val idInt = animeId.toIntOrNull()
            if (idInt != null) {
                val anime = aniListApi.getAnimeDetails(idInt)
                cacheAnimeList(listOf(anime))
                Result.success(anime)
            } else {
                val cached = animeDao.getCachedAnime(animeId)
                if (cached != null) {
                    Result.success(cached.toDomain())
                } else {
                    Result.failure(IllegalArgumentException("Invalid Anime ID: $animeId"))
                }
            }
        } catch (e: Exception) {
            val cached = animeDao.getCachedAnime(animeId)
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getEpisodes(animeId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        val cached = animeDao.getCachedEpisodes(animeId)
        if (cached.isNotEmpty()) {
            val domainEpisodes = cached.map { it.toDomain(playbackDao) }
            return@withContext Result.success(domainEpisodes)
        }

        // Generate episodic entries based on totalEpisodes / nextAiringEpisode from AniList
        val anime = getAnimeDetails(animeId).getOrNull()
        val count = anime?.totalEpisodes ?: anime?.currentEpisodeCount ?: 12
        val generated = (1..count).map { epNum ->
            CachedEpisodeEntity(
                id = "${animeId}_$epNum",
                animeId = animeId,
                number = epNum,
                title = "Episode $epNum",
                thumbnail = anime?.bannerUrl ?: anime?.coverUrl,
                description = "Episode $epNum of ${anime?.title ?: "the series"}",
                durationSeconds = (anime?.episodeDuration?.toLong() ?: 24L) * 60L,
                introStartSeconds = 85L,
                introEndSeconds = 175L,
                outroStartSeconds = 1290L,
                outroEndSeconds = 1380L,
                recapStartSeconds = 0L,
                recapEndSeconds = if (epNum > 1) 40L else 0L
            )
        }
        animeDao.insertCachedEpisodes(generated)
        Result.success(generated.map { it.toDomain(playbackDao) })
    }

    override fun getFavoriteAnimeList(): Flow<List<Anime>> {
        return animeDao.getAllFavorites().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun isFavoriteFlow(animeId: String): Flow<Boolean> {
        return animeDao.isFavoriteFlow(animeId)
    }

    override suspend fun isFavorite(animeId: String): Boolean = withContext(Dispatchers.IO) {
        animeDao.isFavorite(animeId)
    }

    override suspend fun toggleFavorite(anime: Anime): Boolean = withContext(Dispatchers.IO) {
        val isFav = animeDao.isFavorite(anime.id)
        if (isFav) {
            animeDao.deleteFavorite(anime.id)
            false
        } else {
            animeDao.insertFavorite(
                FavoriteAnimeEntity(
                    animeId = anime.id,
                    title = anime.title,
                    romajiTitle = anime.romajiTitle,
                    coverUrl = anime.coverUrl,
                    bannerUrl = anime.bannerUrl,
                    score = anime.score,
                    status = anime.status,
                    studio = anime.studio,
                    genresJson = anime.genres.joinToString(","),
                    totalEpisodes = anime.totalEpisodes
                )
            )
            true
        }
    }

    override suspend fun getCachedAnime(animeId: String): Anime? = withContext(Dispatchers.IO) {
        animeDao.getCachedAnime(animeId)?.toDomain()
    }

    private suspend fun cacheAnimeList(list: List<Anime>) {
        val entities = list.map { anime ->
            CachedAnimeEntity(
                id = anime.id,
                title = anime.title,
                nativeTitle = anime.nativeTitle,
                romajiTitle = anime.romajiTitle,
                englishTitle = anime.englishTitle,
                synonymsJson = anime.synonyms.joinToString("||"),
                bannerUrl = anime.bannerUrl,
                coverUrl = anime.coverUrl,
                coverColor = anime.coverColor,
                description = anime.description,
                score = anime.score,
                meanScore = anime.meanScore,
                popularity = anime.popularity,
                favourites = anime.favourites,
                status = anime.status,
                format = anime.format,
                studio = anime.studio,
                studiosJson = anime.studios.joinToString("||"),
                producersJson = anime.producers.joinToString("||"),
                source = anime.source,
                countryOfOrigin = anime.countryOfOrigin,
                season = anime.season,
                seasonYear = anime.seasonYear,
                genresJson = anime.genres.joinToString(","),
                tagsJson = serializeTags(anime.tags),
                totalEpisodes = anime.totalEpisodes,
                episodeDuration = anime.episodeDuration,
                startDateStr = anime.startDate?.formatted(),
                endDateStr = anime.endDate?.formatted(),
                nextAiringEpisode = anime.nextAiringEpisode,
                nextAiringTime = anime.nextAiringTime,
                timeUntilAiring = anime.nextAiring?.timeUntilAiring,
                trailerSite = anime.trailer?.site,
                trailerId = anime.trailer?.id,
                relationsJson = serializeRelations(anime.relations),
                recommendationsJson = serializeRecommendations(anime.recommendations),
                charactersJson = serializeCharacters(anime.characters),
                staffJson = serializeStaff(anime.staff),
                externalLinksJson = serializeExternalLinks(anime.externalLinks),
                cachedAt = System.currentTimeMillis()
            )
        }
        animeDao.insertCachedAnime(entities)
    }

    private suspend fun getCachedAnimeList(): List<Anime> {
        return animeDao.getRecentCachedAnime(30).map { it.toDomain() }
    }

    private fun CachedAnimeEntity.toDomain() = Anime(
        id = id,
        title = title,
        nativeTitle = nativeTitle,
        romajiTitle = romajiTitle,
        englishTitle = englishTitle,
        synonyms = if (synonymsJson.isBlank()) emptyList() else synonymsJson.split("||"),
        bannerUrl = bannerUrl,
        coverUrl = coverUrl,
        coverColor = coverColor,
        description = description,
        score = score,
        meanScore = meanScore,
        popularity = popularity,
        favourites = favourites,
        status = status,
        format = format,
        studio = studio,
        studios = if (studiosJson.isBlank()) emptyList() else studiosJson.split("||"),
        producers = if (producersJson.isBlank()) emptyList() else producersJson.split("||"),
        source = source,
        countryOfOrigin = countryOfOrigin,
        season = season,
        seasonYear = seasonYear,
        genres = if (genresJson.isBlank()) emptyList() else genresJson.split(","),
        tags = deserializeTags(tagsJson),
        totalEpisodes = totalEpisodes,
        episodeDuration = episodeDuration,
        nextAiringEpisode = nextAiringEpisode,
        nextAiringTime = nextAiringTime,
        nextAiring = if (nextAiringEpisode != null && nextAiringTime != null) {
            NextAiringEpisode(episode = nextAiringEpisode, airingAt = nextAiringTime, timeUntilAiring = timeUntilAiring ?: 0L)
        } else null,
        trailer = if (!trailerId.isNullOrBlank()) AnimeTrailer(id = trailerId, site = trailerSite) else null,
        relations = deserializeRelations(relationsJson),
        recommendations = deserializeRecommendations(recommendationsJson),
        characters = deserializeCharacters(charactersJson),
        staff = deserializeStaff(staffJson),
        externalLinks = deserializeExternalLinks(externalLinksJson),
        streamingLinks = deserializeExternalLinks(externalLinksJson).filter {
            it.type.equals("STREAMING", ignoreCase = true) || it.site.contains("Crunchyroll", ignoreCase = true)
        }
    )

    private fun FavoriteAnimeEntity.toDomain() = Anime(
        id = animeId,
        title = title,
        romajiTitle = romajiTitle,
        coverUrl = coverUrl,
        bannerUrl = bannerUrl,
        score = score,
        status = status,
        studio = studio,
        genres = if (genresJson.isBlank()) emptyList() else genresJson.split(","),
        totalEpisodes = totalEpisodes,
        isFavorite = true
    )

    private suspend fun CachedEpisodeEntity.toDomain(playbackDao: PlaybackDao): Episode {
        val progress = playbackDao.getEpisodeProgress(animeId, number)
        return Episode(
            id = id,
            animeId = animeId,
            number = number,
            title = title,
            thumbnail = thumbnail,
            description = description,
            durationSeconds = durationSeconds,
            introStartSeconds = introStartSeconds,
            introEndSeconds = introEndSeconds,
            outroStartSeconds = outroStartSeconds,
            outroEndSeconds = outroEndSeconds,
            recapStartSeconds = recapStartSeconds,
            recapEndSeconds = recapEndSeconds,
            watchedProgressMs = progress?.currentPositionMs ?: 0L,
            isWatched = progress?.completed == true
        )
    }

    private fun serializeTags(tags: List<AnimeTag>): String {
        if (tags.isEmpty()) return ""
        val arr = JSONArray()
        tags.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("description", t.description ?: "")
                put("category", t.category ?: "")
                if (t.rank != null) put("rank", t.rank)
                put("isMediaSpoiler", t.isMediaSpoiler)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeTags(jsonStr: String): List<AnimeTag> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<AnimeTag>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AnimeTag(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description").takeIf { it.isNotBlank() },
                        category = obj.optString("category").takeIf { it.isNotBlank() },
                        rank = if (obj.has("rank") && !obj.isNull("rank")) obj.getInt("rank") else null,
                        isMediaSpoiler = obj.optBoolean("isMediaSpoiler", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeRelations(relations: List<AnimeRelation>): String {
        if (relations.isEmpty()) return ""
        val arr = JSONArray()
        relations.forEach { r ->
            val obj = JSONObject().apply {
                put("relationType", r.relationType)
                put("animeId", r.animeId)
                put("title", r.title)
                put("format", r.format ?: "")
                put("status", r.status ?: "")
                put("coverUrl", r.coverUrl ?: "")
                if (r.seasonYear != null) put("seasonYear", r.seasonYear)
                if (r.totalEpisodes != null) put("totalEpisodes", r.totalEpisodes)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeRelations(jsonStr: String): List<AnimeRelation> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<AnimeRelation>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AnimeRelation(
                        relationType = obj.getString("relationType"),
                        animeId = obj.getString("animeId"),
                        title = obj.getString("title"),
                        format = obj.optString("format").takeIf { it.isNotBlank() },
                        status = obj.optString("status").takeIf { it.isNotBlank() },
                        coverUrl = obj.optString("coverUrl").takeIf { it.isNotBlank() },
                        seasonYear = if (obj.has("seasonYear") && !obj.isNull("seasonYear")) obj.getInt("seasonYear") else null,
                        totalEpisodes = if (obj.has("totalEpisodes") && !obj.isNull("totalEpisodes")) obj.getInt("totalEpisodes") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeRecommendations(recommendations: List<AnimeRecommendation>): String {
        if (recommendations.isEmpty()) return ""
        val arr = JSONArray()
        recommendations.forEach { r ->
            val obj = JSONObject().apply {
                put("animeId", r.animeId)
                put("title", r.title)
                put("coverUrl", r.coverUrl ?: "")
                put("format", r.format ?: "")
                put("status", r.status ?: "")
                if (r.score != null) put("score", r.score)
                put("rating", r.rating)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeRecommendations(jsonStr: String): List<AnimeRecommendation> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<AnimeRecommendation>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AnimeRecommendation(
                        animeId = obj.getString("animeId"),
                        title = obj.getString("title"),
                        coverUrl = obj.optString("coverUrl").takeIf { it.isNotBlank() },
                        format = obj.optString("format").takeIf { it.isNotBlank() },
                        status = obj.optString("status").takeIf { it.isNotBlank() },
                        score = if (obj.has("score") && !obj.isNull("score")) obj.getDouble("score") else null,
                        rating = obj.optInt("rating", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeCharacters(characters: List<AnimeCharacter>): String {
        if (characters.isEmpty()) return ""
        val arr = JSONArray()
        characters.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("nativeName", c.nativeName ?: "")
                put("role", c.role)
                put("imageUrl", c.imageUrl ?: "")
                put("vaName", c.voiceActorName ?: "")
                put("vaNative", c.voiceActorNativeName ?: "")
                put("vaImg", c.voiceActorImageUrl ?: "")
                put("vaLang", c.voiceActorLanguage ?: "")
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeCharacters(jsonStr: String): List<AnimeCharacter> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<AnimeCharacter>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AnimeCharacter(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        nativeName = obj.optString("nativeName").takeIf { it.isNotBlank() },
                        role = obj.optString("role", "MAIN"),
                        imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() },
                        voiceActorName = obj.optString("vaName").takeIf { it.isNotBlank() },
                        voiceActorNativeName = obj.optString("vaNative").takeIf { it.isNotBlank() },
                        voiceActorImageUrl = obj.optString("vaImg").takeIf { it.isNotBlank() },
                        voiceActorLanguage = obj.optString("vaLang").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeStaff(staff: List<AnimeStaff>): String {
        if (staff.isEmpty()) return ""
        val arr = JSONArray()
        staff.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("nativeName", s.nativeName ?: "")
                put("role", s.role)
                put("imageUrl", s.imageUrl ?: "")
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeStaff(jsonStr: String): List<AnimeStaff> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<AnimeStaff>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AnimeStaff(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        nativeName = obj.optString("nativeName").takeIf { it.isNotBlank() },
                        role = obj.optString("role", "Staff"),
                        imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeExternalLinks(links: List<AnimeExternalLink>): String {
        if (links.isEmpty()) return ""
        val arr = JSONArray()
        links.forEach { l ->
            val obj = JSONObject().apply {
                put("id", l.id)
                put("url", l.url)
                put("site", l.site)
                put("type", l.type ?: "")
                put("iconUrl", l.iconUrl ?: "")
                put("color", l.color ?: "")
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeExternalLinks(jsonStr: String): List<AnimeExternalLink> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<AnimeExternalLink>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AnimeExternalLink(
                        id = obj.getInt("id"),
                        url = obj.getString("url"),
                        site = obj.getString("site"),
                        type = obj.optString("type").takeIf { it.isNotBlank() },
                        iconUrl = obj.optString("iconUrl").takeIf { it.isNotBlank() },
                        color = obj.optString("color").takeIf { it.isNotBlank() }
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
