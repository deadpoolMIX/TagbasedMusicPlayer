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

    private suspend fun cursorToSong(cursor: Cursor, columnMap: Map<String, Int>): Song {
        val filePath = cursor.getString(columnMap[Media.DATA]!!) ?: ""

        // 优先提取内嵌歌词，其次从外部 .lrc 文件读取
        val lyrics = LyricsParser.getLyrics(context, filePath)

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