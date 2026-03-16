package com.tagplayer.musicplayer.util

/**
 * 歌手名称解析工具
 * 用于解析包含多个歌手的字符串，如 "周杰伦、方文山" -> ["周杰伦", "方文山"]
 */
object ArtistNameParser {

    // 分隔符正则：支持多种分隔符
    // 、（中文顿号）,（逗号）/ & ; feat. ft.
    private val separatorRegex = Regex("""\s*(?:、|,|/|&|;|feat\.|ft\.)\s*""", RegexOption.IGNORE_CASE)

    /**
     * 解析歌手字符串，返回单独的歌手名列表
     *
     * 示例:
     * - "周杰伦" -> ["周杰伦"]
     * - "周杰伦、方文山" -> ["周杰伦", "方文山"]
     * - "Jay feat. Beyonce" -> ["Jay", "Beyonce"]
     * - "A & B ft. C" -> ["A", "B", "C"]
     * - "" 或 null -> ["Unknown Artist"]
     */
    fun parse(artistString: String?): List<String> {
        if (artistString.isNullOrBlank()) {
            return listOf("Unknown Artist")
        }

        return artistString
            .split(separatorRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?: listOf("Unknown Artist")
    }

    /**
     * 检查歌手字符串是否包含指定的歌手名（精确匹配）
     */
    fun containsArtist(artistString: String?, targetArtist: String): Boolean {
        if (artistString.isNullOrBlank() || targetArtist.isBlank()) {
            return false
        }

        val artists = parse(artistString)
        return artists.any { it.equals(targetArtist, ignoreCase = false) }
    }
}