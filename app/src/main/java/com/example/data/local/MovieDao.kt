package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    // --- Watchlist ---
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE slug = :slug)")
    fun isWatchlisted(slug: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE slug = :slug")
    suspend fun deleteWatchlist(slug: String)

    // --- History / Continue Watching ---
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE slug = :slug LIMIT 1")
    suspend fun getHistory(slug: String): HistoryEntity?

    @Query("SELECT * FROM watch_history WHERE slug = :slug LIMIT 1")
    fun getHistoryFlow(slug: String): Flow<HistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistory(history: HistoryEntity)

    @Query("DELETE FROM watch_history WHERE slug = :slug")
    suspend fun deleteHistory(slug: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()
}
