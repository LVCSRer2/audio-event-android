package com.audiolifelog.app.di

import android.content.Context
import androidx.room.Room
import com.audiolifelog.app.data.db.AppDatabase
import com.audiolifelog.app.data.db.dao.AudioEventDao
import com.audiolifelog.app.data.repository.AudioEventRepository
import com.audiolifelog.app.data.repository.AudioEventRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ai.onnxruntime.OrtEnvironment
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAudioEventRepository(
        impl: AudioEventRepositoryImpl
    ): AudioEventRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "audio_lifelog.db"
            ).build()
        }

        @Provides
        @Singleton
        fun provideAudioEventDao(database: AppDatabase): AudioEventDao {
            return database.audioEventDao()
        }

        @Provides
        @Singleton
        fun provideOrtEnvironment(): OrtEnvironment {
            return OrtEnvironment.getEnvironment()
        }
    }
}
