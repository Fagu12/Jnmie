package com.example.player.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Singleton cache manager for Media3 media streaming.
 * Provides progressive disk caching for HTTP streams without blocking playback initiation.
 */
@OptIn(UnstableApi::class)
object PlayerCacheManager {
    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: run {
            val cacheDir = File(context.applicationContext.cacheDir, "media3_video_cache").apply {
                if (!exists()) mkdirs()
            }
            val evictor = LeastRecentlyUsedCacheEvictor(250L * 1024 * 1024) // 250 MB
            val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
            SimpleCache(cacheDir, evictor, databaseProvider).also {
                simpleCache = it
            }
        }
    }

    fun buildCacheDataSourceFactory(
        context: Context,
        upstreamFactory: DefaultDataSource.Factory
    ): CacheDataSource.Factory {
        val cache = getCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
