package com.audiolifelog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.audiolifelog.app.data.db.entity.AudioEventEntity
import kotlinx.coroutines.flow.Flow

data class DailySummary(
    val eventLabel: String,
    val count: Int,
    val totalDurationMs: Long
)

@Dao
interface AudioEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AudioEventEntity>)

    @Query("SELECT * FROM audio_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getEventsInRange(startTime: Long, endTime: Long): Flow<List<AudioEventEntity>>

    @Query("SELECT * FROM audio_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<AudioEventEntity>>

    @Query(
        """
        SELECT eventLabel,
               COUNT(*) AS count,
               SUM(durationMs) AS totalDurationMs
        FROM audio_events
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY eventLabel
        ORDER BY count DESC
        """
    )
    fun getDailySummary(startTime: Long, endTime: Long): Flow<List<DailySummary>>

    @Query("DELETE FROM audio_events WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int

    @Query("DELETE FROM audio_events")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM audio_events")
    fun getEventCount(): Flow<Int>
}
