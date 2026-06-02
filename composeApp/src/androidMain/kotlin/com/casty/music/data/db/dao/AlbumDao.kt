package com.casty.music.data.db.dao

import androidx.room.*
import com.casty.music.data.db.entities.AlbumEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Advanced Album DAO - Enterprise-Grade Data Access
 * 
 * Features:
 * - CRUD operations with transaction safety
 * - Reactive Flow observations for real-time UI updates
 * - Smart queries (favorites, recently played, trending, unplayed)
 * - Analytics queries (statistics, listening reports)
 * - Full-text search with relevance scoring
 * - Batch operations for bulk updates
 * - Optimized indices utilization
 */
@Dao
interface AlbumDao {
    
    // ==================== BASIC CRUD ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: AlbumEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<AlbumEntity>)
    
    @Update
    suspend fun update(album: AlbumEntity)
    
    @Delete
    suspend fun delete(album: AlbumEntity)
    
    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteById(albumId: String)
    
    @Query("SELECT * FROM albums WHERE id = :albumId")
    fun getById(albumId: String): AlbumEntity?
    
    @Query("SELECT * FROM albums WHERE id = :albumId")
    fun observeById(albumId: String): Flow<AlbumEntity?>
    
    @Query("SELECT * FROM albums")
    fun observeAll(): Flow<List<AlbumEntity>>
    
    // ==================== SMART QUERIES ====================
    
    @Query("""
        SELECT * FROM albums 
        WHERE isFavorite = 1 
        ORDER BY lastPlayedAt DESC NULLS LAST, title ASC
    """)
    fun observeFavoriteAlbums(): Flow<List<AlbumEntity>>
    
    @Query("""
        SELECT * FROM albums 
        WHERE lastPlayedAt IS NOT NULL 
        ORDER BY lastPlayedAt DESC 
        LIMIT :limit
    """)
    fun observeRecentlyPlayed(limit: Int = 20): Flow<List<AlbumEntity>>
    
    @Query("""
        SELECT * FROM albums 
        WHERE playCount > 5 
        ORDER BY playCount DESC, lastPlayedAt DESC 
        LIMIT :limit
    """)
    fun observeTrendingAlbums(limit: Int = 20): Flow<List<AlbumEntity>>
    
    @Query("""
        SELECT * FROM albums 
        WHERE cachedAt IS NOT NULL 
          AND (:currentTime < expiresAt OR expiresAt IS NULL)
        ORDER BY cachedAt DESC 
        LIMIT :limit
    """)
    fun observeCachedAlbums(currentTime: LocalDateTime, limit: Int = 50): Flow<List<AlbumEntity>>
    
    @Query("""
        SELECT * FROM albums 
        WHERE playCount = 0 
        ORDER BY cachedAt DESC, title ASC 
        LIMIT :limit
    """)
    fun observeUnplayedAlbums(limit: Int = 20): Flow<List<AlbumEntity>>
    
    @Query("""
        SELECT * FROM albums 
        WHERE isDownloaded = 1 
        ORDER BY title ASC
    """)
    fun observeDownloadedAlbums(): Flow<List<AlbumEntity>>
    
    @Query("""
        SELECT * FROM albums 
        WHERE isHidden = 0 
        ORDER BY title ASC
    """)
    fun observeVisibleAlbums(): Flow<List<AlbumEntity>>
    
    // ==================== ANALYTICS ====================
    
    @Query("""
        SELECT COUNT(*) FROM albums
    """)
    suspend fun getTotalAlbumCount(): Int
    
    @Query("""
        SELECT COUNT(*) FROM albums WHERE isFavorite = 1
    """)
    suspend fun getFavoriteCount(): Int
    
    @Query("""
        SELECT COUNT(*) FROM albums WHERE lastPlayedAt IS NOT NULL
    """)
    suspend fun getPlayedCount(): Int
    
    @Query("""
        SELECT SUM(playCount) FROM albums
    """)
    suspend fun getTotalPlayCount(): Long
    
    @Query("""
        SELECT SUM(totalListenTimeSeconds) FROM albums
    """)
    suspend fun getTotalListenTimeSeconds(): Long
    
    @Query("""
        SELECT * FROM albums 
        WHERE lastPlayedAt >= :startTime 
          AND lastPlayedAt <= :endTime 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun observeTopAlbumsInPeriod(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        limit: Int = 10
    ): Flow<List<AlbumEntity>>
    
    @Query("""
        SELECT DISTINCT genre FROM albums 
        WHERE genre IS NOT NULL 
        ORDER BY COUNT(*) DESC
    """)
    fun observeGenreDistribution(): Flow<List<String>>
    
    // ==================== SEARCH ====================
    
    @Query("""
        SELECT *, 
            CASE 
                WHEN title LIKE '%' || :query || '%' THEN 3
                WHEN authorsText LIKE '%' || :query || '%' THEN 2
                ELSE 1
            END AS relevance_score
        FROM albums 
        WHERE title LIKE '%' || :query || '%' 
           OR authorsText LIKE '%' || :query || '%'
        ORDER BY relevance_score DESC, playCount DESC, title ASC
        LIMIT :limit
    """)
    suspend fun searchWithRelevance(query: String, limit: Int = 20): List<AlbumEntity>
    
    @Query("""
        SELECT * FROM albums 
        WHERE title MATCH :query 
           OR authorsText MATCH :query
        ORDER BY rank
        LIMIT :limit
    """)
    suspend fun fullTextSearch(query: String, limit: Int = 20): List<AlbumEntity>
    
    // ==================== BATCH OPERATIONS ====================
    
    @Transaction
    suspend fun upsertAll(albums: List<AlbumEntity>) {
        albums.forEach { album ->
            val existing = getById(album.id)
            if (existing != null) {
                update(AlbumEntity.mergeMetadata(existing, album))
            } else {
                insert(album)
            }
        }
    }
    
    @Transaction
    suspend fun markAsPlayed(albumIds: List<String>, timestamp: LocalDateTime = LocalDateTime.now()) {
        albumIds.forEach { id ->
            setLastPlayed(id, timestamp)
            incrementPlayCount(id)
        }
    }
    
    @Transaction
    suspend fun toggleFavorite(albumId: String) {
        val album = getById(albumId) ?: return
        update(album.copy(isFavorite = !album.isFavorite, updatedAt = LocalDateTime.now()))
    }
    
    @Transaction
    suspend fun setHidden(albumIds: List<String>, hidden: Boolean) {
        albumIds.forEach { id ->
            val album = getById(id) ?: return@forEach
            update(album.copy(isHidden = hidden, updatedAt = LocalDateTime.now()))
        }
    }
    
    // ==================== HELPER QUERIES ====================
    
    @Query("UPDATE albums SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :albumId")
    suspend fun setFavorite(albumId: String, isFavorite: Boolean, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE albums SET lastPlayedAt = :timestamp, updatedAt = :updatedAt WHERE id = :albumId")
    suspend fun setLastPlayed(albumId: String, timestamp: LocalDateTime?, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE albums SET playCount = playCount + 1, updatedAt = :updatedAt WHERE id = :albumId")
    suspend fun incrementPlayCount(albumId: String, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE albums SET totalListenTimeSeconds = totalListenTimeSeconds + :seconds, updatedAt = :updatedAt WHERE id = :albumId")
    suspend fun addListenTime(albumId: String, seconds: Int, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE albums SET isDownloaded = :downloaded, downloadedAt = :timestamp, updatedAt = :updatedAt WHERE id = :albumId")
    suspend fun setDownloaded(albumId: String, downloaded: Boolean, timestamp: LocalDateTime = LocalDateTime.now(), updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE albums SET cachedAt = :cachedAt, expiresAt = :expiresAt, updatedAt = :updatedAt WHERE id = :albumId")
    suspend fun updateCacheInfo(albumId: String, cachedAt: LocalDateTime?, expiresAt: LocalDateTime?, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("DELETE FROM albums WHERE cachedAt < :threshold")
    suspend fun deleteExpiredCache(threshold: LocalDateTime): Int
    
    @Query("DELETE FROM albums")
    suspend fun deleteAll()
}
