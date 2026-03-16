package com.tagplayer.musicplayer.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.tagplayer.musicplayer.data.local.database.SongDao
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.scanner.MusicScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 删除文件的结果状态
 */
sealed class DeleteFileResult {
    data class Success(val deleted: Boolean) : DeleteFileResult()
    data class NeedPermission(val intentSender: IntentSender, val song: Song) : DeleteFileResult()
    data class Error(val message: String) : DeleteFileResult()
}

@Singleton
class SongRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val musicScanner: MusicScanner,
    private val scanFolderRepository: ScanFolderRepository
) {
    private val contentResolver: ContentResolver = context.contentResolver
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    fun getRecentlyPlayedSongs(): Flow<List<Song>> = songDao.getRecentlyPlayedSongs()

    suspend fun getSongById(songId: Long): Song? = songDao.getSongById(songId)

    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    fun getSongCount(): Flow<Int> = songDao.getSongCount()

    suspend fun insertSong(song: Song) = songDao.insertSong(song)

    suspend fun insertSongs(songs: List<Song>) = songDao.insertSongs(songs)

    suspend fun deleteSong(song: Song) = songDao.deleteSong(song)

    suspend fun deleteSongById(songId: Long) = songDao.deleteSongById(songId)

    suspend fun deleteAllSongs() = songDao.deleteAllSongs()

    suspend fun incrementPlayCount(songId: Long) {
        songDao.incrementPlayCount(songId, System.currentTimeMillis())
    }

    /**
     * 扫描所有歌曲
     */
    suspend fun scanAndSaveSongs() {
        musicScanner.scanAllSongs().collect { scannedSongs ->
            songDao.insertSongs(scannedSongs)
        }
    }

    /**
     * 扫描指定文件夹内的歌曲
     */
    suspend fun scanFolder(path: String): List<Song> {
        val songs = musicScanner.scanSongsByPath(path)
        songDao.insertSongs(songs)
        return songs
    }

    suspend fun syncSongs() {
        // Get current songs from MediaStore
        musicScanner.scanAllSongs().collect { scannedSongs ->
            val scannedIds = scannedSongs.map { it.id }.toSet()

            // Delete songs that no longer exist in MediaStore
            // This requires getting all songs from DB and comparing
            // Implementation depends on specific sync strategy
            songDao.insertSongs(scannedSongs)
        }
    }

    /**
     * 删除歌曲的本地文件
     * 优先检查文件是否在已授权的文件夹内，如果是则直接删除
     *
     * @param song 要删除的歌曲
     * @return DeleteFileResult - 成功、需要权限或错误
     */
    suspend fun deleteSongFile(song: Song): DeleteFileResult = withContext(Dispatchers.IO) {
        try {
            val audioUri: Uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                song.id
            )

            // 先尝试直接通过 MediaStore 删除
            try {
                val deletedRows = contentResolver.delete(audioUri, null, null)
                if (deletedRows > 0) {
                    deleteLrcFile(song.filePath)
                    return@withContext DeleteFileResult.Success(true)
                }
            } catch (e: SecurityException) {
                // Android 11+ 需要权限，尝试通过已授权文件夹删除
                val folders = scanFolderRepository.getAllScanFoldersOnce()
                val matchingFolder = folders.find { folder ->
                    folder.realPath != null && song.filePath.startsWith(folder.realPath)
                }

                if (matchingFolder != null && matchingFolder.realPath != null) {
                    // 文件在已授权文件夹内，尝试通过 DocumentFile 删除
                    val deleted = deleteViaDocumentFile(song, matchingFolder.path, matchingFolder.realPath)
                    if (deleted) {
                        deleteLrcFile(song.filePath)
                        return@withContext DeleteFileResult.Success(true)
                    }
                }

                // 需要用户授权
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createWriteRequest(contentResolver, listOf(audioUri))
                    return@withContext DeleteFileResult.NeedPermission(pendingIntent.intentSender, song)
                } else {
                    return@withContext DeleteFileResult.Error("删除失败：权限不足")
                }
            }

            DeleteFileResult.Success(false)
        } catch (e: Exception) {
            e.printStackTrace()
            DeleteFileResult.Error(e.message ?: "删除失败")
        }
    }

    /**
     * 通过 DocumentFile 删除文件（使用已授权的文件夹权限）
     * @param song 要删除的歌曲
     * @param folderUriString 文件夹的 URI 字符串
     * @param realPath 文件夹的真实路径
     */
    private fun deleteViaDocumentFile(song: Song, folderUriString: String, realPath: String): Boolean {
        return try {
            val folderUri = Uri.parse(folderUriString)
            val folderDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return false

            // 计算相对路径
            val relativePath = song.filePath.removePrefix(realPath).removePrefix("/")

            // 遍历路径找到文件
            val pathParts = relativePath.split("/")
            var currentDoc = folderDoc

            for (i in pathParts.indices) {
                val part = pathParts[i]
                if (part.isBlank()) continue

                if (i == pathParts.lastIndex) {
                    // 最后一个部分是文件名
                    val fileDoc = currentDoc.findFile(part)
                    if (fileDoc != null && fileDoc.delete()) {
                        return true
                    }
                } else {
                    // 中间部分是文件夹
                    val subDir = currentDoc.findFile(part)
                    if (subDir != null && subDir.isDirectory) {
                        currentDoc = subDir
                    } else {
                        return false
                    }
                }
            }

            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 在用户授权后删除文件
     * 用于处理 Android 11+ 的权限请求回调
     */
    suspend fun deleteSongFileAfterPermission(song: Song): Boolean = withContext(Dispatchers.IO) {
        try {
            val audioUri: Uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                song.id
            )

            val deletedRows = contentResolver.delete(audioUri, null, null)
            if (deletedRows > 0) {
                deleteLrcFile(song.filePath)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 旧的删除方法，保留兼容性
     */
    suspend fun deleteSongFileLegacy(song: Song): Boolean = withContext(Dispatchers.IO) {
        try {
            val audioUri: Uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                song.id
            )

            val deletedRows = contentResolver.delete(audioUri, null, null)

            if (deletedRows > 0) {
                deleteLrcFile(song.filePath)

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    context.sendBroadcast(
                        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, audioUri)
                    )
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除同名 .lrc 歌词文件
     */
    private fun deleteLrcFile(songPath: String) {
        try {
            val lrcPath = songPath.substringBeforeLast(".") + ".lrc"
            val lrcFile = File(lrcPath)
            if (lrcFile.exists()) {
                lrcFile.delete()
            }

            // 也尝试删除大写的 .LRC
            val lrcPathUpper = songPath.substringBeforeLast(".") + ".LRC"
            val lrcFileUpper = File(lrcPathUpper)
            if (lrcFileUpper.exists()) {
                lrcFileUpper.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
