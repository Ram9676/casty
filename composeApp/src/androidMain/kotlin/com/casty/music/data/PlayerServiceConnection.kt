@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.casty.music.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import dagger.hilt.android.qualifiers.ApplicationContext
import com.casty.music.backend.playback.MusicService
import com.casty.music.backend.playback.PlayerConnection
import com.casty.music.backend.playback.queues.ListQueue
import com.casty.music.backend.playback.queues.YouTubePlaylistQueue
import com.casty.music.backend.models.MediaMetadata
import com.casty.music.backend.models.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val appContext = context.applicationContext
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isBound = false

    private val _binder = MutableStateFlow<PlayerConnection?>(null)
    val binder: StateFlow<PlayerConnection?> = _binder.asStateFlow()
    val connection: StateFlow<PlayerConnection?> = binder

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = snapshotPlayer(player)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _binder.value?.player?.let(::snapshotPlayer)
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            _binder.value?.player?.let(::snapshotPlayer)
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            _binder.value?.player?.let(::snapshotPlayer)
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackError.value = error.toFriendlyMessage()
            _binder.value?.player?.let(::snapshotPlayer)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val musicBinder = service as? MusicService.MusicBinder ?: return
            if (!musicBinder.service.isReadyForConnections) {
                Timber.e("Casty playback service connected before player initialization completed")
                _playbackError.value = "Playback service is not ready."
                runCatching { appContext.unbindService(this) }
                isBound = false
                _binder.value = null
                return
            }
            _binder.value?.player?.removeListener(playerListener)
            val connection = PlayerConnection(appContext, musicBinder, coroutineScope)
            _binder.value = connection
            connection.player.addListener(playerListener)
            snapshotPlayer(connection.player)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _binder.value?.player?.removeListener(playerListener)
            _binder.value = null
            isBound = false
        }

        override fun onBindingDied(name: ComponentName?) {
            _binder.value?.player?.removeListener(playerListener)
            _binder.value = null
            isBound = false
            connect()
        }
    }

    init {
        coroutineScope.launch {
            while (isActive) {
                _binder.value?.player?.let(::snapshotPlayer)
                delay(500)
            }
        }
    }

    fun connect(startPlaybackService: Boolean = false) {
        if (isBound && _binder.value != null) {
            Timber.d("PlayerServiceConnection already bound and ready")
            return
        }
        
        val intent = Intent(appContext, MusicService::class.java)
        if (startPlaybackService) {
            runCatching { appContext.startService(intent) }
                .onFailure {
                    Timber.e(it, "Unable to start Casty playback service")
                    _playbackError.value = "Playback service could not start."
                }
        }
        if (isBound) return

        Timber.d("Binding to PlayerServiceConnection...")
        runCatching {
            appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }.onSuccess { bound ->
            isBound = bound
            if (!bound) {
                Timber.e("Casty playback service bind returned false")
                _playbackError.value = "Playback service is not available."
            } else {
                Timber.d("PlayerServiceConnection bind successful")
            }
        }.onFailure {
            isBound = false
            Timber.e(it, "Unable to bind Casty playback service")
            _playbackError.value = "Playback service is not available."
        }
    }

    fun playSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    fun playTrack(track: com.casty.music.backend.models.MediaMetadata) {
        playMediaItem(track.toMediaItem())
    }

    fun playQueue(title: String?, tracks: List<com.casty.music.backend.models.MediaMetadata>, startIndex: Int = 0) {
        playMediaItems(title, tracks.map { it.toMediaItem() }, startIndex)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        playMediaItems(null, songs.map { it.asMediaItem }, startIndex)
    }

    fun playYouTubePlaylist(
        playlistId: String,
        title: String?,
        songs: List<Song>,
        startIndex: Int = 0,
        continuation: String? = null,
    ) {
        if (playlistId.isBlank()) {
            playQueue(songs, startIndex)
            return
        }
        val mediaItems = songs.map { it.asMediaItem }
        if (mediaItems.isEmpty()) return
        val boundedIndex = startIndex.coerceIn(mediaItems.indices)
        val preload = songs.getOrNull(boundedIndex)?.asMediaMetadata
        _playbackError.value = null
        withConnectionWhenReady {
            playQueue(
                YouTubePlaylistQueue(
                    playlistId = playlistId,
                    playlistTitle = title,
                    initialItems = mediaItems,
                    startIndex = boundedIndex,
                    continuation = continuation,
                    preloadItem = preload,
                )
            )
        }
    }

    fun playMediaItem(mediaItem: MediaItem) {
        playMediaItems(null, listOf(mediaItem), 0)
    }

    fun playNext(song: Song) {
        withConnectionWhenReady { playNext(song.asMediaItem) }
    }

    fun playNext(track: com.casty.music.backend.models.MediaMetadata) {
        withConnectionWhenReady { playNext(track.toMediaItem()) }
    }

    fun addToQueue(song: Song) {
        withConnectionWhenReady { addToQueue(song.asMediaItem) }
    }

    fun addToQueue(track: com.casty.music.backend.models.MediaMetadata) {
        withConnectionWhenReady { addToQueue(track.toMediaItem()) }
    }

    fun prefetchSongs(songs: List<Song>, limit: Int = 6) {
        val mediaItems = songs
            .asSequence()
            .distinctBy { it.id }
            .take(limit)
            .map { it.asMediaItem }
            .toList()
        if (mediaItems.isEmpty()) return
        withConnectionWhenReady { service.prefetch(mediaItems, limit = limit) }
    }

    fun prefetchTracks(tracks: List<com.casty.music.backend.models.MediaMetadata>, limit: Int = 6) {
        val mediaItems = tracks
            .asSequence()
            .distinctBy { it.id }
            .take(limit)
            .map { it.toMediaItem() }
            .toList()
        if (mediaItems.isEmpty()) return
        withConnectionWhenReady { service.prefetch(mediaItems, limit = limit) }
    }

    fun cacheForOffline(songs: List<Song>, limit: Int = 50) {
        val mediaItems = songs
            .asSequence()
            .distinctBy { it.id }
            .take(limit)
            .map { it.asMediaItem }
            .toList()
        if (mediaItems.isEmpty()) return
        withConnectionWhenReady { service.cacheForOffline(mediaItems, limit = limit) }
    }

    fun play() {
        withConnectionWhenReady { play() }
    }

    fun pause() {
        onPlayerThread { _binder.value?.pause() }
    }

    fun seekTo(positionMs: Long) {
        onPlayerThread { _binder.value?.seekTo(positionMs) }
    }

    fun skipToNext() {
        onPlayerThread { _binder.value?.seekToNext() }
    }

    fun skipToPrevious() {
        onPlayerThread { _binder.value?.seekToPrevious() }
    }

    fun toggleShuffle() {
        onPlayerThread {
            _binder.value?.player?.let { player ->
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                snapshotPlayer(player)
            }
        }
    }

    fun setRepeatMode(repeatMode: Int) {
        onPlayerThread {
            _binder.value?.player?.let { player ->
                player.repeatMode = repeatMode
                snapshotPlayer(player)
            }
        }
    }

    private fun playMediaItems(title: String?, mediaItems: List<MediaItem>, startIndex: Int) {
        if (mediaItems.isEmpty()) return
        _playbackError.value = null
        val boundedIndex = startIndex.coerceIn(mediaItems.indices)
        val preload = mediaItems.getOrNull(boundedIndex)
            ?.localConfiguration
            ?.tag as? MediaMetadata
        withConnectionWhenReady {
            playQueue(ListQueue(title = title, items = mediaItems, startIndex = boundedIndex, preloadItem = preload))
        }
    }

    private fun withConnectionWhenReady(action: PlayerConnection.() -> Unit) {
        coroutineScope.launch {
            Timber.d("withConnectionWhenReady called, current binder: ${_binder.value != null}")
            connect(startPlaybackService = true)
            var attempts = 0
            while (attempts < 120) {
                val connection = _binder.value
                if (connection != null) {
                    Timber.d("Player connection ready, executing action")
                    connection.action()
                    snapshotPlayer(connection.player)
                    return@launch
                }
                attempts++
                if (attempts % 20 == 0) {
                    Timber.w("Waiting for player connection... attempt $attempts")
                }
                delay(50)
            }
            Timber.e("Failed to get player connection after 120 attempts")
            _playbackError.value = "Player service did not become ready. Please try again."
        }
    }

    private fun onPlayerThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            coroutineScope.launch { action() }
        }
    }

    private fun snapshotPlayer(player: Player) {
        _isPlaying.value = player.isPlaying
        _currentMediaItem.value = player.currentMediaItem
        _playbackPosition.value = player.currentPosition.coerceAtLeast(0L)
        _duration.value = player.safeDuration()
        if (player.playbackState == Player.STATE_READY) {
            _playbackError.value = null
        }
    }

    private fun Player.safeDuration(): Long =
        duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L

    private fun PlaybackException.toFriendlyMessage(): String = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "No internet connection."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Connection timed out while loading the stream."
        PlaybackException.ERROR_CODE_REMOTE_ERROR -> message ?: "YouTube Music rejected this stream."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "YouTube Music returned an unavailable stream."
        else -> message ?: "This track could not be played."
    }
}
