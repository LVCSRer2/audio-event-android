package com.audiolifelog.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val RMS_THRESHOLD = floatPreferencesKey("rms_threshold")
        val MIN_CONFIDENCE = floatPreferencesKey("min_confidence")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
        val NNAPI_ENABLED = booleanPreferencesKey("nnapi_enabled")
    }

    val rmsThreshold: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.RMS_THRESHOLD] ?: 0.01f
    }

    val minConfidence: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.MIN_CONFIDENCE] ?: 0.3f
    }

    val retentionDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.RETENTION_DAYS] ?: 30
    }

    val nnapiEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NNAPI_ENABLED] ?: false
    }

    suspend fun setRmsThreshold(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RMS_THRESHOLD] = value
        }
    }

    suspend fun setMinConfidence(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MIN_CONFIDENCE] = value
        }
    }

    suspend fun setRetentionDays(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RETENTION_DAYS] = value
        }
    }

    suspend fun setNnapiEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NNAPI_ENABLED] = value
        }
    }
}
