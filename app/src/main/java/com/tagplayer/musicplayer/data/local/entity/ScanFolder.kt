package com.tagplayer.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_folders")
data class ScanFolder(
    @PrimaryKey val path: String,
    val name: String? = null,
    val isIncluded: Boolean = true,
    val addedAt: Long
) {
    fun getDisplayName(): String {
        return name ?: path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: path
    }
}
