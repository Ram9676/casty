package com.casty.music.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.PlayerServiceConnection
import com.casty.music.data.Song
import com.casty.music.data.durationSecondsToText
import com.casty.music.data.durationTextToMillis
import com.casty.music.backend.constants.AudioQuality
import com.casty.music.backend.constants.AudioQualityKey
import com.casty.music.backend.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.casty.music.backend.playback.PlayerConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentMediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0, // 0 = None, 1 = One, 2 = All
    val isLiked: Boolean = false,
    val isHighQuality: Boolean = false,
    val currentSong: Song? = null,
    val playbackError: String? = null,
)

sealed class PlayerEvent {
    object PlayPause : PlayerEvent()
    object SkipNext : PlayerEvent()
    object SkipPrev : PlayerEvent()
    data class SeekTo(val positionMs: Long) : PlayerEvent()
    object ToggleShuffle : PlayerEvent()
    data class SetRepeatMode(val repeatMode: Int) : PlayerEvent()
    object ToggleLike : PlayerEvent()
}

@HiltViewModel
@androidx.annotation.OptIn(UnstableApi::class)
class CastyPlayerViewModel @Inject constructor(
    private val playerConnection: PlayerServiceConnection,
    private val repository: DatabaseRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val isLikedFlow = playerConnection.currentMediaItem
        .flatMapLatest { mediaItem ->
            if (mediaItem != null) {
                repository.getSong(mediaItem.mediaId).map { song ->
                    song?.likedAt != null
                }
            } else {
                flowOf(false)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentSongFlow = playerConnection.currentMediaItem
        .flatMapLatest { mediaItem ->
            if (mediaItem != null) {
                repository.getSong(mediaItem.mediaId).map { song ->
                    song ?: mediaItem.asDisplaySong()
                }
            } else {
                flowOf(null)
            }
        }

    private val audioQualityFlow = context.dataStore.data
        .map { settings ->
            settings[AudioQualityKey]
                ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
                ?: AudioQuality.VERY_HIGH
        }
        .distinctUntilChanged()

    // Expose audio session ID safely for premium visualizers (without touching core playback logic)
    val audioSessionId: StateFlow<Int> = playerConnection.binder
        .map { binder ->
            binder?.player?.audioSessionId ?: 0
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val uiState: StateFlow<PlayerUiState> = combine(
        playerConnection.currentMediaItem,
        playerConnection.isPlaying,
        playerConnection.playbackPosition,
        playerConnection.duration,
        playerConnection.binder,
        isLikedFlow,
        currentSongFlow,
        playerConnection.playbackError,
        audioQualityFlow,
    ) { array ->
        val currentMediaItem = array[0] as? MediaItem
        val isPlaying = array[1] as Boolean
        val playbackPosition = array[2] as Long
        val duration = array[3] as Long
        val binder = array[4] as? PlayerConnection
        val isLiked = array[5] as Boolean
        val currentSong = array[6] as? Song
        val playbackError = array[7] as? String
        val audioQuality = array[8] as AudioQuality

        PlayerUiState(
            currentMediaItem = currentMediaItem,
            isPlaying = isPlaying,
            progressMs = playbackPosition,
            durationMs = duration.takeIf { it > 0L }
                ?: currentSong?.durationText?.let(::durationTextToMillis)
                ?: 0L,
            isShuffleEnabled = binder?.player?.shuffleModeEnabled ?: false,
            repeatMode = binder?.player?.repeatMode ?: 0,
            isLiked = isLiked,
            isHighQuality = audioQuality == AudioQuality.HIGH || audioQuality == AudioQuality.VERY_HIGH,
            currentSong = currentSong,
            playbackError = playbackError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState()
    )

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.PlayPause -> {
                if (uiState.value.isPlaying) {
                    playerConnection.pause()
                } else {
                    playerConnection.play()
                }
            }
            is PlayerEvent.SkipNext -> {
                playerConnection.skipToNext()
            }
            is PlayerEvent.SkipPrev -> {
                playerConnection.skipToPrevious()
            }
            is PlayerEvent.SeekTo -> {
                playerConnection.seekTo(event.positionMs)
            }
            is PlayerEvent.ToggleShuffle -> {
                playerConnection.toggleShuffle()
            }
            is PlayerEvent.SetRepeatMode -> {
                playerConnection.setRepeatMode(event.repeatMode)
            }
            is PlayerEvent.ToggleLike -> {
                uiState.value.currentSong?.let { song ->
                    viewModelScope.launch {
                        repository.toggleLike(song)
                    }
                }
            }
        }
    }

    private fun MediaItem.asDisplaySong(): Song {
        val metadata = mediaMetadata
        @Suppress("DEPRECATION")
        val taggedMetadata = requestMetadata.extras?.getSerializable("casty_metadata") as? com.casty.music.backend.models.MediaMetadata
        val durationText = metadata.extras?.getString("durationText")
        return Song(
            id = mediaId,
            title = taggedMetadata?.title
                ?: metadata.title?.toString().orEmpty().ifBlank { mediaId },
            artistsText = taggedMetadata?.artists?.joinToString(", ") { it.name }
                ?: metadata.artist?.toString(),
            durationText = durationText ?: taggedMetadata?.duration?.let(::durationSecondsToText),
            thumbnailUrl = taggedMetadata?.thumbnailUrl
                ?: metadata.artworkUri?.toString()
                ?: metadata.extras?.getString("artwork_uri"),
        )
    }
}
