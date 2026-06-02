package com.casty.music.data.db.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.casty.music.data.db.converters.Converters
import java.time.LocalDateTime

/**
 * Advanced Song Entity - Production-Ready Design
 * 
 * Enhanced features:
 * - FTS5 full-text search support for instant search
 * - Composite indices for complex queries
 * - Audio fingerprint hash for duplicate detection
 * - Advanced caching metadata
 * - Cloud sync timestamps for multi-device support
 * - Playback analytics data
 * - Dynamic quality tracking
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
        Index(value = ["inWatchLater"]),
        Index(value = ["albumId"]),
        Index(value = ["playCount"]),
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"]),
        Index(value = ["explicit"]),
        Index(value = ["durationMs"]),
        // Composite indices for advanced queries
        Index(value = ["likedAt", "lastPlayedAt"]),
        Index(value = ["inLibrary", "librarySyncedAt"]),
        Index(value = ["isDownloaded", "downloadedAt"]),
        Index(value = ["playCount", "lastPlayedAt"]),
        Index(value = ["artistsText", "title"]),
        Index(value = ["inLibrary", "likedAt"]),
        Index(value = ["isDownloaded", "offlineExpiry"]),
        Index(value = ["energyLevel", "moodTags"])
    ]
)
@TypeConverters(Converters::class)
data class SongEntity(
    @PrimaryKey
    val id: String,                    // YouTube videoId (stable)

    // Core metadata
    val title: String,
    val artistsText: String? = null,
    val albumTitle: String? = null,
    val albumId: String? = null,
    val durationMs: Long = 0L,         // Duration in milliseconds for precision
    val durationText: String? = null,  // Human-readable format
    val thumbnailUrl: String? = null,
    val highResThumbnailUrl: String? = null, // For retina displays
    
    // Audio quality & formats
    val audioQuality: String = "AUTO", // AUTO, LOW, MEDIUM, HIGH, LOSSLESS
    val availableFormats: List<String> = emptyList(),
    val currentFormat: String? = null,
    
    // Content metadata
    val genre: String? = null,
    val year: String? = null,
    val explicit: Boolean = false,
    val isLive: Boolean = false,
    val isPremiere: Boolean = false,
    val language: String? = null,      // ISO 639-1 code
    
    // Library state - source of truth
    val likedAt: LocalDateTime? = null,
    val inLibrary: Boolean = false,
    val librarySyncedAt: LocalDateTime? = null,
    val inWatchLater: Boolean = false,
    val watchLaterAddedAt: LocalDateTime? = null,
    val isHidden: Boolean = false,     // User-hidden songs
    
    // Download / offline state
    val isDownloaded: Boolean = false,
    val downloadedAt: LocalDateTime? = null,
    val downloadPath: String? = null,  // Local file path
    val cachedSizeBytes: Long? = null,
    val downloadQuality: String? = null,
    val offlineExpiry: LocalDateTime? = null, // DRM expiry
    
    // Playback history & smart analytics
    val lastPlayedAt: LocalDateTime? = null,
    val playCount: Int = 0,
    val totalPlayTimeMs: Long = 0L,    // Total time spent listening
    val skipCount: Int = 0,            // Times skipped
    val completionRate: Float = 0.8f,  // Average completion percentage
    val lastSkipPositionMs: Long = 0L, // Where user typically skips
    
    // Smart recommendations data
    val similarityHash: String? = null, // Audio fingerprint for similar songs
    val moodTags: List<String> = emptyList(),
    val energyLevel: Float = 0.5f,      // 0.0 (calm) to 1.0 (energetic)
    val danceability: Float = 0.5f,
    val acousticness: Float = 0.5f,
    
    // Social & sharing
    val shareCount: Int = 0,
    val userRating: Float = 0f,         // 0-5 stars
    val userReview: String? = null,
    
    // Cloud sync (for multi-device)
    val cloudSynced: Boolean = false,
    val cloudLastSyncedAt: LocalDateTime? = null,
    val cloudVersion: Long = 0L,
    val deviceId: String? = null,       // Last device that modified
    
    // Internal tracking
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val version: Int = 1                // For optimistic locking
) {
    // Computed properties for convenience
    @get:Ignore
    val isLiked: Boolean get() = likedAt != null

    @get:Ignore
    val isInLibrary: Boolean get() = inLibrary || likedAt != null

    @get:Ignore
    val durationSeconds: Long get() = durationMs / 1000

    @get:Ignore
    val formattedDuration: String get() = formatDuration(durationMs)

    @get:Ignore
    val needsSync: Boolean get() = cloudVersion > 0 && !cloudSynced

    @get:Ignore
    val isRecentlyPlayed: Boolean get() = lastPlayedAt?.isAfter(LocalDateTime.now().minusDays(7)) ?: false

    @get:Ignore
    val isTrending: Boolean get() = playCount > 10 && lastPlayedAt?.isAfter(LocalDateTime.now().minusDays(30)) ?: false
    
    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
            else -> String.format("%d:%02d", minutes, seconds % 60)
        }
    }
    
    // Smart copy helpers
    @Ignore
    fun withPlayback(positionMs: Long, completed: Boolean): SongEntity {
        val now = LocalDateTime.now()
        val completionRatio = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            completionRate
        }
        val shouldCountPlay = completed || (durationMs > 0L && positionMs >= (durationMs * 0.7).toLong())
        val newPlayCount = if (shouldCountPlay) playCount + 1 else playCount
        val newTotalTime = totalPlayTimeMs + positionMs
        val samples = playCount.coerceAtLeast(0) + 1
        
        return copy(
            lastPlayedAt = now,
            playCount = newPlayCount,
            totalPlayTimeMs = newTotalTime,
            completionRate = if (durationMs > 0L) {
                ((completionRate * playCount.coerceAtLeast(0)) + completionRatio) / samples
            } else {
                completionRate
            },
            updatedAt = now,
            version = version + 1
        )
    }
    
    @Ignore
    fun withSkip(positionMs: Long): SongEntity {
        val now = LocalDateTime.now()
        return copy(
            skipCount = skipCount + 1,
            lastSkipPositionMs = positionMs,
            updatedAt = now,
            version = version + 1
        )
    }
}
