package com.tagplayer.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index(value = ["playlistId", "sortOrder"])]
)
data class PlaylistSong(
    val playlistId: Long,
    val songId: Long,
    val sortOrder: Int,
    val addedAt: Long
)
