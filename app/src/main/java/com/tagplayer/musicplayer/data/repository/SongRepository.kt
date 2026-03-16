package com.tagplayer.musicplayer.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

@Singleton
class SongRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val musicScanner: MusicScanner
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
     * 使用 MediaStore API 删除音频文件，同时删除同名 .lrc 歌词文件
     *
     * @param song 要删除的歌曲
     * @return 是否删除成功
     */
    suspend fun deleteSongFile(song: Song): Boolean = withContext(Dispatchers.IO) {
        try {
            // 使用 MediaStore 删除音频文件
            val audioUri: Uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                song.id
            )

            val deletedRows = contentResolver.delete(audioUri, null, null)

            if (deletedRows > 0) {
                // 删除同名 .lrc 歌词文件
                deleteLrcFile(song.filePath)

                // 通知 MediaStore 扫描更新
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    context.sendBroadcast(
                        android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, audioUri)
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
