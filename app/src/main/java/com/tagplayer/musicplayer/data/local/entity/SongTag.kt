package com.tagplayer.musicplayer.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "song_tags",
    primaryKeys = ["songId", "tagId"]
)
data class SongTag(
    val songId: Long,
    val tagId: Long,
    val addedAt: Long
)
