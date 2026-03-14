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

    private val LRC_TIME_REGEX = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")
    private val ENHANCED_LRC_REGEX = Regex("((?:\\[\\d{1,2}:\\d{2}(?:\\.\\d{2,3})?\\])+)(.*)")
    private val METADATA_REGEX = Regex("^\\[[a-zA-Z]+:.+\\]$")

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

    suspend fun extractEmbeddedLyricsFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val lyrics = retriever.extractMetadata(METADATA_KEY_LYRICS)
            if (!lyrics.isNullOrBlank()) {
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
            "$fileNameWithoutExt.LRC",
            "${audioFile.name}.lrc",
            "${audioFile.name}.LRC"
        )

        for (lrcFileName in possibleNames) {
            val lrcFile = File(parentDir, lrcFileName)
            if (lrcFile.exists() && lrcFile.canRead()) {
                try {
                    val content = readFileWithEncoding(lrcFile)
                    if (!content.isNullOrBlank()) {
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
            if (!content.contains("\uFFFD")) {
                return content
            }
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
        val embeddedLyrics = extractEmbeddedLyrics(context, audioFilePath)
        if (!embeddedLyrics.isNullOrBlank()) {
            return@withContext embeddedLyrics
        }
        loadLyricsFromExternalFile(audioFilePath)
    }

    fun parseLrc(lrcContent: String): List<LyricLine> {
        if (lrcContent.isBlank()) return emptyList()

        val lines = mutableListOf<LyricLine>()

        lrcContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach

            if (METADATA_REGEX.matches(trimmedLine)) {
                return@forEach
            }

            val matchResult = ENHANCED_LRC_REGEX.find(trimmedLine)
            if (matchResult != null) {
                val timeTags = matchResult.groupValues[1]
                val text = matchResult.groupValues[2].trim()

                LRC_TIME_REGEX.findAll(timeTags).forEach { timeMatch ->
                    val minutes = timeMatch.groupValues[1].toIntOrNull() ?: 0
                    val seconds = timeMatch.groupValues[2].toIntOrNull() ?: 0
                    val millisStr = timeMatch.groupValues[3]
                    val millis = if (millisStr.length == 2) {
                        millisStr.toInt() * 10
                    } else {
                        millisStr.toInt()
                    }

                    val totalMillis = (minutes * 60L + seconds) * 1000L + millis
                    lines.add(LyricLine(totalMillis, text))
                }
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
            content.lines()
                .filter { it.isNotBlank() }
                .mapIndexed { index, line ->
                    LyricLine(index * 5000L, line.trim())
                }
        }
    }
}