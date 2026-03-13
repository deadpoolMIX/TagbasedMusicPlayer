package com.tagplayer.musicplayer.data.repository

import com.tagplayer.musicplayer.data.local.database.ScanFolderDao
import com.tagplayer.musicplayer.data.local.entity.ScanFolder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanFolderRepository @Inject constructor(
    private val scanFolderDao: ScanFolderDao
) {
    fun getAllScanFolders(): Flow<List<ScanFolder>> = scanFolderDao.getAllScanFolders()

    suspend fun getScanFolderByPath(path: String): ScanFolder? =
        scanFolderDao.getScanFolderByPath(path)

    suspend fun addScanFolder(path: String, name: String? = null, isIncluded: Boolean = true) {
        val displayName = name ?: path.substringAfterLast('/')
        val scanFolder = ScanFolder(
            path = path,
            name = displayName,
            isIncluded = isIncluded,
            addedAt = System.currentTimeMillis()
        )
        scanFolderDao.insertScanFolder(scanFolder)
    }

    suspend fun removeScanFolder(scanFolder: ScanFolder) =
        scanFolderDao.deleteScanFolder(scanFolder)

    suspend fun removeScanFolderByPath(path: String) =
        scanFolderDao.deleteScanFolderByPath(path)

    fun getScanFolderCount(): Flow<Int> = scanFolderDao.getScanFolderCount()

    suspend fun scanFolderExists(path: String): Boolean =
        scanFolderDao.scanFolderExists(path)
}
