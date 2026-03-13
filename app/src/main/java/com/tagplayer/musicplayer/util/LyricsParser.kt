package com.tagplayer.musicplayer.util

import java.io.File
import java.util.regex.Pattern

data class LyricLine(
    val time: Long, // 毫秒
    val text: String
)

object LyricsParser {

    private val TIME_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

    fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val linePattern = Pattern.compile("((?:\\[\\d{2}:\\d{2}\\.\\d{2,3}\\])+)(.*)")

        lrcContent.lines().forEach { line ->
            val matcher = linePattern.matcher(line.trim())
            if (matcher.find()) {
                val timeTags = matcher.group(1) ?: return@forEach
                val text = matcher.group(2)?.trim() ?: ""

                // 解析时间标签
                val timeMatcher = TIME_PATTERN.matcher(timeTags)
                while (timeMatcher.find()) {
                    val minutes = timeMatcher.group(1)?.toIntOrNull() ?: 0
                    val seconds = timeMatcher.group(2)?.toIntOrNull() ?: 0
                    val millisStr = timeMatcher.group(3) ?: "00"
                    val millis = if (millisStr.length == 2) {
                        millisStr.toInt() * 10 // .00 -> 毫秒
                    } else {
                        millisStr.toInt()
                    }

                    val totalMillis = (minutes * 60 + seconds) * 1000L + millis
                    lines.add(LyricLine(totalMillis, text))
                }
            }
        }

        return lines.sortedBy { it.time }
    }

    fun parseLrcFromFile(file: File): List<LyricLine> {
        return if (file.exists()) {
            parseLrc(file.readText())
        } else {
            emptyList()
        }
    }

    fun getCurrentLineIndex(lyrics: List<LyricLine>, currentTime: Long): Int {
        if (lyrics.isEmpty()) return -1

        for (i in lyrics.indices.reversed()) {
            if (lyrics[i].time <= currentTime) {
                return i
            }
        }
        return -1
    }

    fun formatTime(millis: Long): String {
        val seconds = (millis / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }
}
