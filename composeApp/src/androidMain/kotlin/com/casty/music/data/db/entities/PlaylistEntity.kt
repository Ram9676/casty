package com.casty.music.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Playlist entity.
 * Supports both local playlists and YouTube-synced ones.
 */
@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isYoutubePlaylist"]),
        Index(value = ["updatedAt"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey
    val id: String,                    // "local-xxx" or real YouTube playlist ID

    val name: String,
    val isYoutubePlaylist: Boolean = false,
    val thumbnailUrl: String? = null,
    val authorText: String? = null,
    val description: String? = null,

    val songCount: Int = 0,            // denormalized for fast UI (kept in sync via triggers or app logic)

    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
