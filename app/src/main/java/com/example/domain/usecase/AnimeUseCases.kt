package com.example.domain.usecase

import com.example.domain.model.Anime
import com.example.domain.model.Episode
import com.example.domain.repository.AnimeRepository
import kotlinx.coroutines.flow.Flow

class GetTrendingAnimeUseCase(private val repository: AnimeRepository) {
    suspend operator fun invoke(page: Int = 1): Result<List<Anime>> =
        repository.getTrendingAnime(page)
}

class GetPopularSeasonAnimeUseCase(private val repository: AnimeRepository) {
    suspend operator fun invoke(page: Int = 1): Result<List<Anime>> =
        repository.getPopularThisSeason(page)
}

class GetRecentlyUpdatedAnimeUseCase(private val repository: AnimeRepository) {
    suspend operator fun invoke(page: Int = 1): Result<List<Anime>> =
        repository.getRecentlyUpdated(page)
}

class SearchAnimeUseCase(private val repository: AnimeRepository) {
    suspend operator fun invoke(query: String, genre: String? = null, page: Int = 1): Result<List<Anime>> =
        repository.searchAnime(query = query, genre = genre, page = page)
}

class GetAnimeDetailsUseCase(private val repository: AnimeRepository) {
    suspend operator fun invoke(animeId: String): Result<Anime> =
        repository.getAnimeDetails(animeId)
}

class GetEpisodesUseCase(private val repository: AnimeRepository) {
    suspend operator fun invoke(animeId: String): Result<List<Episode>> =
        repository.getEpisodes(animeId)
}

class GetFavoriteAnimeUseCase(private val repository: AnimeRepository) {
    operator fun invoke(): Flow<List<Anime>> =
        repository.getFavoriteAnimeList()
}

class ToggleFavoriteUseCase(private val repository: AnimeRepository) {
    suspend operator fun invoke(anime: Anime): Boolean =
        repository.toggleFavorite(anime)
}
