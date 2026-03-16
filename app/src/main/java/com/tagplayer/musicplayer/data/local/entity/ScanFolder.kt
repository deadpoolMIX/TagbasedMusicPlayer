package com.tagplayer.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "scan_folders")
data class ScanFolder(
    @PrimaryKey val path: String,
    val name: String? = null,
    val realPath: String? = null,  // 真实文件路径，用于检查文件是否在文件夹内
    val isIncluded: Boolean = true,
    val addedAt: Long
) {
    fun getDisplayName(): String {
        return name ?: path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: path
    }
}
