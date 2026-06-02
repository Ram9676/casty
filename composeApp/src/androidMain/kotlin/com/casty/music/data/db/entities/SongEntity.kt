package com.casty.music.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Core song entity for Casty.
 *
 * Best-ever design notes:
 * - Uses YouTube videoId as stable primary key.
 * - likedAt is the source of truth for "liked" state (synced with YTM when possible).
 * - download* fields enable first-class offline library experience.
 * - playCount + lastPlayedAt power smart sorting, stats, and "recently played".
 * - thumbnailUrl is cached at save time for offline resilience.
 */
@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artistsText"]),
        Index(value = ["likedAt"]),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["isDownloaded"]),
        Index(value = ["inLibrary"]),
        Index(value = ["inWatchLater"])
    ]
)
data class SongEntity(
    @PrimaryKey
    val id: String,                    // YouTube videoId (stable)

    val title: String,
    val artistsText: String? = null,
    val durationText: String? = null,
    val thumbnailUrl: String? = null,

    // Library state
    val likedAt: LocalDateTime? = null,
    val inLibrary: Boolean = false,
    val librarySyncedAt: LocalDateTime? = null,
    val inWatchLater: Boolean = false,
    val watchLaterAddedAt: LocalDateTime? = null,

    // Download / offline state (populated by CastyAudioCache + user action)
    val isDownloaded: Boolean = false,
    val downloadedAt: LocalDateTime? = null,
    val cachedSizeBytes: Long? = null, // future: actual on-disk size

    // Playback history & stats
    val lastPlayedAt: LocalDateTime? = null,
    val playCount: Int = 0,

    // Metadata for future features (radio seeds, taste, etc.)
    val albumId: String? = null,
    val albumTitle: String? = null,
    val year: String? = null,
    val explicit: Boolean = false,

    // Timestamps for sync / conflict resolution
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
