package com.audiolifelog.app.data.repository

import com.audiolifelog.app.data.db.dao.DailySummary
import com.audiolifelog.app.data.db.entity.AudioEventEntity
import kotlinx.coroutines.flow.Flow

interface AudioEventRepository {

    suspend fun insertAll(events: List<AudioEventEntity>)

    fun getEventsInRange(startTime: Long, endTime: Long): Flow<List<AudioEventEntity>>

    fun getRecentEvents(limit: Int): Flow<List<AudioEventEntity>>

    fun getDailySummary(startTime: Long, endTime: Long): Flow<List<DailySummary>>

    suspend fun deleteOlderThan(timestamp: Long): Int

    suspend fun deleteAll(): Int

    fun getEventCount(): Flow<Int>
}
