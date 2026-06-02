package com.casty.music.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted recent searches (replaces in-memory list in DatabaseRepository).
 */
@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
