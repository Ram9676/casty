package com.casty.music.backend.playback.queues

import androidx.media3.common.MediaItem
import com.casty.music.backend.models.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?
        get() = null

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean = false

    suspend fun nextPage(): List<MediaItem> = emptyList()

    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val startIndex: Int,
        val position: Long,
    )
}
