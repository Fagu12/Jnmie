package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_history",
    primaryKeys = ["animeId", "episodeNumber"],
    indices = [
        Index(value = ["animeId"]),
        Index(value = ["lastWatchedTimestamp"]),
        Index(value = ["completed"])
    ]
)
data class WatchHistoryEntity(
    val animeId: String,
    val episodeNumber: Int,
    val animeTitle: String,
    val coverUrl: String?,
    val episodeTitle: String,
    val currentPositionMs: Long,
    val durationMs: Long,
    val lastWatchedTimestamp: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)

@Entity(
    tableName = "favorites",
    indices = [
        Index(value = ["addedAt"])
    ]
)
data class FavoriteAnimeEntity(
    @PrimaryKey val animeId: String,
    val title: String,
    val romajiTitle: String?,
    val coverUrl: String?,
    val bannerUrl: String?,
    val score: Double,
    val status: String,
    val studio: String?,
    val genresJson: String,
    val totalEpisodes: Int?,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "extension_repos"
)
data class ExtensionRepoEntity(
    @PrimaryKey val url: String,
    val name: String,
    val description: String?,
    val extensionCount: Int,
    val lastRefreshed: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "installed_extensions",
    indices = [
        Index(value = ["isEnabled"])
    ]
)
data class InstalledExtensionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val language: String,
    val iconUrl: String?,
    val baseUrl: String,
    val description: String?,
    val isNsfw: Boolean,
    val repoUrl: String,
    val isEnabled: Boolean = true,
    val supportedQualitiesJson: String = "1080p,720p,480p"
)

@Entity(
    tableName = "cached_anime",
    indices = [
        Index(value = ["cachedAt"])
    ]
)
data class CachedAnimeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val nativeTitle: String? = null,
    val romajiTitle: String? = null,
    val englishTitle: String? = null,
    val synonymsJson: String = "",
    val bannerUrl: String? = null,
    val coverUrl: String? = null,
    val coverColor: String? = null,
    val description: String? = null,
    val score: Double = 0.0,
    val meanScore: Double? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val status: String = "RELEASING",
    val format: String = "TV",
    val studio: String? = null,
    val studiosJson: String = "",
    val producersJson: String = "",
    val source: String? = null,
    val countryOfOrigin: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val genresJson: String = "",
    val tagsJson: String = "",
    val totalEpisodes: Int? = null,
    val episodeDuration: Int? = null,
    val startDateStr: String? = null,
    val endDateStr: String? = null,
    val nextAiringEpisode: Int? = null,
    val nextAiringTime: Long? = null,
    val timeUntilAiring: Long? = null,
    val trailerSite: String? = null,
    val trailerId: String? = null,
    val relationsJson: String = "",
    val recommendationsJson: String = "",
    val charactersJson: String = "",
    val staffJson: String = "",
    val externalLinksJson: String = "",
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cached_episodes",
    indices = [
        Index(value = ["animeId", "number"])
    ]
)
data class CachedEpisodeEntity(
    @PrimaryKey val id: String, // animeId + "_" + number
    val animeId: String,
    val number: Int,
    val title: String,
    val thumbnail: String?,
    val description: String?,
    val durationSeconds: Long,
    val introStartSeconds: Long?,
    val introEndSeconds: Long?,
    val outroStartSeconds: Long?,
    val outroEndSeconds: Long?,
    val recapStartSeconds: Long?,
    val recapEndSeconds: Long?
)

@Entity(
    tableName = "pending_sync",
    indices = [
        Index(value = ["animeId"]),
        Index(value = ["timestamp"])
    ]
)
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val animeId: String,
    val episodeNumber: Int,
    val status: String? = null,
    val score: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)

@Entity(
    tableName = "anilist_media_entries",
    indices = [
        Index(value = ["animeId"]),
        Index(value = ["status"]),
        Index(value = ["updatedAt"])
    ]
)
data class AniListMediaEntryEntity(
    @PrimaryKey val mediaId: Int,
    val animeId: String,
    val title: String,
    val romajiTitle: String? = null,
    val coverUrl: String? = null,
    val bannerUrl: String? = null,
    val status: String, // CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING
    val progress: Int,
    val score: Double = 0.0,
    val totalEpisodes: Int? = null,
    val format: String = "TV",
    val genresJson: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

