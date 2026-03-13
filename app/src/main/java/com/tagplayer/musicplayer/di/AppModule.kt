package com.tagplayer.musicplayer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tagplayer.musicplayer.data.local.database.MusicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                createFavoritesPlaylist(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // 确保"我喜欢"歌单存在（升级时）
                createFavoritesPlaylist(db)
            }

            private fun createFavoritesPlaylist(db: SupportSQLiteDatabase) {
                CoroutineScope(Dispatchers.IO).launch {
                    // 检查"我喜欢"歌单是否已存在
                    val cursor = db.query(
                        "SELECT id FROM playlists WHERE id = -1"
                    )
                    val exists = cursor.moveToFirst()
                    cursor.close()

                    if (!exists) {
                        db.execSQL(
                            "INSERT INTO playlists (id, name, createdAt, updatedAt, sortOrder, songCount, isSystem) " +
                            "VALUES (-1, '我喜欢', ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, -1, 0, 1)"
                        )
                    }
                }
            }
        })
        .build()
    }

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
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
