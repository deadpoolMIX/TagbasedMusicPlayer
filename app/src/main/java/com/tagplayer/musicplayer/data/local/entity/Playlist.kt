package com.tagplayer.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Int = 0,
    val songCount: Int = 0
)
