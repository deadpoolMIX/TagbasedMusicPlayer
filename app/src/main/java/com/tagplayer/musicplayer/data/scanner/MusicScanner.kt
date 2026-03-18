package com.tagplayer.musicplayer.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore.Audio.Media
import android.util.Log
import com.tagplayer.musicplayer.data.local.entity.Song
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

    companion object {
        private const val TAG = "MusicScanner"
    }

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

        // 统计变量
        var totalCount = 0
        var skippedExtension = 0
        var skippedDuration = 0
        var errorCount = 0
        val extensionStats = mutableMapOf<String, Int>()

        val cursor = contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            null,  // 不加任何筛选，查询所有音频
            null,
            sortOrder
        )

        cursor?.use {
            val columnIndexMap = createColumnIndexMap(it)

            Log.d(TAG, "=== 开始扫描 MediaStore ===")
            Log.d(TAG, "MediaStore 返回总行数: ${it.count}")

            while (it.moveToNext()) {
                totalCount++
                try {
                    val filePath = it.getString(columnIndexMap[Media.DATA]!!) ?: ""
                    val duration = it.getLong(columnIndexMap[Media.DURATION]!!)

                    // 统计扩展名分布
                    val extension = filePath.substringAfterLast('.', "").lowercase()
                    extensionStats[extension] = (extensionStats[extension] ?: 0) + 1

                    // 检查时长
                    if (duration <= 1000) {
                        skippedDuration++
                        continue
                    }

                    // 检查文件扩展名是否支持
                    if (extension !in supportedExtensions) {
                        skippedExtension++
                        continue
                    }

                    val song = cursorToSong(it, columnIndexMap, filePath)
                    songs.add(song)
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "解析歌曲失败 (#$errorCount): ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        // 打印详细统计
        Log.d(TAG, "=== 扫描统计 ===")
        Log.d(TAG, "MediaStore 总行数: $totalCount")
        Log.d(TAG, "时长<=1秒跳过: $skippedDuration")
        Log.d(TAG, "不支持的格式跳过: $skippedExtension")
        Log.d(TAG, "解析错误跳过: $errorCount")
        Log.d(TAG, "最终有效歌曲数: ${songs.size}")
        Log.d(TAG, "=== 扩展名分布 ===")
        extensionStats.entries.sortedByDescending { it.value }.forEach { (ext, count) ->
            val supported = if (ext in supportedExtensions) "[支持]" else "[不支持]"
            Log.d(TAG, "$ext: $count $supported")
        }

        emit(songs)
    }.flowOn(Dispatchers.IO)

    suspend fun scanSongsByPath(path: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        Log.d(TAG, "=== 按路径扫描: $path ===")

        // 先查询不带路径限制的总数
        val totalCursor = contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            arrayOf(Media._ID),
            null,
            null,
            null
        )
        val totalInMediaStore = totalCursor?.use { it.count } ?: 0
        Log.d(TAG, "MediaStore 中音频总行数: $totalInMediaStore")

        // 再查询带路径限制的
        val pathSelection = "${Media.DURATION} > 1000 AND ${Media.DATA} LIKE ?"
        val pathArgs = arrayOf("$path%")

        val cursor = contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            pathSelection,
            pathArgs,
            sortOrder
        )

        var skippedExtension = 0
        var errorCount = 0

        cursor?.use {
            Log.d(TAG, "路径匹配的行数: ${it.count}")

            val columnIndexMap = createColumnIndexMap(it)

            while (it.moveToNext()) {
                try {
                    val filePath = it.getString(columnIndexMap[Media.DATA]!!) ?: ""

                    // 检查文件扩展名是否支持
                    val extension = filePath.substringAfterLast('.', "").lowercase()
                    if (extension !in supportedExtensions) {
                        skippedExtension++
                        continue
                    }

                    val song = cursorToSong(it, columnIndexMap, filePath)
                    songs.add(song)
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "解析歌曲失败 (#$errorCount): ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        Log.d(TAG, "扫描结果: ${songs.size} 首歌曲, 跳过格式: $skippedExtension, 错误: $errorCount")

        // 如果路径扫描结果为空，打印 MediaStore 中前10条记录的路径格式
        if (songs.isEmpty() && totalInMediaStore > 0) {
            Log.w(TAG, "路径扫描结果为空，打印 MediaStore 中前10条路径格式作为参考:")
            val debugCursor = contentResolver.query(
                Media.EXTERNAL_CONTENT_URI,
                arrayOf(Media._ID, Media.DATA, Media.TITLE),
                null,
                null,
                "${Media.DATE_ADDED} DESC LIMIT 10"
            )
            debugCursor?.use {
                val dataColumn = it.getColumnIndex(Media.DATA)
                val titleColumn = it.getColumnIndex(Media.TITLE)
                while (it.moveToNext()) {
                    Log.w(TAG, "  路径: ${it.getString(dataColumn)} | 标题: ${it.getString(titleColumn)}")
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

    private fun cursorToSong(cursor: Cursor, columnMap: Map<String, Int>, filePath: String): Song {
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
            lyrics = null  // 歌词延迟加载，不在扫描时解析
        )
    }
}