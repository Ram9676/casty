package com.casty.music.data.db.dao

import androidx.room.*
import com.casty.music.data.db.entities.SongEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface SongDao {

    // ==================== Basic CRUD ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id = :id")
    fun observeById(id: String): Flow<SongEntity?>

    // ==================== Library Queries (used by DatabaseRepository) ====================

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE likedAt IS NOT NULL ORDER BY likedAt DESC")
    fun observeLikedSongs(): Flow<List<SongEntity>>

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

    @Query("""
        SELECT * FROM songs 
        WHERE lastPlayedAt IS NOT NULL 
        ORDER BY lastPlayedAt DESC 
        LIMIT :limit
    """)
    fun observeRecentSongs(limit: Int = 50): Flow<List<SongEntity>>

    // ==================== Mutations ====================

    @Query("UPDATE songs SET likedAt = :likedAt, updatedAt = :now WHERE id = :id")
    suspend fun setLiked(id: String, likedAt: LocalDateTime?, now: LocalDateTime = LocalDateTime.now())

    @Query("UPDATE songs SET isDownloaded = :downloaded, downloadedAt = :downloadedAt, updatedAt = :now WHERE id = :id")
    suspend fun setDownloaded(
        id: String,
        downloaded: Boolean,
        downloadedAt: LocalDateTime? = if (downloaded) LocalDateTime.now() else null,
        now: LocalDateTime = LocalDateTime.now()
    )

    @Query("""
        UPDATE songs 
        SET lastPlayedAt = :now, 
            playCount = playCount + 1,
            updatedAt = :now 
        WHERE id = :id
    """)
    suspend fun recordPlay(id: String, now: LocalDateTime = LocalDateTime.now())

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun delete(id: String)

    // ==================== Search ====================

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
}
