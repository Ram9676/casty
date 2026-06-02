package com.casty.music.data.db.dao

import androidx.room.*
import com.casty.music.data.db.entities.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {

    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}
