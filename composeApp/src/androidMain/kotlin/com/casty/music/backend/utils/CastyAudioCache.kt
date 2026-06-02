@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.casty.music.backend.utils

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import timber.log.Timber

object CastyAudioCache {
    private const val MAX_CACHE_BYTES = 128L * 1024L * 1024L
    @Volatile private var cache: SimpleCache? = null
    @Volatile private var databaseProvider: StandaloneDatabaseProvider? = null

    fun get(context: Context): SimpleCache {
        val appContext = context.applicationContext
        val legacyCacheDir = appContext.filesDir.resolve("Casty_audio_cache")
        if (legacyCacheDir.exists()) {
            runCatching { legacyCacheDir.deleteRecursively() }
                .onFailure { Timber.w(it, "Unable to remove legacy Casty audio cache") }
        }
        return cache ?: synchronized(this) {
            cache ?: createCache(appContext).also { cache = it }
        }
    }

    private fun createCache(appContext: Context): SimpleCache {
        val cacheDir = appContext.cacheDir.resolve("casty_audio_cache")
        val provider = databaseProvider ?: StandaloneDatabaseProvider(appContext).also { databaseProvider = it }
        fun build(): SimpleCache =
            SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES), provider)

        return runCatching { build() }
            .getOrElse { firstFailure ->
                Timber.w(firstFailure, "Casty audio cache was corrupt or locked; rebuilding it")
                runCatching { cacheDir.deleteRecursively() }
                    .onFailure { Timber.w(it, "Unable to clear broken Casty audio cache") }
                build()
            }
    }
}
