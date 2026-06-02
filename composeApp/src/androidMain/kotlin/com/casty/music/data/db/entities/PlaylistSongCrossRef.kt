package com.casty.music.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Many-to-many join table with explicit position.
 * This allows users to reorder songs inside a playlist reliably.
 *
 * Best practice: composite primary key + position column.
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songId"],
    indices = [
        Index(value = ["playlistId", "position"]),
        Index(value = ["songId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val position: Int,                 // 0-based ordering
    val addedAt: Long = System.currentTimeMillis()
)
