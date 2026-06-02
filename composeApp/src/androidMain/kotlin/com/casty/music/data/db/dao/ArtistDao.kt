package com.casty.music.data.db.dao

import androidx.room.*
import com.casty.music.data.db.entities.ArtistEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Advanced Artist DAO - Enterprise-Grade Data Access
 * 
 * Features:
 * - CRUD operations with transaction safety
 * - Reactive Flow observations for real-time UI updates
 * - Smart queries (favorites, followed, trending, top artists)
 * - Analytics queries (statistics, listener reports)
 * - Full-text search with relevance scoring
 * - Batch operations for bulk updates
 * - Optimized indices utilization
 */
@Dao
interface ArtistDao {
    
    // ==================== BASIC CRUD ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(artist: ArtistEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(artists: List<ArtistEntity>)
    
    @Update
    suspend fun update(artist: ArtistEntity)
    
    @Delete
    suspend fun delete(artist: ArtistEntity)
    
    @Query("DELETE FROM artists WHERE id = :artistId")
    suspend fun deleteById(artistId: String)
    
    @Query("SELECT * FROM artists WHERE id = :artistId")
    fun getById(artistId: String): ArtistEntity?
    
    @Query("SELECT * FROM artists WHERE id = :artistId")
    fun observeById(artistId: String): Flow<ArtistEntity?>
    
    @Query("SELECT * FROM artists")
    fun observeAll(): Flow<List<ArtistEntity>>
    
    // ==================== SMART QUERIES ====================
    
    @Query("""
        SELECT * FROM artists 
        WHERE isFavorite = 1 
        ORDER BY monthlyListeners DESC NULLS LAST, name ASC
    """)
    fun observeFavoriteArtists(): Flow<List<ArtistEntity>>
    
    @Query("""
        SELECT * FROM artists 
        WHERE isFollowed = 1 
        ORDER BY name ASC
    """)
    fun observeFollowedArtists(): Flow<List<ArtistEntity>>
    
    @Query("""
        SELECT * FROM artists 
        WHERE monthlyListeners > :minListeners 
        ORDER BY monthlyListeners DESC 
        LIMIT :limit
    """)
    fun observeTrendingArtists(minListeners: Long = 1000000L, limit: Int = 20): Flow<List<ArtistEntity>>
    
    @Query("""
        SELECT * FROM artists 
        WHERE lastPlayedAt IS NOT NULL 
        ORDER BY lastPlayedAt DESC 
        LIMIT :limit
    """)
    fun observeRecentlyPlayed(limit: Int = 20): Flow<List<ArtistEntity>>
    
    @Query("""
        SELECT * FROM artists 
        WHERE playCount > 10 
        ORDER BY playCount DESC, monthlyListeners DESC 
        LIMIT :limit
    """)
    fun observeTopArtists(limit: Int = 20): Flow<List<ArtistEntity>>
    
    @Query("""
        SELECT * FROM artists 
        WHERE cachedAt IS NOT NULL 
          AND (:currentTime < expiresAt OR expiresAt IS NULL)
        ORDER BY cachedAt DESC 
        LIMIT :limit
    """)
    fun observeCachedArtists(currentTime: LocalDateTime, limit: Int = 50): Flow<List<ArtistEntity>>
    
    @Query("""
        SELECT * FROM artists 
        WHERE playCount = 0 
        ORDER BY cachedAt DESC, name ASC 
        LIMIT :limit
    """)
    fun observeDiscoveredArtists(limit: Int = 20): Flow<List<ArtistEntity>>
    
    @Query("""
        SELECT * FROM artists 
        WHERE isHidden = 0 
        ORDER BY name ASC
    """)
    fun observeVisibleArtists(): Flow<List<ArtistEntity>>
    
    // ==================== ANALYTICS ====================
    
    @Query("""
        SELECT COUNT(*) FROM artists
    """)
    suspend fun getTotalArtistCount(): Int
    
    @Query("""
        SELECT COUNT(*) FROM artists WHERE isFavorite = 1
    """)
    suspend fun getFavoriteCount(): Int
    
    @Query("""
        SELECT COUNT(*) FROM artists WHERE isFollowed = 1
    """)
    suspend fun getFollowedCount(): Int
    
    @Query("""
        SELECT COUNT(*) FROM artists WHERE lastPlayedAt IS NOT NULL
    """)
    suspend fun getPlayedCount(): Int
    
    @Query("""
        SELECT SUM(playCount) FROM artists
    """)
    suspend fun getTotalPlayCount(): Long
    
    @Query("""
        SELECT SUM(totalListenTimeSeconds) FROM artists
    """)
    suspend fun getTotalListenTimeSeconds(): Long
    
    @Query("""
        SELECT AVG(monthlyListeners) FROM artists WHERE monthlyListeners > 0
    """)
    suspend fun getAverageMonthlyListeners(): Double?
    
    @Query("""
        SELECT * FROM artists 
        WHERE lastPlayedAt >= :startTime 
          AND lastPlayedAt <= :endTime 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun observeTopArtistsInPeriod(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        limit: Int = 10
    ): Flow<List<ArtistEntity>>
    
    @Query("""
        SELECT DISTINCT genre FROM artists 
        WHERE genre IS NOT NULL 
        ORDER BY COUNT(*) DESC
    """)
    fun observeGenreDistribution(): Flow<List<String>>
    
    // ==================== SEARCH ====================
    
    @Query("""
        SELECT *, 
            CASE 
                WHEN name LIKE '%' || :query || '%' THEN 3
                WHEN description LIKE '%' || :query || '%' THEN 2
                ELSE 1
            END AS relevance_score
        FROM artists 
        WHERE name LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY relevance_score DESC, monthlyListeners DESC, name ASC
        LIMIT :limit
    """)
    suspend fun searchWithRelevance(query: String, limit: Int = 20): List<ArtistEntity>
    
    @Query("""
        SELECT * FROM artists 
        WHERE name MATCH :query 
           OR description MATCH :query
        ORDER BY rank
        LIMIT :limit
    """)
    suspend fun fullTextSearch(query: String, limit: Int = 20): List<ArtistEntity>
    
    // ==================== BATCH OPERATIONS ====================
    
    @Transaction
    suspend fun upsertAll(artists: List<ArtistEntity>) {
        artists.forEach { artist ->
            val existing = getById(artist.id)
            if (existing != null) {
                update(ArtistEntity.mergeMetadata(existing, artist))
            } else {
                insert(artist)
            }
        }
    }
    
    @Transaction
    suspend fun markAsPlayed(artistIds: List<String>, timestamp: LocalDateTime = LocalDateTime.now()) {
        artistIds.forEach { id ->
            setLastPlayed(id, timestamp)
            incrementPlayCount(id)
        }
    }
    
    @Transaction
    suspend fun toggleFollow(artistId: String) {
        val artist = getById(artistId) ?: return
        update(artist.copy(isFollowed = !artist.isFollowed, isSubscribed = !artist.isFollowed, updatedAt = LocalDateTime.now()))
    }
    
    @Transaction
    suspend fun toggleFavorite(artistId: String) {
        val artist = getById(artistId) ?: return
        update(artist.copy(isFavorite = !artist.isFavorite, updatedAt = LocalDateTime.now()))
    }
    
    @Transaction
    suspend fun setHidden(artistIds: List<String>, hidden: Boolean) {
        artistIds.forEach { id ->
            val artist = getById(id) ?: return@forEach
            update(artist.copy(isHidden = hidden, updatedAt = LocalDateTime.now()))
        }
    }
    
    // ==================== HELPER QUERIES ====================
    
    @Query("UPDATE artists SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun setFavorite(artistId: String, isFavorite: Boolean, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE artists SET isFollowed = :isFollowed, isSubscribed = :isSubscribed, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun setFollowed(artistId: String, isFollowed: Boolean, isSubscribed: Boolean = isFollowed, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE artists SET lastPlayedAt = :timestamp, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun setLastPlayed(artistId: String, timestamp: LocalDateTime?, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE artists SET playCount = playCount + 1, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun incrementPlayCount(artistId: String, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE artists SET totalListenTimeSeconds = totalListenTimeSeconds + :seconds, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun addListenTime(artistId: String, seconds: Int, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE artists SET monthlyListeners = :listeners, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun updateMonthlyListeners(artistId: String, listeners: Long, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE artists SET subscriberCount = :count, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun updateSubscriberCount(artistId: String, count: Long?, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE artists SET cachedAt = :cachedAt, expiresAt = :expiresAt, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun updateCacheInfo(artistId: String, cachedAt: LocalDateTime?, expiresAt: LocalDateTime?, updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("UPDATE artists SET isDownloaded = :downloaded, downloadedAt = :timestamp, updatedAt = :updatedAt WHERE id = :artistId")
    suspend fun setDownloaded(artistId: String, downloaded: Boolean, timestamp: LocalDateTime = LocalDateTime.now(), updatedAt: LocalDateTime = LocalDateTime.now())
    
    @Query("DELETE FROM artists WHERE cachedAt < :threshold")
    suspend fun deleteExpiredCache(threshold: LocalDateTime): Int
    
    @Query("DELETE FROM artists")
    suspend fun deleteAll()
}
