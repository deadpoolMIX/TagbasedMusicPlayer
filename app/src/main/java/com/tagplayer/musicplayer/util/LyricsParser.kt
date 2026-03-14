package com.tagplayer.musicplayer.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

object LyricsParser {

    private const val METADATA_KEY_LYRICS = 20

    // 时间戳合并阈值：100ms内的歌词行视为同一时间，合并显示
    private const val MERGE_THRESHOLD_MS = 100L

    // 标准 LRC 时间标签: [mm:ss.xx] 或 [mm:ss.xxx]
    private val LRC_TIME_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")

    // offset 标签正则: [offset:xxx] 或 [offset:-xxx]
    private val LRC_OFFSET_REGEX = Regex("""\[offset:(-?\d+)\]""", RegexOption.IGNORE_CASE)

    suspend fun extractEmbeddedLyrics(context: Context, filePath: String): String? = withContext(Dispatchers.IO) {
        if (filePath.isEmpty()) return@withContext null
        val file = File(filePath)
        if (!file.exists()) return@withContext null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val lyrics = retriever.extractMetadata(METADATA_KEY_LYRICS)
            if (!lyrics.isNullOrBlank() && isValidLyricsContent(lyrics)) {
                return@withContext lyrics
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) { }
        }
    }

    private fun isValidLyricsContent(content: String): Boolean {
        if (content.isBlank()) return false

        if (content.contains("[") && content.contains("]")) {
            if (LRC_TIME_REGEX.containsMatchIn(content)) return true
            if (content.contains("[ti:") || content.contains("[ar:") || content.contains("[al:")) return true
        }

        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size >= 2) return true

        val singleLine = content.trim()
        if (singleLine.length <= 6 && singleLine.all { it.isDigit() }) return false

        return false
    }

    suspend fun extractEmbeddedLyricsFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val lyrics = retriever.extractMetadata(METADATA_KEY_LYRICS)
            if (!lyrics.isNullOrBlank() && isValidLyricsContent(lyrics)) {
                return@withContext lyrics
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) { }
        }
    }

    suspend fun loadLyricsFromExternalFile(audioFilePath: String): String? = withContext(Dispatchers.IO) {
        if (audioFilePath.isEmpty()) return@withContext null

        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) return@withContext null

        val parentDir = audioFile.parentFile ?: return@withContext null
        val fileNameWithoutExt = audioFile.nameWithoutExtension

        val possibleNames = listOf(
            "$fileNameWithoutExt.lrc",
            "$fileNameWithoutExt.LRC"
        )

        for (lrcFileName in possibleNames) {
            val lrcFile = File(parentDir, lrcFileName)
            if (lrcFile.exists() && lrcFile.canRead()) {
                try {
                    val content = readFileWithEncoding(lrcFile)
                    if (!content.isNullOrBlank() && isValidLyricsContent(content)) {
                        return@withContext content
                    }
                } catch (e: Exception) { }
            }
        }

        null
    }

    private fun readFileWithEncoding(file: File): String? {
        val bytes = file.readBytes()

        try {
            val content = String(bytes, Charsets.UTF_8)
            if (!content.contains("\uFFFD")) return content
        } catch (e: Exception) { }

        try {
            return String(bytes, Charset.forName("GBK"))
        } catch (e: Exception) { }

        try {
            return String(bytes, Charsets.UTF_16)
        } catch (e: Exception) { }

        return try {
            String(bytes)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLyrics(audioFilePath: String): String? = withContext(Dispatchers.IO) {
        loadLyricsFromExternalFile(audioFilePath)
    }

    suspend fun getLyrics(context: Context, audioFilePath: String): String? = withContext(Dispatchers.IO) {
        val externalLyrics = loadLyricsFromExternalFile(audioFilePath)
        if (!externalLyrics.isNullOrBlank()) return@withContext externalLyrics
        extractEmbeddedLyrics(context, audioFilePath)
    }

    fun parseLrc(lrcContent: String): List<LyricLine> {
        if (lrcContent.isBlank()) return emptyList()

        // 解析 offset 偏移值
        var offsetMs = 0L
        val offsetMatch = LRC_OFFSET_REGEX.find(lrcContent)
        if (offsetMatch != null) {
            offsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
        }

        val rawLines = mutableListOf<LyricLine>()

        lrcContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach

            // 跳过元数据行（包括 offset 行）
            if (trimmedLine.startsWith("[") && !trimmedLine.startsWith("[0") && !trimmedLine.startsWith("[1") && !trimmedLine.startsWith("[2")) {
                val colonIndex = trimmedLine.indexOf(':')
                if (colonIndex > 0 && colonIndex < trimmedLine.indexOf(']')) {
                    return@forEach
                }
            }

            val timeMatches = LRC_TIME_REGEX.findAll(trimmedLine).toList()
            if (timeMatches.isEmpty()) return@forEach

            val lastBracketIndex = trimmedLine.lastIndexOf(']')
            val text = if (lastBracketIndex >= 0 && lastBracketIndex < trimmedLine.length - 1) {
                trimmedLine.substring(lastBracketIndex + 1).trim()
            } else {
                ""
            }

            timeMatches.forEach { match ->
                val minutes = match.groupValues[1].toIntOrNull() ?: 0
                val seconds = match.groupValues[2].toIntOrNull() ?: 0
                val millisStr = match.groupValues[3]
                val millis = if (millisStr.length == 2) millisStr.toInt() * 10 else millisStr.toInt()

                val totalMillis = (minutes * 60L + seconds) * 1000L + millis + offsetMs
                rawLines.add(LyricLine(totalMillis, text))
            }
        }

        // 按时间戳排序
        rawLines.sortBy { it.timestampMs }

        // 合并时间戳相近的歌词行（处理双语歌词）
        return mergeBilingualLyrics(rawLines)
    }

    /**
     * 合并双语歌词
     * 将时间戳相近（100ms内）的歌词行合并为一行，用换行符分隔
     */
    private fun mergeBilingualLyrics(lines: List<LyricLine>): List<LyricLine> {
        if (lines.isEmpty()) return emptyList()

        val mergedLines = mutableListOf<LyricLine>()
        var currentGroup = mutableListOf<LyricLine>()

        for (line in lines) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(line)
            } else {
                val lastTimestamp = currentGroup.last().timestampMs
                // 如果时间戳差距在阈值内，合并到当前组
                if (line.timestampMs - lastTimestamp <= MERGE_THRESHOLD_MS) {
                    currentGroup.add(line)
                } else {
                    // 时间戳差距过大，保存当前组并开始新组
                    mergedLines.add(mergeGroup(currentGroup))
                    currentGroup = mutableListOf(line)
                }
            }
        }

        // 处理最后一组
        if (currentGroup.isNotEmpty()) {
            mergedLines.add(mergeGroup(currentGroup))
        }

        return mergedLines
    }

    /**
     * 将一组时间戳相近的歌词行合并为一行
     */
    private fun mergeGroup(group: List<LyricLine>): LyricLine {
        if (group.size == 1) return group[0]

        // 使用第一个时间戳
        val timestamp = group.first().timestampMs

        // 合并所有文本，用换行符分隔
        // 通常双语歌词是先中文后英文，或者先英文后中文
        val mergedText = group
            .map { it.text }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

        return LyricLine(timestamp, mergedText)
    }

    fun parseLrcFile(file: File): List<LyricLine> {
        if (!file.exists()) return emptyList()
        val content = readFileWithEncoding(file) ?: return emptyList()
        return parseLrc(content)
    }

    fun getCurrentLineIndex(lyrics: List<LyricLine>, currentTimeMs: Long): Int {
        if (lyrics.isEmpty()) return -1

        var left = 0
        var right = lyrics.size - 1
        var result = -1

        while (left <= right) {
            val mid = (left + right) / 2
            if (lyrics[mid].timestampMs <= currentTimeMs) {
                result = mid
                left = mid + 1
            } else {
                right = mid - 1
            }
        }

        return result
    }

    fun formatTime(millis: Long): String {
        val totalSeconds = (millis / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun isLrcFormat(content: String): Boolean {
        if (content.isBlank()) return false
        return LRC_TIME_REGEX.containsMatchIn(content)
    }

    fun parseLyrics(content: String): List<LyricLine> {
        if (content.isBlank()) return emptyList()

        return if (isLrcFormat(content)) {
            parseLrc(content)
        } else {
            content.lines()
                .filter { it.isNotBlank() }
                .mapIndexed { index, line ->
                    LyricLine(index * 5000L, line.trim())
                }
        }
    }
}