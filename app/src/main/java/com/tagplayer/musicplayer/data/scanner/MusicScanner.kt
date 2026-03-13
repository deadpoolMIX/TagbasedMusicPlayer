package com.tagplayer.musicplayer.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import com.tagplayer.musicplayer.data.local.entity.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    // MediaStore projection
    private val projection = arrayOf(
        Media._ID,
        Media.TITLE,
        Media.ARTIST,
        Media.ALBUM,
        Media.ALBUM_ID,
        Media.DURATION,
        Media.DATA,
        Media.DISPLAY_NAME,
        Media.DATE_ADDED,
        Media.DATE_MODIFIED,
        Media.SIZE
    )

    private val selection = "${Media.IS_MUSIC} != 0 AND ${Media.DURATION} > 10000" // > 10 seconds

    private val sortOrder = "${Media.DATE_ADDED} DESC"

    fun scanAllSongs(): Flow<List<Song>> = flow {
        val songs = mutableListOf<Song>()

        val cursor = contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val columnIndexMap = createColumnIndexMap(it)

            while (it.moveToNext()) {
                try {
                    val song = cursorToSong(it, columnIndexMap)
                    songs.add(song)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Skip problematic files
                }
            }
        }

        emit(songs)
    }.flowOn(Dispatchers.IO)

    suspend fun scanSongsByPath(path: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val pathSelection = "$selection AND ${Media.DATA} LIKE ?"
        val pathArgs = arrayOf("$path%")

        val cursor = contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            pathSelection,
            pathArgs,
            sortOrder
        )

        cursor?.use {
            val columnIndexMap = createColumnIndexMap(it)

            while (it.moveToNext()) {
                try {
                    val song = cursorToSong(it, columnIndexMap)
                    songs.add(song)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        songs
    }

    suspend fun scanSongById(songId: Long): Song? = withContext(Dispatchers.IO) {
        val cursor = contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            "${Media._ID} = ?",
            arrayOf(songId.toString()),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndexMap = createColumnIndexMap(it)
                return@withContext cursorToSong(it, columnIndexMap)
            }
        }

        null
    }

    private fun createColumnIndexMap(cursor: Cursor): Map<String, Int> {
        return projection.associateWith { cursor.getColumnIndexOrThrow(it) }
    }

    private fun cursorToSong(cursor: Cursor, columnMap: Map<String, Int>): Song {
        val filePath = cursor.getString(columnMap[Media.DATA]!!) ?: ""
        val lyrics = loadLyricsFromFile(filePath)

        return Song(
            id = cursor.getLong(columnMap[Media._ID]!!),
            title = cursor.getString(columnMap[Media.TITLE]!!) ?: "Unknown",
            artist = cursor.getString(columnMap[Media.ARTIST]!!) ?: "Unknown Artist",
            album = cursor.getString(columnMap[Media.ALBUM]!!) ?: "Unknown Album",
            albumId = cursor.getLong(columnMap[Media.ALBUM_ID]!!),
            duration = cursor.getLong(columnMap[Media.DURATION]!!),
            filePath = filePath,
            fileName = cursor.getString(columnMap[Media.DISPLAY_NAME]!!) ?: "",
            dateAdded = cursor.getLong(columnMap[Media.DATE_ADDED]!!) * 1000,
            dateModified = cursor.getLong(columnMap[Media.DATE_MODIFIED]!!) * 1000,
            size = cursor.getLong(columnMap[Media.SIZE]!!),
            lyrics = lyrics
        )
    }

    private fun loadLyricsFromFile(filePath: String): String? {
        if (filePath.isEmpty()) return null

        val file = java.io.File(filePath)
        if (!file.exists()) return null

        // 尝试查找同名的 .lrc 文件
        val parentDir = file.parentFile ?: return null
        val fileNameWithoutExt = file.nameWithoutExtension

        val possibleNames = listOf(
            "$fileNameWithoutExt.lrc",
            "$fileNameWithoutExt.LRC",
            "$fileNameWithoutExt.txt",
            "$fileNameWithoutExt.TXT"
        )

        for (lrcFileName in possibleNames) {
            val lrcFile = java.io.File(parentDir, lrcFileName)
            if (lrcFile.exists() && lrcFile.canRead()) {
                try {
                    val content = lrcFile.readText(Charsets.UTF_8)
                    if (content.isNotBlank()) {
                        return content
                    }
                } catch (e: Exception) {
                    try {
                        val bytes = lrcFile.readBytes()
                        val content = String(bytes, Charset.forName("GBK"))
                        if (content.isNotBlank()) {
                            return content
                        }
                    } catch (e2: Exception) {
                        // 继续尝试下一个文件
                    }
                }
            }
        }

        return null
    }
}
