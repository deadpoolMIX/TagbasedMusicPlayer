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

    // 标准 LRC 时间标签: [mm:ss.xx] 或 [mm:ss.xxx]
    private val LRC_TIME_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")

    suspend fun extractEmbeddedLyrics(context: Context, filePath: String): String? = withContext(Dispatchers.IO) {
        if (filePath.isEmpty()) return@withContext null
        val file = File(filePath)
        if (!file.exists()) return@withContext null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val lyrics = retriever.extractMetadata(METADATA_KEY_LYRICS)
            if (!lyrics.isNullOrBlank()) {
                // 检查是否是有效的歌词格式（包含时间标签或至少有多行文本）
                if (isValidLyricsContent(lyrics)) {
                    return@withContext lyrics
                }
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

    /**
     * 检查歌词内容是否有效
     */
    private fun isValidLyricsContent(content: String): Boolean {
        if (content.isBlank()) return false

        // 如果是 LRC 格式，检查是否有时间标签
        if (content.contains("[") && content.contains("]")) {
            // 检查是否有时间标签 [mm:ss.xx]
            if (LRC_TIME_REGEX.containsMatchIn(content)) {
                return true
            }
            // 检查是否有元数据标签 [ti:] [ar:] [al:] 等
            if (content.contains("[ti:") || content.contains("[ar:") || content.contains("[al:")) {
                return true
            }
        }

        // 检查是否有多行文本（至少2行非空内容）
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size >= 2) {
            return true
        }

        // 单行内容，检查是否是纯数字（不是有效歌词）
        val singleLine = content.trim()
        if (singleLine.length <= 6 && singleLine.all { it.isDigit() }) {
            return false
        }

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

        // 尝试 UTF-8
        try {
            val content = String(bytes, Charsets.UTF_8)
            if (!content.contains("\uFFFD")) {
                return content
            }
        } catch (e: Exception) { }

        // 尝试 GBK（常见中文编码）
        try {
            return String(bytes, Charset.forName("GBK"))
        } catch (e: Exception) { }

        // 尝试 UTF-16
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
        // 优先尝试外部 .lrc 文件
        val externalLyrics = loadLyricsFromExternalFile(audioFilePath)
        if (!externalLyrics.isNullOrBlank()) {
            return@withContext externalLyrics
        }
        // 其次尝试内嵌歌词
        extractEmbeddedLyrics(context, audioFilePath)
    }

    fun parseLrc(lrcContent: String): List<LyricLine> {
        if (lrcContent.isBlank()) return emptyList()

        val lines = mutableListOf<LyricLine>()

        lrcContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach

            // 跳过元数据行 [ti:], [ar:], [al:], [by:], [offset:] 等
            if (trimmedLine.startsWith("[") && !trimmedLine.startsWith("[0") && !trimmedLine.startsWith("[1") && !trimmedLine.startsWith("[2")) {
                val colonIndex = trimmedLine.indexOf(':')
                if (colonIndex > 0 && colonIndex < trimmedLine.indexOf(']')) {
                    return@forEach
                }
            }

            // 查找时间标签
            val timeMatches = LRC_TIME_REGEX.findAll(trimmedLine).toList()
            if (timeMatches.isEmpty()) return@forEach

            // 提取歌词文本（最后一个 ] 之后的内容）
            val lastBracketIndex = trimmedLine.lastIndexOf(']')
            val text = if (lastBracketIndex >= 0 && lastBracketIndex < trimmedLine.length - 1) {
                trimmedLine.substring(lastBracketIndex + 1).trim()
            } else {
                ""
            }

            // 为每个时间标签创建歌词行
            timeMatches.forEach { match ->
                val minutes = match.groupValues[1].toIntOrNull() ?: 0
                val seconds = match.groupValues[2].toIntOrNull() ?: 0
                val millisStr = match.groupValues[3]
                val millis = if (millisStr.length == 2) {
                    millisStr.toInt() * 10
                } else {
                    millisStr.toInt()
                }

                val totalMillis = (minutes * 60L + seconds) * 1000L + millis
                lines.add(LyricLine(totalMillis, text))
            }
        }

        return lines.sortedBy { it.timestampMs }
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
            // 纯文本歌词
            content.lines()
                .filter { it.isNotBlank() }
                .mapIndexed { index, line ->
                    LyricLine(index * 5000L, line.trim())
                }
        }
    }
}