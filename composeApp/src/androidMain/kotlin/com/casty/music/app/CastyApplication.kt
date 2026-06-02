package com.casty.music.app

import android.app.Application
import androidx.datastore.preferences.core.edit
import com.casty.music.BuildConfig
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import com.casty.music.backend.innertube.YouTube
import com.casty.music.backend.constants.DataSyncIdKey
import com.casty.music.backend.constants.InnerTubeCookieKey
import com.casty.music.backend.constants.UseLoginForBrowse
import com.casty.music.backend.constants.VisitorDataKey
import com.casty.music.backend.security.SecureSessionStore
import com.casty.music.backend.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CastyApplication : Application(), SingletonImageLoader.Factory {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var secureSessionStore: SecureSessionStore

    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base)
        // Set crash handler as EARLY as possible — before Hilt does heavy initialization.
        // This is the most important change for catching "app exits immediately" bugs.
        installEarlyCrashHandler()
    }

    private fun installEarlyCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            val report = buildString {
                appendLine("=== CASTY FATAL CRASH @ $ts ===")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception: ${throwable::class.java.name}")
                appendLine("Message: ${throwable.message}")
                appendLine()
                appendLine("=== STACKTRACE ===")
                appendLine(android.util.Log.getStackTraceString(throwable))
                appendLine("=== END CRASH ===")
            }

            // Write to MULTIPLE locations so user can actually find it
            try {
                // 1. Internal cache (for the app to read on next launch)
                cacheDir.resolve("last_crash.txt").writeText(report)
            } catch (_: Exception) {}

            try {
                // 2. App's external files dir (often accessible without root)
                getExternalFilesDir(null)?.resolve("Casty_crash_log.txt")?.writeText(report)
            } catch (_: Exception) {}

            try {
                // 3. Public Downloads folder — EASIEST for user to find
                @Suppress("DEPRECATION")
                val downloads = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                if (downloads != null) {
                    downloads.mkdirs()
                    java.io.File(downloads, "Casty_crash_log.txt").writeText(report)
                }
            } catch (_: Exception) {}

            // Also try to log if Timber is available
            try {
                android.util.Log.e("CastyCrash", "FATAL CRASH:\n$report")
            } catch (_: Exception) {}

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Plant Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {}
            })
        }

        try {
            com.casty.music.backend.utils.cipher.CipherDeobfuscator.initialize(this)
            bootstrapCastySession()
        } catch (e: Exception) {
            Timber.tag("CastyInit").e(e, "Error during Application.onCreate")
            // Also write this init error to the crash file
            try {
                val err = "=== CAUGHT ERROR IN Application.onCreate ===\n${android.util.Log.getStackTraceString(e)}"
                cacheDir.resolve("last_crash.txt").writeText(err)
            } catch (_: Exception) {}
        }
    }

    private fun bootstrapCastySession() {
        applicationScope.launch {
            // Wait for injection to complete if it hasn't already (rare race condition)
            var retryCount = 0
            while (!::secureSessionStore.isInitialized && retryCount < 10) {
                delay(100)
                retryCount++
            }
            if (!::secureSessionStore.isInitialized) {
                Timber.tag("CastyInit").e("Hilt injection for secureSessionStore failed or timed out")
                return@launch
            }

            runCatching {
                val settings = applicationContext.dataStore.data.first()
                val migratedCookie = settings[InnerTubeCookieKey].takeUnless { it.isNullOrBlank() }
                val migratedVisitorData = settings[VisitorDataKey].takeUnless { it.isNullOrBlank() || it == "null" }
                val migratedDataSyncId = settings[DataSyncIdKey].takeUnless { it.isNullOrBlank() || it == "null" }
                if (secureSessionStore.innerTubeCookie.isNullOrBlank() && !migratedCookie.isNullOrBlank()) {
                    secureSessionStore.updateSession(
                        innerTubeCookie = migratedCookie,
                        visitorData = migratedVisitorData,
                        dataSyncId = migratedDataSyncId,
                    )
                    applicationContext.dataStore.edit { preferences ->
                        preferences.remove(InnerTubeCookieKey)
                        preferences.remove(VisitorDataKey)
                        preferences.remove(DataSyncIdKey)
                    }
                }

                YouTube.cookie = secureSessionStore.innerTubeCookie
                YouTube.dataSyncId = secureSessionStore.dataSyncId
                YouTube.useLoginForBrowse = settings[UseLoginForBrowse] ?: true

                val storedVisitor = secureSessionStore.visitorData
                YouTube.visitorData = storedVisitor ?: YouTube.visitorData
                if (YouTube.visitorData.isNullOrBlank()) {
                    YouTube.visitorData()
                        .onSuccess { visitor ->
                            YouTube.visitorData = visitor
                            secureSessionStore.visitorData = visitor
                        }
                        .onFailure { Timber.e(it, "Unable to fetch Casty visitor data") }
                }
            }.onFailure {
                Timber.e(it, "Unable to bootstrap Casty session")
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(OkHttpNetworkFetcherFactory())
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache(
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build(),
            )
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache(
                DiskCache.Builder()
                    .directory(filesDir.resolve("casty_coil").toOkioPath())
                    .maxSizeBytes(128L * 1024L * 1024L)
                    .build(),
            )
            .build()
}
