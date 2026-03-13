package com.tagplayer.musicplayer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tagplayer.musicplayer.data.local.entity.Playlist
import com.tagplayer.musicplayer.data.local.entity.PlaylistSong
import com.tagplayer.musicplayer.data.local.entity.ScanFolder
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.local.entity.SongTag
import com.tagplayer.musicplayer.data.local.entity.Tag

@Database(
    entities = [
        Song::class,
        Tag::class,
        SongTag::class,
        Playlist::class,
        PlaylistSong::class,
        ScanFolder::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun tagDao(): TagDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun scanFolderDao(): ScanFolderDao
}
