package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AnimeDao
import com.example.data.local.dao.PlaybackDao
import com.example.data.local.dao.SyncDao
import com.example.data.local.entity.AniListMediaEntryEntity
import com.example.data.local.entity.CachedAnimeEntity
import com.example.data.local.entity.CachedEpisodeEntity
import com.example.data.local.entity.FavoriteAnimeEntity
import com.example.data.local.entity.PendingSyncEntity
import com.example.data.local.entity.WatchHistoryEntity

@Database(
    entities = [
        WatchHistoryEntity::class,
        FavoriteAnimeEntity::class,
        CachedAnimeEntity::class,
        CachedEpisodeEntity::class,
        PendingSyncEntity::class,
        AniListMediaEntryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
    abstract fun playbackDao(): PlaybackDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "just_anime.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
