package com.casty.music.backend.playback.queues

import androidx.media3.common.MediaItem
import com.casty.music.backend.innertube.YouTube
import com.casty.music.backend.models.MediaMetadata
import com.casty.music.backend.models.toMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubePlaylistQueue(
    private val playlistId: String,
    private val playlistTitle: String? = null,
    private val initialItems: List<MediaItem> = emptyList(),
    private val startIndex: Int = 0,
    private var continuation: String? = null,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    override suspend fun getInitialStatus(): Queue.Status = withContext(Dispatchers.IO) {
        if (initialItems.isNotEmpty()) {
            return@withContext Queue.Status(
                title = playlistTitle,
                items = initialItems,
                startIndex = initialItems.safeStartIndex(startIndex),
                position = 0L,
            )
        }

        val page = YouTube.playlist(playlistId).getOrThrow()
        continuation = page.songsContinuation
        val items = page.songs.map { it.toMediaItem() }
        Queue.Status(
            title = page.playlist.title,
            items = items,
            startIndex = items.safeStartIndex(startIndex),
            position = 0L,
        )
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(Dispatchers.IO) {
        val token = continuation ?: return@withContext emptyList()
        val page = YouTube.playlistContinuation(token).getOrThrow()
        continuation = page.continuation
        page.songs.map { it.toMediaItem() }
    }

    private fun List<MediaItem>.safeStartIndex(index: Int): Int =
        if (isEmpty()) 0 else index.coerceIn(indices)
}
