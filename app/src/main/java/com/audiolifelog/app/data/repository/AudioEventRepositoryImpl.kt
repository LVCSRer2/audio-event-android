package com.audiolifelog.app.data.repository

import com.audiolifelog.app.data.db.dao.AudioEventDao
import com.audiolifelog.app.data.db.dao.DailySummary
import com.audiolifelog.app.data.db.entity.AudioEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEventRepositoryImpl @Inject constructor(
    private val audioEventDao: AudioEventDao
) : AudioEventRepository {

    override suspend fun insertAll(events: List<AudioEventEntity>) {
        withContext(Dispatchers.IO) {
            audioEventDao.insertAll(events)
        }
    }

    override fun getEventsInRange(startTime: Long, endTime: Long): Flow<List<AudioEventEntity>> {
        return audioEventDao.getEventsInRange(startTime, endTime)
    }

    override fun getRecentEvents(limit: Int): Flow<List<AudioEventEntity>> {
        return audioEventDao.getRecentEvents(limit)
    }

    override fun getDailySummary(startTime: Long, endTime: Long): Flow<List<DailySummary>> {
        return audioEventDao.getDailySummary(startTime, endTime)
    }

    override suspend fun deleteOlderThan(timestamp: Long): Int {
        return withContext(Dispatchers.IO) {
            audioEventDao.deleteOlderThan(timestamp)
        }
    }

    override suspend fun deleteAll(): Int {
        return withContext(Dispatchers.IO) {
            audioEventDao.deleteAll()
        }
    }

    override fun getEventCount(): Flow<Int> {
        return audioEventDao.getEventCount()
    }
}
