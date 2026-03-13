package com.tagplayer.musicplayer.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.tagplayer.musicplayer.data.local.entity.ScanFolder

@Dao
interface ScanFolderDao {
    @Query("SELECT * FROM scan_folders ORDER BY addedAt DESC")
    fun getAllScanFolders(): Flow<List<ScanFolder>>

    @Query("SELECT * FROM scan_folders WHERE path = :path LIMIT 1")
    suspend fun getScanFolderByPath(path: String): ScanFolder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanFolder(scanFolder: ScanFolder)

    @Delete
    suspend fun deleteScanFolder(scanFolder: ScanFolder)

    @Query("DELETE FROM scan_folders WHERE path = :path")
    suspend fun deleteScanFolderByPath(path: String)

    @Query("SELECT COUNT(*) FROM scan_folders")
    fun getScanFolderCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM scan_folders WHERE path = :path)")
    suspend fun scanFolderExists(path: String): Boolean
}
