package com.casty.music.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.casty.music.data.db.converters.Converters
import com.casty.music.data.db.dao.PlaylistDao
import com.casty.music.data.db.dao.RecentSearchDao
import com.casty.music.data.db.dao.SongDao
import com.casty.music.data.db.entities.PlaylistEntity
import com.casty.music.data.db.entities.PlaylistSongCrossRef
import com.casty.music.data.db.entities.RecentSearchEntity
import com.casty.music.data.db.entities.SongEntity

/**
 * Casty primary database.
 *
 * Design philosophy (best-ever):
 * - Single source of truth for all user library data.
 * - Survives process death and app restarts.
 * - Clean separation from legacy Casty schema (we start at version 1).
 * - Future migrations will live in the same package.
 *
 * Current version: 3 (Casty native)
 */
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        RecentSearchEntity::class
        // Future: QueueStateEntity, TasteProfileSnapshot, etc.
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CastyDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        const val DATABASE_NAME = "casty_music.db"
    }
}
