package com.casty.music.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.casty.music.data.Album
import java.time.LocalDateTime

/**
 * Advanced Album Entity - Production-Ready Persistence
 * 
 * Features:
 * - Full metadata storage (title, artists, year, genre, label)
 * - Analytics (play count, last played, total listen time)
 * - Smart caching (cached at, expires at for stale data detection)
 * - User preferences (is favorite, is hidden)
 * - FTS4 search integration for instant album discovery
 * - Optimized indices for complex queries
 */
@Entity(
    tableName = "albums",
    indices = [
        // Performance indices
        Index(value = ["title"], order = Index.Order.ASC),
        Index(value = ["authorsText"], order = Index.Order.ASC),
        Index(value = ["year"]),
        Index(value = ["isFavorite"]),
        Index(value = ["lastPlayedAt"]),
        Index(value = ["cachedAt"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isDownloaded"]),
        Index(value = ["downloadedAt"]),
        // Composite indices for advanced queries
        Index(value = ["isFavorite", "lastPlayedAt"], order = [Index.Order.DESC, Index.Order.DESC]),
        Index(value = ["year", "title"], order = [Index.Order.DESC, Index.Order.ASC]),
        Index(value = ["isDownloaded", "downloadedAt"], order = [Index.Order.DESC, Index.Order.DESC]),
    ]
)
@Fts4(contentEntity = AlbumEntity::class)
data class AlbumEntity(
    @PrimaryKey
    val id: String,
    
    // Core metadata
    val title: String?,
    val authorsText: String?,
    val thumbnailUrl: String?,
    
    // Extended metadata (advanced features)
    @ColumnInfo(defaultValue = "null")
    val year: Int? = null,
    
    @ColumnInfo(defaultValue = "null")
    val genre: String? = null,
    
    @ColumnInfo(defaultValue = "null")
    val label: String? = null,
    
    @ColumnInfo(defaultValue = "null")
    val description: String? = null,
    
    // Track information
    @ColumnInfo(defaultValue = "0")
    val trackCount: Int = 0,
    
    @ColumnInfo(defaultValue = "0")
    val durationSeconds: Int = 0,
    
    // Analytics & Engagement
    @ColumnInfo(defaultValue = "0")
    val playCount: Long = 0L,
    
    @ColumnInfo(defaultValue = "0")
    val totalListenTimeSeconds: Long = 0L,
    
    @ColumnInfo(defaultValue = "null")
    val lastPlayedAt: LocalDateTime? = null,
    
    @ColumnInfo(defaultValue = "null")
    val firstPlayedAt: LocalDateTime? = null,
    
    // User preferences
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(defaultValue = "0")
    val isHidden: Boolean = false,
    
    // Download / offline state
    @ColumnInfo(defaultValue = "0")
    val isDownloaded: Boolean = false,
    
    @ColumnInfo(defaultValue = "null")
    val downloadedAt: LocalDateTime? = null,
    
    @ColumnInfo(defaultValue = "null")
    val downloadPath: String? = null,
    
    @ColumnInfo(defaultValue = "null")
    val cachedSizeBytes: Long? = null,
    
    @ColumnInfo(defaultValue = "null")
    val downloadQuality: String? = null,
    
    @ColumnInfo(defaultValue = "null")
    val offlineExpiry: LocalDateTime? = null,
    
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
    val audioPlaylistId: String? = null
) {
    @Ignore
    fun toAlbum(): Album = Album(
        id = id,
        title = title,
        authorsText = authorsText,
        thumbnailUrl = thumbnailUrl
    )
    
    @Ignore
    fun withPlayback(startTime: LocalDateTime = LocalDateTime.now()): AlbumEntity = copy(
        playCount = playCount + 1,
        lastPlayedAt = startTime,
        firstPlayedAt = firstPlayedAt ?: startTime,
        cachedAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    @Ignore
    fun withListenTime(seconds: Int): AlbumEntity = copy(
        totalListenTimeSeconds = totalListenTimeSeconds + seconds,
        cachedAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    @Ignore
    fun withMetadata(
        year: Int? = this.year,
        genre: String? = this.genre,
        label: String? = this.label,
        description: String? = this.description,
        trackCount: Int = this.trackCount,
        durationSeconds: Int = this.durationSeconds
    ): AlbumEntity = copy(
        year = year,
        genre = genre,
        label = label,
        description = description,
        trackCount = trackCount,
        durationSeconds = durationSeconds,
        cachedAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
    
    companion object {
        fun fromAlbum(album: Album): AlbumEntity = AlbumEntity(
            id = album.id,
            title = album.title,
            authorsText = album.authorsText,
            thumbnailUrl = album.thumbnailUrl,
            cachedAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        /**
         * Merge existing entity with incoming metadata
         * Preserves analytics while updating metadata
         */
        fun mergeMetadata(existing: AlbumEntity?, incoming: AlbumEntity): AlbumEntity {
            if (existing == null) return incoming
            
            return incoming.copy(
                // Preserve analytics
                playCount = existing.playCount,
                totalListenTimeSeconds = existing.totalListenTimeSeconds,
                lastPlayedAt = existing.lastPlayedAt,
                firstPlayedAt = existing.firstPlayedAt,
                
                // Preserve user preferences
                isFavorite = existing.isFavorite,
                isHidden = existing.isHidden,
                isDownloaded = existing.isDownloaded,
                
                // Preserve timestamps
                cachedAt = existing.cachedAt,
                createdAt = existing.createdAt,
                
                // Use incoming metadata
                updatedAt = LocalDateTime.now()
            )
        }
    }
}
