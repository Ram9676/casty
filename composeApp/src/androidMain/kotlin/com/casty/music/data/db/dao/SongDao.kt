package com.casty.music.data.db.dao

import androidx.room.*
import com.casty.music.data.db.entities.SongEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Advanced Song DAO - Production-Ready Data Access Layer
 * 
 * Features:
 * - Optimized queries with proper indexing
 * - Smart search with relevance scoring
 * - Analytics and statistics queries
 * - Batch operations for performance
 * - Flow-based reactive observations
 */
@Dao
interface SongDao {

    // ==================== Basic CRUD ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>): List<Long>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id = :id")
    fun observeById(id: String): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    fun observeByIds(ids: List<String>): Flow<List<SongEntity>>

    // ==================== Library Queries ====================

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getTotalSongCount(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE likedAt IS NOT NULL")
    suspend fun getLikedSongCount(): Int

    @Query("SELECT * FROM songs WHERE likedAt IS NOT NULL ORDER BY likedAt DESC")
    fun observeLikedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE likedAt IS NOT NULL ORDER BY likedAt DESC LIMIT :limit")
    suspend fun getLikedSongs(limit: Int): List<SongEntity>

    @Query("""
        SELECT * FROM songs
        WHERE likedAt IS NOT NULL OR inLibrary = 1
        ORDER BY
            CASE
                WHEN likedAt IS NOT NULL THEN likedAt
                WHEN librarySyncedAt IS NOT NULL THEN librarySyncedAt
                ELSE updatedAt
            END DESC
    """)
    fun observeLibrarySongs(): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs
        WHERE inWatchLater = 1
        ORDER BY watchLaterAddedAt DESC
    """)
    fun observeWatchLaterSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isDownloaded = 1 ORDER BY downloadedAt DESC")
    fun observeDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT SUM(cachedSizeBytes) FROM songs WHERE isDownloaded = 1")
    suspend fun getTotalDownloadSize(): Long?

    @Query("""
        SELECT * FROM songs 
        WHERE lastPlayedAt IS NOT NULL 
        ORDER BY lastPlayedAt DESC 
        LIMIT :limit
    """)
    fun observeRecentSongs(limit: Int = 50): Flow<List<SongEntity>>

    // ==================== Smart Discovery Queries ====================

    @Query("""
        SELECT * FROM songs 
        WHERE playCount > 0 
        ORDER BY 
            (playCount * completionRate) DESC,
            lastPlayedAt DESC
        LIMIT :limit
    """)
    fun observeFavoriteSongs(limit: Int = 25): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs 
        WHERE lastPlayedAt IS NOT NULL 
        AND lastPlayedAt > datetime('now', '-30 days')
        AND playCount > 1
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    fun observeTrendingSongs(limit: Int = 20): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs 
        WHERE lastPlayedAt IS NULL 
        AND (likedAt IS NOT NULL OR inLibrary = 1)
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    fun observeUnplayedLibrarySongs(limit: Int = 10): Flow<List<SongEntity>>

    @Query("""
        SELECT DISTINCT genre, COUNT(*) as count
        FROM songs 
        WHERE genre IS NOT NULL 
        GROUP BY genre 
        ORDER BY count DESC
    """)
    suspend fun getGenreDistribution(): Map<String, Int>

    @Query("""
        SELECT * FROM songs 
        WHERE energyLevel >= :minEnergy 
        AND energyLevel <= :maxEnergy
        ORDER BY energyLevel DESC
        LIMIT :limit
    """)
    fun observeSongsByEnergy(minEnergy: Float, maxEnergy: Float, limit: Int): Flow<List<SongEntity>>

    // ==================== Analytics & Statistics ====================

    @Query("""
        SELECT 
            COUNT(*) as totalSongs,
            SUM(playCount) as totalPlays,
            SUM(totalPlayTimeMs) as totalListeningTimeMs,
            AVG(completionRate) as avgCompletionRate,
            COUNT(DISTINCT CASE WHEN likedAt IS NOT NULL THEN id END) as likedCount,
            COUNT(DISTINCT CASE WHEN isDownloaded = 1 THEN id END) as downloadedCount
        FROM songs
    """)
    suspend fun getLibraryStats(): LibraryStats

    @Query("""
        SELECT strftime('%Y-%m', lastPlayedAt) as month,
               COUNT(*) as playCount,
               SUM(totalPlayTimeMs) as listeningTimeMs
        FROM songs
        WHERE lastPlayedAt IS NOT NULL
        GROUP BY month
        ORDER BY month DESC
        LIMIT :months
    """)
    suspend fun getMonthlyListeningStats(months: Int = 12): List<MonthlyListeningStat>

    // ==================== Advanced Search ====================

    @Query("""
        SELECT *, 
            CASE 
                WHEN title LIKE :query || '%' THEN 3
                WHEN artistsText LIKE :query || '%' THEN 2
                WHEN title LIKE '%' || :query || '%' THEN 1
                WHEN artistsText LIKE '%' || :query || '%' THEN 0.5
                ELSE 0
            END as relevance
        FROM songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artistsText LIKE '%' || :query || '%'
           OR albumTitle LIKE '%' || :query || '%'
           OR genre LIKE '%' || :query || '%'
        ORDER BY relevance DESC, lastPlayedAt DESC, title COLLATE NOCASE ASC
        LIMIT :limit
    """)
    suspend fun searchWithRelevance(query: String, limit: Int = 100): List<SongEntity>

    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artistsText LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN lastPlayedAt IS NULL THEN 1 ELSE 0 END,
            lastPlayedAt DESC,
            title COLLATE NOCASE ASC
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 100): List<SongEntity>

    @Query("""
        SELECT * FROM songs 
        WHERE genre = :genre
        ORDER BY playCount DESC, lastPlayedAt DESC
        LIMIT :limit
    """)
    fun observeSongsByGenre(genre: String, limit: Int = 50): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs 
        WHERE language = :language
        ORDER BY playCount DESC, lastPlayedAt DESC
        LIMIT :limit
    """)
    fun observeSongsByLanguage(language: String, limit: Int = 50): Flow<List<SongEntity>>

    // ==================== Mutations ====================

    @Query("UPDATE songs SET likedAt = :likedAt, updatedAt = :now WHERE id = :id")
    suspend fun setLiked(id: String, likedAt: LocalDateTime?, now: LocalDateTime = LocalDateTime.now())

    @Query("UPDATE songs SET userRating = :rating, updatedAt = :now WHERE id = :id")
    suspend fun setUserRating(id: String, rating: Float, now: LocalDateTime = LocalDateTime.now())

    @Query("UPDATE songs SET moodTags = :tags, updatedAt = :now WHERE id = :id")
    suspend fun setMoodTags(id: String, tags: List<String>, now: LocalDateTime = LocalDateTime.now())

    @Query("""
        UPDATE songs 
        SET isDownloaded = :downloaded, 
            downloadedAt = :downloadedAt, 
            downloadQuality = :quality,
            cachedSizeBytes = :sizeBytes,
            updatedAt = :now 
        WHERE id = :id
    """)
    suspend fun setDownloaded(
        id: String,
        downloaded: Boolean,
        downloadedAt: LocalDateTime? = if (downloaded) LocalDateTime.now() else null,
        quality: String? = null,
        sizeBytes: Long? = null,
        now: LocalDateTime = LocalDateTime.now()
    )

    @Query("""
        UPDATE songs 
        SET lastPlayedAt = :now, 
            playCount = playCount + 1,
            totalPlayTimeMs = totalPlayTimeMs + :positionMs,
            updatedAt = :now 
        WHERE id = :id
    """)
    suspend fun recordPlay(id: String, positionMs: Long, now: LocalDateTime = LocalDateTime.now())

    @Query("""
        UPDATE songs 
        SET skipCount = skipCount + 1,
            lastSkipPositionMs = :positionMs,
            updatedAt = :now 
        WHERE id = :id
    """)
    suspend fun recordSkip(id: String, positionMs: Long, now: LocalDateTime = LocalDateTime.now())

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("UPDATE songs SET isHidden = :hidden, updatedAt = :now WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Boolean, now: LocalDateTime = LocalDateTime.now())

    // ==================== Batch Operations ====================

    @Transaction
    suspend fun batchUpdatePlayback(updates: List<PlaybackUpdate>) {
        updates.forEach { update ->
            recordPlay(update.songId, update.positionMs, update.timestamp)
        }
    }

    @Transaction
    suspend fun batchLikeSongs(songIds: List<String>, liked: Boolean) {
        val now = LocalDateTime.now()
        val likedAt = if (liked) now else null
        songIds.forEach { id ->
            setLiked(id, likedAt, now)
        }
    }

    @Transaction
    suspend fun syncWithCloud(remoteSongs: List<SongEntity>) {
        val existingIds = getByIds(remoteSongs.map { it.id }).map { it.id }.toSet()
        val toInsert = remoteSongs.filter { it.id !in existingIds }
        val toUpdate = remoteSongs.filter { it.id in existingIds && it.cloudVersion > 0 }
        
        if (toInsert.isNotEmpty()) upsertAll(toInsert)
        toUpdate.forEach { upsert(it) }
    }
}

// ==================== Data Classes for Stats ====================

data class LibraryStats(
    val totalSongs: Int = 0,
    val totalPlays: Long = 0,
    val totalListeningTimeMs: Long = 0,
    val avgCompletionRate: Float = 0f,
    val likedCount: Int = 0,
    val downloadedCount: Int = 0
) {
    val totalListeningTimeHours: Double get() = totalListeningTimeMs / 3600000.0
    val avgCompletionPercentage: Int get() = (avgCompletionRate * 100).toInt()
}

data class MonthlyListeningStat(
    val month: String,
    val playCount: Int,
    val listeningTimeMs: Long
) {
    val listeningTimeHours: Double get() = listeningTimeMs / 3600000.0
}

data class PlaybackUpdate(
    val songId: String,
    val positionMs: Long,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
