package com.tagplayer.musicplayer.di

import android.content.Context
import androidx.room.Room
import com.tagplayer.musicplayer.data.local.database.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideSongDao(database: MusicDatabase) = database.songDao()

    @Provides
    fun provideTagDao(database: MusicDatabase) = database.tagDao()

    @Provides
    fun providePlaylistDao(database: MusicDatabase) = database.playlistDao()

    @Provides
    fun provideScanFolderDao(database: MusicDatabase) = database.scanFolderDao()
}
