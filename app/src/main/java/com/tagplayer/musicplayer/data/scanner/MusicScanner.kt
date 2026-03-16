package com.tagplayer.musicplayer.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore.Audio.Media
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.util.LyricsParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    // 支持的音频格式
    private val supportedExtensions = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "wma")

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
        Media.SIZE,
        Media.MIME_TYPE
    )

    // 放宽筛选条件：只过滤时长大于 1 秒的音频（避免铃声等短音频）
    // 不依赖 IS_MUSIC 标志，因为部分文件可能未正确标记
    private val selection = "${Media.DURATION} > 1000"

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
                    val filePath = it.getString(columnIndexMap[Media.DATA]!!) ?: ""

                    // 检查文件扩展名是否支持
                    val extension = filePath.substringAfterLast('.', "").lowercase()
                    if (extension !in supportedExtensions) {
                        continue
                    }

                    val song = cursorToSong(it, columnIndexMap, filePath)
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
                    val filePath = it.getString(columnIndexMap[Media.DATA]!!) ?: ""

                    // 检查文件扩展名是否支持
                    val extension = filePath.substringAfterLast('.', "").lowercase()
                    if (extension !in supportedExtensions) {
                        continue
                    }

                    val song = cursorToSong(it, columnIndexMap, filePath)
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
                val filePath = it.getString(columnIndexMap[Media.DATA]!!) ?: ""
                return@withContext cursorToSong(it, columnIndexMap, filePath)
            }
        }

        null
    }

    private fun createColumnIndexMap(cursor: Cursor): Map<String, Int> {
        return projection.associateWith { cursor.getColumnIndexOrThrow(it) }
    }

    private suspend fun cursorToSong(cursor: Cursor, columnMap: Map<String, Int>, filePath: String): Song {
        // 优先提取内嵌歌词，其次从外部 .lrc 文件读取
        // LyricsParser 内部有 try-catch，不会抛出异常
        val lyrics = try {
            LyricsParser.getLyrics(context, filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

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
}