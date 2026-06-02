package com.casty.music.backend.playback.queues

import androidx.media3.common.MediaItem
import com.casty.music.backend.models.MediaMetadata

class ListQueue(
    private val title: String? = null,
    private val items: List<MediaItem>,
    private val startIndex: Int = 0,
    private val position: Long = 0L,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    override suspend fun getInitialStatus(): Queue.Status =
        Queue.Status(title, items, items.safeStartIndex(startIndex), position)

    private fun List<MediaItem>.safeStartIndex(index: Int): Int =
        if (isEmpty()) 0 else index.coerceIn(indices)
}
