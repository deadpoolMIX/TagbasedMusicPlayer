package com.tagplayer.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val filePath: String,
    val fileName: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val lyrics: String? = null,
    val playCount: Int = 0,
    val lastPlayed: Long? = null
)
