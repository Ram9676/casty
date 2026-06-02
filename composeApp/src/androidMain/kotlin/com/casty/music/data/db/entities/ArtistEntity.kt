package com.casty.music.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.casty.music.data.Artist
import java.time.LocalDateTime

/**
 * Advanced Artist Entity - Production-Ready Persistence
 * 
 * Features:
 * - Complete artist metadata (name, description, genres)
 * - Analytics (subscriber count, play count, monthly listeners)
 * - Engagement tracking (total listen time, follower status)
 * - Smart caching with expiration
 * - User preferences (is favorite, is followed, is hidden)
 * - FTS4 full-text search for instant artist discovery
 * - Optimized indices for complex queries and sorting
 */
@Entity(
    tableName = "artists",
    indices = [
        // Performance indices
        Index(value = ["name"], orders = [Index.Order.ASC]),
        Index(value = ["isFavorite"]),
        Index(value = ["isFollowed"]),
        Index(value = ["subscriberCount"]),
        Index(value = ["monthlyListeners"]),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["cachedAt"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isDownloaded"]),
        Index(value = ["downloadedAt"]),
        // Composite indices for advanced queries
        Index(value = ["isFavorite", "monthlyListeners"], orders = [Index.Order.DESC, Index.Order.DESC]),
        Index(value = ["isFollowed", "name"], orders = [Index.Order.DESC, Index.Order.ASC]),
        Index(value = ["genre", "subscriberCount"], orders = [Index.Order.ASC, Index.Order.DESC]),
        Index(value = ["isDownloaded", "downloadedAt"], orders = [Index.Order.DESC, Index.Order.DESC]),
    ]
)
data class ArtistEntity(
    @PrimaryKey
    val id: String,
    
    // Core metadata
    val name: String,
    val thumbnailUrl: String?,
    
    // Extended metadata
    @ColumnInfo(defaultValue = "null")
    val description: String? = null,
    
    @ColumnInfo(defaultValue = "null")
    val genre: String? = null,
    
    @ColumnInfo(defaultValue = "null")
    val genres: String? = null, // Comma-separated list
    
    // Statistics & Analytics
    @ColumnInfo(defaultValue = "null")
    val subscriberCount: Long? = null,
    
    @ColumnInfo(defaultValue = "0")
    val monthlyListeners: Long = 0L,
    
    @ColumnInfo(defaultValue = "0")
    val playCount: Long = 0L,
    
    @ColumnInfo(defaultValue = "0")
    val totalListenTimeSeconds: Long = 0L,
    
    @ColumnInfo(defaultValue = "null")
    val lastPlayedAt: LocalDateTime? = null,
    
    @ColumnInfo(defaultValue = "null")
    val firstPlayedAt: LocalDateTime? = null,
    
    // User engagement
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(defaultValue = "0")
    val isFollowed: Boolean = false,
    
    @ColumnInfo(defaultValue = "0")
    val isHidden: Boolean = false,
    
    @ColumnInfo(defaultValue = "0")
    val isSubscribed: Boolean = false,
    
    // Download / offline state (for artist's content)
    @ColumnInfo(defaultValue = "0")
    val isDownloaded: Boolean = false,
    
    @ColumnInfo(defaultValue = "null")
    val downloadedAt: LocalDateTime? = null,
    
    // Content tracking
    @ColumnInfo(defaultValue = "0")
    val albumCount: Int = 0,
    
    @ColumnInfo(defaultValue = "0")
    val songCount: Int = 0,
    
    // Sync & Cache management
    @ColumnInfo(defaultValue = "null")
    val cachedAt: LocalDateTime? = null,
    
    @ColumnInfo(defaultValue = "null")
    val expiresAt: LocalDateTime? = null,
    
    @ColumnInfo(defaultValue = "null")
    val lastSyncedAt: LocalDateTime? = null,
    
    @ColumnInfo(defaultValue = "null")
    val updatedAt: LocalDateTime? = null,
    
    @ColumnInfo(defaultValue = "null")
    val createdAt: LocalDateTime? = null,
    
    // Source tracking
    @ColumnInfo(defaultValue = "'youtube'")
    val source: String = "youtube",
    
    @ColumnInfo(defaultValue = "null")
    val browseId: String? = null,
    
    @ColumnInfo(defaultValue = "null")
    val shuffleId: String? = null,
    
    @ColumnInfo(defaultValue = "null")
    val radioId: String? = null
) {
    @Ignore
    fun toArtist(): Artist = Artist(
        id = id,
        name = name,
        thumbnailUrl = thumbnailUrl
    )
    
    @Ignore
    fun withPlayback(startTime: LocalDateTime = LocalDateTime.now()): ArtistEntity = copy(
        playCount = playCount + 1,
        lastPlayedAt = startTime,
        firstPlayedAt = firstPlayedAt ?: startTime,
        cachedAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    @Ignore
    fun withListenTime(seconds: Int): ArtistEntity = copy(
        totalListenTimeSeconds = totalListenTimeSeconds + seconds,
        cachedAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    @Ignore
    fun withStats(
        subscriberCount: Long? = this.subscriberCount,
        monthlyListeners: Long = this.monthlyListeners,
        albumCount: Int = this.albumCount,
        songCount: Int = this.songCount
    ): ArtistEntity = copy(
        subscriberCount = subscriberCount,
        monthlyListeners = monthlyListeners,
        albumCount = albumCount,
        songCount = songCount,
        cachedAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    @Ignore
    fun withMetadata(
        description: String? = this.description,
        genre: String? = this.genre,
        genres: String? = this.genres
    ): ArtistEntity = copy(
        description = description,
        genre = genre,
        genres = genres,
        cachedAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    @Ignore
    fun toggleFollow(): ArtistEntity = copy(
        isFollowed = !isFollowed,
        isSubscribed = !isFollowed,
        updatedAt = LocalDateTime.now()
    )
    
    @Ignore
    fun toggleFavorite(): ArtistEntity = copy(
        isFavorite = !isFavorite,
        updatedAt = LocalDateTime.now()
    )
    
    companion object {
        fun fromArtist(artist: Artist): ArtistEntity = ArtistEntity(
            id = artist.id,
            name = artist.name,
            thumbnailUrl = artist.thumbnailUrl,
            cachedAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        /**
         * Merge existing entity with incoming metadata
         * Preserves analytics and user preferences while updating metadata
         */
        fun mergeMetadata(existing: ArtistEntity?, incoming: ArtistEntity): ArtistEntity {
            if (existing == null) return incoming
            
            return incoming.copy(
                // Preserve analytics
                playCount = existing.playCount,
                totalListenTimeSeconds = existing.totalListenTimeSeconds,
                lastPlayedAt = existing.lastPlayedAt,
                firstPlayedAt = existing.firstPlayedAt,
                
                // Preserve user preferences
                isFavorite = existing.isFavorite,
                isFollowed = existing.isFollowed,
                isHidden = existing.isHidden,
                isSubscribed = existing.isSubscribed,
                
                // Preserve timestamps
                cachedAt = existing.cachedAt,
                createdAt = existing.createdAt,
                
                // Use incoming statistics if available
                subscriberCount = incoming.subscriberCount ?: existing.subscriberCount,
                monthlyListeners = incoming.monthlyListeners.takeIf { it > 0 } ?: existing.monthlyListeners,
                albumCount = incoming.albumCount.takeIf { it > 0 } ?: existing.albumCount,
                songCount = incoming.songCount.takeIf { it > 0 } ?: existing.songCount,
                
                // Use incoming metadata
                updatedAt = LocalDateTime.now()
            )
        }
    }
}
