package com.example.data.repository

import com.example.domain.model.Anime
import com.example.domain.repository.AnimeRepository
import com.example.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Implementation of SearchRepository.
 * Fully powered by AniList metadata with persistent search history and popular search tags.
 */
class SearchRepositoryImpl(
    private val animeMetadataRepository: AnimeRepository
) : SearchRepository {

    private val _recentSearches = MutableStateFlow<List<String>>(
        listOf("Solo Leveling", "DAN DA DAN", "Bleach", "One Piece", "Jujutsu Kaisen", "Demon Slayer")
    )

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
    ): Result<List<Anime>> {
        return try {
            val cleanQuery = query.trim()
            animeMetadataRepository.searchAnime(
                query = cleanQuery,
                genre = genre,
                tag = tag,
                season = season,
                seasonYear = seasonYear,
                format = format,
                status = status,
                sort = sort,
                page = page
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPopularSearchTags(): List<String> {
        return listOf(
            "Action", "Fantasy", "Adventure", "Romance", "Supernatural",
            "Comedy", "Sci-Fi", "Isekai", "Shounen", "Slice of Life",
            "Drama", "Mystery", "Sports", "Psychological"
        )
    }

    override fun getRecentSearches(): Flow<List<String>> {
        return _recentSearches.asStateFlow()
    }

    override suspend fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _recentSearches.update { current ->
            val updated = current.toMutableList()
            updated.remove(trimmed)
            updated.add(0, trimmed)
            if (updated.size > 15) updated.take(15) else updated
        }
    }

    override suspend fun removeRecentSearch(query: String) {
        _recentSearches.update { current ->
            current.filterNot { it.equals(query.trim(), ignoreCase = true) }
        }
    }

    override suspend fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }
}
