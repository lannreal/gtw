package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val slug: String,
    val title: String,
    val poster: String,
    val year: String = "-",
    val rating: String = "-",
    val quality: String = "HD",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey val slug: String,
    val title: String,
    val poster: String,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val server: String = "cast",
    val episodeTitle: String? = null,
    val season: String? = null,
    val lastWatchedAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
