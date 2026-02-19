package com.audiolifelog.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_events",
    indices = [Index(value = ["timestamp"])]
)
data class AudioEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,          // epoch millis
    val eventLabel: String,
    val confidence: Float,
    val durationMs: Long
)
