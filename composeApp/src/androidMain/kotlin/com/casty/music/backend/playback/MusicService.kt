package com.casty.music.backend.playback

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.content.getSystemService
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.casty.music.backend.innertube.YouTube
import com.casty.music.backend.constants.AudioQuality
import com.casty.music.backend.constants.AudioQualityKey
import com.casty.music.backend.constants.DataSyncIdKey
import com.casty.music.backend.constants.InnerTubeCookieKey
import com.casty.music.backend.constants.SkipSilenceKey
import com.casty.music.backend.constants.UseLoginForBrowse
import com.casty.music.backend.constants.VisitorDataKey
import com.casty.music.backend.security.SecureSessionStore
import com.casty.music.backend.playback.queues.Queue
import com.casty.music.backend.models.toMediaItem
import com.casty.music.backend.utils.CastyAudioCache
import com.casty.music.backend.utils.YTPlayerUtils
import com.casty.music.backend.utils.cipher.CipherDeobfuscator
import com.casty.music.backend.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@UnstableApi
@dagger.hilt.android.AndroidEntryPoint
class MusicService : MediaLibraryService() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val streamUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val binder = MusicBinder()
    @Volatile private var currentAudioQuality: AudioQuality = AudioQuality.VERY_HIGH
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(NETWORK_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NETWORK_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NETWORK_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .proxy(YouTube.proxy)
            .build()
    }
    
    @javax.inject.Inject
    lateinit var secureSessionStore: SecureSessionStore
    
    private lateinit var mediaSession: MediaLibrarySession
    private var currentQueue: Queue? = null
    @Volatile private var isLoadingMoreQueue = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && ::player.isInitialized) {
                player.pause()
            }
        }
    }

    lateinit var player: ExoPlayer
        private set

    val isReadyForConnections: Boolean
        get() = ::player.isInitialized && ::mediaSession.isInitialized && isPlayerReady.value

    internal val mainScope: CoroutineScope
        get() = scope

    val isPlayerReady = MutableStateFlow(false)
    val playerFlow = MutableStateFlow<ExoPlayer?>(null)
    var queueTitle: String? = null
        private set

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        runCatching {
        // CipherDeobfuscator is now initialized in CastyApplication
        observeCastySession()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Zero-latency optimized for music (small buffers, aggressive prefetch)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(1500, 5000, 1500, 2000)
            .setBackBuffer(2000, false)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(createDataSourceFactory()))
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setSeekParameters(SeekParameters.EXACT) // Faster, more precise seeking
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
                addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        loadMoreQueueItemsIfNeeded()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            loadMoreQueueItemsIfNeeded()
                        }
                    }
                })
            }
        ContextCompat.registerReceiver(
            this,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Reliable, battle-tested default provider from Media3.
        // This is the simplest and most stable path for background playback + lockscreen.
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(NOW_PLAYING_NOTIFICATION_ID)
            .setChannelId(NOW_PLAYING_CHANNEL_ID)
            .setChannelName(com.casty.music.R.string.now_playing_notification_channel)
            .build()
            .apply {
                setSmallIcon(com.casty.music.R.drawable.musical_notes)
            }
        setMediaNotificationProvider(notificationProvider)

        // Premium MediaLibrarySession with proper callback (enables future god-tier features
        // like custom commands from Android Auto, Wear, lock screen, notification, etc.)
        mediaSession = MediaLibrarySession.Builder(this, player, CastyMediaSessionCallback())
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, com.casty.music.app.CastyMainActivity::class.java)
                        .putExtra("expandPlayerBottomSheet", true),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()

        isPlayerReady.value = true
        playerFlow.value = player
        observePlaybackSettings()
        }.onFailure { throwable ->
            Timber.e(throwable, "Casty playback service failed during startup")
            isPlayerReady.value = false
            playerFlow.value = null
            runCatching { if (::mediaSession.isInitialized) mediaSession.release() }
            runCatching { if (::player.isInitialized) player.release() }
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent) ?: binder

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = mediaSession

    override fun onDestroy() {
        isPlayerReady.value = false
        playerFlow.value = null
        runCatching { unregisterReceiver(noisyReceiver) }
        if (::mediaSession.isInitialized) mediaSession.release()
        if (::player.isInitialized) player.release()
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Critical for seamless background experience.
     * When user swipes app away from recents, we decide whether to keep playing.
     * For a music app this should usually continue (like Spotify).
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (::player.isInitialized && player.playWhenReady) {
            // Continue playback in background (Spotify-like behavior)
            // Do NOT stop the service here
        } else {
            // No active playback — clean stop
            if (::player.isInitialized) player.stop()
            stopSelf()
        }
    }

    fun playQueue(queue: Queue) {
        currentQueue = queue
        scope.launch {
            queueTitle = null
            queue.preloadItem?.let { preload ->
                player.setMediaItem(preload.toMediaItem())
                player.prepare()
                player.playWhenReady = true
                prefetch(listOf(preload.toMediaItem()), limit = 1)
            }

            val status = withContext(Dispatchers.IO) { queue.getInitialStatus() }
            if (status.items.isEmpty()) return@launch
            queueTitle = status.title
            if (queue.preloadItem != null) {
                val start = status.startIndex.coerceIn(status.items.indices)
                val before = status.items.take(start)
                val after = status.items.drop(start + 1)
                if (before.isNotEmpty()) {
                    player.addMediaItems(0, before)
                }
                if (after.isNotEmpty()) {
                    player.addMediaItems(after)
                }
            } else {
                player.setMediaItems(status.items, status.startIndex, status.position)
                player.prepare()
                player.playWhenReady = true
            }
            prefetch(status.items.drop(status.startIndex).take(4))
            loadMoreQueueItemsIfNeeded()
        }
    }

    fun playNext(items: List<MediaItem>) = onPlayerThread {
        if (items.isNotEmpty()) {
            val insertIndex = (player.currentMediaItemIndex + 1).coerceAtLeast(0).coerceAtMost(player.mediaItemCount)
            player.addMediaItems(insertIndex, items)
        }
    }

    fun addToQueue(items: List<MediaItem>) = onPlayerThread {
        if (items.isNotEmpty()) player.addMediaItems(items)
    }

    fun prefetch(mediaItems: List<MediaItem>, limit: Int = 4) {
        val ids = mediaItems
            .map { it.mediaId.takeIf { id -> id.isNotBlank() } ?: it.localConfiguration?.uri?.toString().orEmpty() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(limit)
        if (ids.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            ids.forEach { mediaId ->
                runCatching { resolveStreamUrl(mediaId) }
                    .onFailure { Timber.e(it, "Unable to prefetch Casty stream for $mediaId") }
            }
        }
    }

    fun cacheForOffline(mediaItems: List<MediaItem>, limit: Int = 50) {
        val ids = mediaItems
            .map { it.mediaId.takeIf { id -> id.isNotBlank() } ?: it.localConfiguration?.uri?.toString().orEmpty() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(limit)
        if (ids.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            ids.forEach { mediaId ->
                runCatching {
                    val dataSource = createCacheDataSourceFactory().createDataSource() as CacheDataSource
                    val dataSpec = DataSpec.Builder()
                        .setUri(resolveStreamUrl(mediaId).toUri())
                        .setKey(mediaId)
                        .build()
                    CacheWriter(dataSource, dataSpec, null, null).cache()
                }.onFailure {
                    Timber.e(it, "Unable to cache Casty stream for $mediaId")
                }
            }
        }
    }

    fun toggleLike() {
        // Casty owns visible library state for now; Casty account feedback can be wired here.
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val upstream = runCatching { createCacheDataSourceFactory() as DataSource.Factory }
            .getOrElse { throwable ->
                Timber.e(throwable, "Casty audio cache unavailable; using network-only playback")
                createNetworkDataSourceFactory()
            }
        return createResolvingDataSourceFactory(upstream)
    }

    private fun createResolvingDataSourceFactory(upstream: DataSource.Factory): DataSource.Factory {
        return ResolvingDataSource.Factory(upstream) { dataSpec ->
            val mediaId = dataSpec.key
                ?: dataSpec.uri.toString().takeIf { it.isNotBlank() }
                ?: error("No media id")

            dataSpec.withUri(resolveStreamUrl(mediaId).toUri())
        }
    }

    private fun createNetworkDataSourceFactory(): DataSource.Factory =
        DefaultDataSource.Factory(
            this,
            OkHttpDataSource.Factory(httpClient),
        )

    private fun createCacheDataSourceFactory(): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(CastyAudioCache.get(this))
            .setUpstreamDataSourceFactory(createNetworkDataSourceFactory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun resolveStreamUrl(mediaId: String): String {
        val now = System.currentTimeMillis()
        streamUrlCache[mediaId]?.takeIf { it.second - STREAM_URL_EXPIRY_SKEW_MS > now }?.let {
            return it.first
        }

        val playbackData = runBlocking(Dispatchers.IO) {
            YTPlayerUtils.playerResponseForPlayback(
                videoId = mediaId,
                audioQuality = readAudioQuality(),
                connectivityManager = getSystemService<ConnectivityManager>() ?: error("No connectivity manager"),
            )
        }.getOrElse { throwable ->
            throw throwable.toPlaybackException()
        }

        streamUrlCache[mediaId] =
            playbackData.streamUrl to now + (playbackData.streamExpiresInSeconds * 1000L)
        return playbackData.streamUrl
    }

    private fun loadMoreQueueItemsIfNeeded() {
        val queue = currentQueue ?: return
        if (isLoadingMoreQueue || !queue.hasNextPage()) return
        if (!::player.isInitialized || player.mediaItemCount == 0) return
        if (player.mediaItemCount - player.currentMediaItemIndex > 5) return

        isLoadingMoreQueue = true
        scope.launch {
            try {
                val moreItems = withContext(Dispatchers.IO) { queue.nextPage() }
                if (moreItems.isNotEmpty()) {
                    player.addMediaItems(moreItems)
                    prefetch(moreItems.take(4))
                }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Unable to extend Casty queue")
            } finally {
                isLoadingMoreQueue = false
            }
        }
    }

    private fun readAudioQuality(): AudioQuality =
        currentAudioQuality

    private fun Throwable.toPlaybackException(): PlaybackException =
        when (this) {
            is PlaybackException -> this
            is ConnectException, is UnknownHostException -> PlaybackException(
                "No internet connection",
                this,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            )
            is SocketTimeoutException -> PlaybackException(
                "Connection timed out",
                this,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            )
            else -> PlaybackException(
                message ?: "Unable to resolve Casty stream",
                this,
                PlaybackException.ERROR_CODE_REMOTE_ERROR,
            )
        }

    private fun observeCastySession() {
        scope.launch(Dispatchers.IO) {
            try {
                dataStore.data.collect { settings ->
                    val migratedCookie = settings[InnerTubeCookieKey].takeUnless { it.isNullOrBlank() }
                    val migratedVisitorData = settings[VisitorDataKey].takeUnless { it.isNullOrBlank() || it == "null" }
                    val migratedDataSyncId = settings[DataSyncIdKey].takeUnless { it.isNullOrBlank() || it == "null" }
                    if (secureSessionStore.innerTubeCookie.isNullOrBlank() && !migratedCookie.isNullOrBlank()) {
                        secureSessionStore.updateSession(
                            innerTubeCookie = migratedCookie,
                            visitorData = migratedVisitorData,
                            dataSyncId = migratedDataSyncId,
                        )
                        dataStore.edit { preferences ->
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
                        YouTube.visitorData().onSuccess { visitor ->
                            YouTube.visitorData = visitor
                            secureSessionStore.visitorData = visitor
                        }.onFailure {
                            Timber.e(it, "Unable to fetch Casty visitorData")
                        }
                    }
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                Timber.e(throwable, "Casty session observer failed")
            }
        }
    }

    private fun observePlaybackSettings() {
        scope.launch {
            try {
                dataStore.data
                    .map { settings ->
                        PlaybackSettings(
                            skipSilence = settings[SkipSilenceKey] ?: false,
                            audioQuality = settings[AudioQualityKey].toAudioQualityOrDefault(),
                        )
                    }
                    .distinctUntilChanged()
                    .collect { settings ->
                        currentAudioQuality = settings.audioQuality
                        if (::player.isInitialized) {
                            player.setSkipSilenceEnabled(settings.skipSilence)
                        }
                    }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                currentAudioQuality = AudioQuality.VERY_HIGH
                Timber.e(throwable, "Casty playback settings observer failed")
            }
        }
    }

    private fun String?.toAudioQualityOrDefault(): AudioQuality =
        this?.let { value ->
            runCatching { AudioQuality.valueOf(value) }
                .onFailure { Timber.w(it, "Ignoring invalid Casty audio quality preference: $value") }
                .getOrNull()
        } ?: AudioQuality.VERY_HIGH

    private fun onPlayerThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            scope.launch { action() }
        }
    }

    private companion object {
        private const val NETWORK_CONNECT_TIMEOUT_SECONDS = 10L
        private const val NETWORK_READ_TIMEOUT_SECONDS = 30L
        private const val NETWORK_WRITE_TIMEOUT_SECONDS = 30L
        private const val STREAM_URL_EXPIRY_SKEW_MS = 5 * 60 * 1000L
        private const val NOW_PLAYING_NOTIFICATION_ID = 8042
        private const val NOW_PLAYING_CHANNEL_ID = "casty_now_playing"
    }

    private data class PlaybackSettings(
        val skipSilence: Boolean,
        val audioQuality: AudioQuality,
    )
}
