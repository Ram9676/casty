@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.casty.music.backend.playback

import android.content.Context
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.casty.music.backend.playback.MusicService.MusicBinder
import com.casty.music.backend.playback.queues.Queue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerConnection(
    @Suppress("UNUSED_PARAMETER") context: Context,
    binder: MusicBinder,
    scope: CoroutineScope,
) : Player.Listener {
    val service = binder.service
    val player: ExoPlayer
        get() = service.player

    val playbackState = MutableStateFlow(player.playbackState)
    private val playWhenReady = MutableStateFlow(player.playWhenReady)
    val isPlaying = combine(playbackState, playWhenReady) { state, ready ->
        ready && state != Player.STATE_ENDED && state != Player.STATE_IDLE
    }.stateIn(scope, SharingStarted.Eagerly, player.isPlaying)

    val currentMediaItem = MutableStateFlow(player.currentMediaItem)
    val currentMediaItemIndex = MutableStateFlow(player.currentMediaItemIndex)
    val shuffleModeEnabled = MutableStateFlow(player.shuffleModeEnabled)
    val repeatMode = MutableStateFlow(player.repeatMode)
    val error = MutableStateFlow<androidx.media3.common.PlaybackException?>(null)

    init {
        player.addListener(this)
    }

    fun playQueue(queue: Queue) = onPlayerThread { service.playQueue(queue) }
    fun playNext(item: MediaItem) = playNext(listOf(item))
    fun playNext(items: List<MediaItem>) = onPlayerThread { service.playNext(items) }
    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))
    fun addToQueue(items: List<MediaItem>) = onPlayerThread { service.addToQueue(items) }
    fun toggleLike() = service.toggleLike()

    fun play() = onPlayerThread {
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.playWhenReady = true
        snapshot()
    }

    fun pause() = onPlayerThread {
        player.playWhenReady = false
        snapshot()
    }

    fun togglePlayPause() = onPlayerThread {
        if (player.isPlaying) {
            player.playWhenReady = false
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.playWhenReady = true
        }
        snapshot()
    }

    fun seekTo(position: Long) = onPlayerThread {
        player.seekTo(position)
        snapshot()
    }

    fun seekToNext() = onPlayerThread {
        player.seekToNextMediaItem()
        player.playWhenReady = true
        snapshot()
    }

    fun seekToPrevious() = onPlayerThread {
        player.seekToPreviousMediaItem()
        player.playWhenReady = true
        snapshot()
    }

    override fun onEvents(player: Player, events: Player.Events) = snapshot()
    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        this.error.value = error
    }

    private fun onPlayerThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            service.mainScope.launch { action() }
        }
    }

    private fun snapshot() {
        playbackState.value = player.playbackState
        playWhenReady.value = player.playWhenReady
        currentMediaItem.value = player.currentMediaItem
        currentMediaItemIndex.value = player.currentMediaItemIndex
        shuffleModeEnabled.value = player.shuffleModeEnabled
        repeatMode.value = player.repeatMode
    }
}
