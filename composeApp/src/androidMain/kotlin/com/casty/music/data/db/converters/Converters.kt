package com.casty.music.data.db.converters

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Room TypeConverters for Casty database.
 * Best-ever: explicit, null-safe, uses UTC-based storage for timestamps.
 */
class Converters {

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? {
        return value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? {
        return value?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString(separator = "|||")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value.isNullOrBlank()) emptyList() else value.split("|||")
    }
}
