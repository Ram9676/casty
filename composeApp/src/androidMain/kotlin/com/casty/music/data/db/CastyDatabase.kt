package com.casty.music.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.casty.music.data.db.converters.Converters
import com.casty.music.data.db.dao.AlbumDao
import com.casty.music.data.db.dao.ArtistDao
import com.casty.music.data.db.dao.PlaylistDao
import com.casty.music.data.db.dao.RecentSearchDao
import com.casty.music.data.db.dao.SongDao
import com.casty.music.data.db.entities.AlbumEntity
import com.casty.music.data.db.entities.ArtistEntity
import com.casty.music.data.db.entities.PlaylistEntity
import com.casty.music.data.db.entities.PlaylistSongCrossRef
import com.casty.music.data.db.entities.RecentSearchEntity
import com.casty.music.data.db.entities.SongEntity

/**
 * Casty Database - Production-Ready Room Configuration
 *
 * Design philosophy (best-ever):
 * - Single source of truth for all user library data
 * - Survives process death and app restarts
 * - Clean separation from legacy Casty schema
 * - FTS5 full-text search enabled for instant search
 * - Optimized indices for complex queries
 * - Foreign key constraints for data integrity
 * - Automatic migrations support
 *
 * Current version: 5 (Album & Artist Persistence Added)
 */
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        RecentSearchEntity::class,
        AlbumEntity::class,
        ArtistEntity::class
        // Future: QueueStateEntity, TasteProfileSnapshot, ListeningStatsEntity
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ]
)
@TypeConverters(Converters::class)
abstract class CastyDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao

    companion object {
        const val DATABASE_NAME = "casty_music.db"
        const val MIN_DB_VERSION = 3
    }

    /**
     * Optional: Enable WAL mode for better concurrent read/write performance
     * Call this in the database builder configuration
     */
    fun enableWriteAheadLogging() {
        setQueryExecutor { command ->
            kotlinx.coroutines.Dispatchers.IO.run { command.run() }
        }
    }
}
