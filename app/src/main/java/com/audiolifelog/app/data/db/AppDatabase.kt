package com.audiolifelog.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.audiolifelog.app.data.db.dao.AudioEventDao
import com.audiolifelog.app.data.db.entity.AudioEventEntity

@Database(
    entities = [AudioEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioEventDao(): AudioEventDao
}
