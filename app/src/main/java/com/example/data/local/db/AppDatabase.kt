package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AnimeDao
import com.example.data.local.dao.ExtensionDao
import com.example.data.local.dao.PlaybackDao
import com.example.data.local.dao.SyncDao
import com.example.data.local.entity.AniListMediaEntryEntity
import com.example.data.local.entity.CachedAnimeEntity
import com.example.data.local.entity.CachedEpisodeEntity
import com.example.data.local.entity.ExtensionRepoEntity
import com.example.data.local.entity.FavoriteAnimeEntity
import com.example.data.local.entity.InstalledExtensionEntity
import com.example.data.local.entity.PendingSyncEntity
import com.example.data.local.entity.WatchHistoryEntity

@Database(
    entities = [
        WatchHistoryEntity::class,
        FavoriteAnimeEntity::class,
        ExtensionRepoEntity::class,
        InstalledExtensionEntity::class,
        CachedAnimeEntity::class,
        CachedEpisodeEntity::class,
        PendingSyncEntity::class,
        AniListMediaEntryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
    abstract fun playbackDao(): PlaybackDao
    abstract fun extensionDao(): ExtensionDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from Database v1 to v2:
         * Adds cached anime, episode caching, extension repository configurations,
         * offline pending sync queue, and tracked AniList media entries.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `extension_repos` (
                        `url` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `extensionCount` INTEGER NOT NULL,
                        `lastRefreshed` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installed_extensions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `iconUrl` TEXT,
                        `baseUrl` TEXT NOT NULL,
                        `description` TEXT,
                        `isNsfw` INTEGER NOT NULL,
                        `repoUrl` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `supportedQualitiesJson` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_anime` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `nativeTitle` TEXT,
                        `romajiTitle` TEXT,
                        `englishTitle` TEXT,
                        `synonymsJson` TEXT NOT NULL,
                        `bannerUrl` TEXT,
                        `coverUrl` TEXT,
                        `coverColor` TEXT,
                        `description` TEXT,
                        `score` REAL NOT NULL,
                        `meanScore` REAL,
                        `popularity` INTEGER,
                        `favourites` INTEGER,
                        `status` TEXT NOT NULL,
                        `format` TEXT NOT NULL,
                        `studio` TEXT,
                        `studiosJson` TEXT NOT NULL,
                        `producersJson` TEXT NOT NULL,
                        `source` TEXT,
                        `countryOfOrigin` TEXT,
                        `season` TEXT,
                        `seasonYear` INTEGER,
                        `genresJson` TEXT NOT NULL,
                        `tagsJson` TEXT NOT NULL,
                        `totalEpisodes` INTEGER,
                        `episodeDuration` INTEGER,
                        `startDateStr` TEXT,
                        `endDateStr` TEXT,
                        `nextAiringEpisode` INTEGER,
                        `nextAiringTime` INTEGER,
                        `timeUntilAiring` INTEGER,
                        `trailerSite` TEXT,
                        `trailerId` TEXT,
                        `relationsJson` TEXT NOT NULL,
                        `recommendationsJson` TEXT NOT NULL,
                        `charactersJson` TEXT NOT NULL,
                        `staffJson` TEXT NOT NULL,
                        `externalLinksJson` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_episodes` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `animeId` TEXT NOT NULL,
                        `number` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `thumbnail` TEXT,
                        `description` TEXT,
                        `durationSeconds` INTEGER NOT NULL,
                        `introStartSeconds` INTEGER,
                        `introEndSeconds` INTEGER,
                        `outroStartSeconds` INTEGER,
                        `outroEndSeconds` INTEGER,
                        `recapStartSeconds` INTEGER,
                        `recapEndSeconds` INTEGER
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_sync` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `animeId` TEXT NOT NULL,
                        `episodeNumber` INTEGER NOT NULL,
                        `status` TEXT,
                        `score` REAL,
                        `timestamp` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `lastError` TEXT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `anilist_media_entries` (
                        `mediaId` INTEGER NOT NULL PRIMARY KEY,
                        `animeId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `romajiTitle` TEXT,
                        `coverUrl` TEXT,
                        `bannerUrl` TEXT,
                        `status` TEXT NOT NULL,
                        `progress` INTEGER NOT NULL,
                        `score` REAL NOT NULL,
                        `totalEpisodes` INTEGER,
                        `format` TEXT NOT NULL,
                        `genresJson` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from Database v2 to v3:
         * Adds performance optimization indices across query keys.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_history_animeId` ON `watch_history` (`animeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_history_lastWatchedTimestamp` ON `watch_history` (`lastWatchedTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_history_completed` ON `watch_history` (`completed`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorites_addedAt` ON `favorites` (`addedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_installed_extensions_isEnabled` ON `installed_extensions` (`isEnabled`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_anime_cachedAt` ON `cached_anime` (`cachedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_episodes_animeId_number` ON `cached_episodes` (`animeId`, `number`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_sync_animeId` ON `pending_sync` (`animeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_sync_timestamp` ON `pending_sync` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anilist_media_entries_animeId` ON `anilist_media_entries` (`animeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anilist_media_entries_status` ON `anilist_media_entries` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_anilist_media_entries_updatedAt` ON `anilist_media_entries` (`updatedAt`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "just_anime.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

