package com.casty.music.data.db.dao

import androidx.room.*
import com.casty.music.data.db.entities.PlaylistEntity
import com.casty.music.data.db.entities.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    // ==================== Playlists ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE ASC")
    fun observeAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: String): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)

    // ==================== Playlist <-> Song relationship ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: String)

    @Transaction
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_song_cross_ref ref ON s.id = ref.songId
        WHERE ref.playlistId = :playlistId
        ORDER BY ref.position ASC
    """)
    fun observeSongsInPlaylist(playlistId: String): Flow<List<com.casty.music.data.db.entities.SongEntity>>

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getSongCount(playlistId: String): Int

    /**
     * Best-ever: atomic reorder operation.
     * Call this from repository when user drags songs.
     */
    @Transaction
    suspend fun reorderPlaylist(playlistId: String, orderedSongIds: List<String>) {
        // Delete old refs
        clearPlaylistSongs(playlistId)

        // Re-insert with correct positions
        val newRefs = orderedSongIds.mapIndexed { index, songId ->
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = index
            )
        }
        insertCrossRefs(newRefs)

        // Update denormalized count
        val playlist = getPlaylist(playlistId)
        if (playlist != null) {
            upsertPlaylist(playlist.copy(songCount = orderedSongIds.size, updatedAt = java.time.LocalDateTime.now()))
        }
    }
}
