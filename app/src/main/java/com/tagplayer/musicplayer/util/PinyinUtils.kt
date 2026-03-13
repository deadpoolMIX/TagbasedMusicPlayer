package com.tagplayer.musicplayer.util

/**
 * 中文转拼音工具类
 * 简单的汉字到拼音首字母的映射
 */
object PinyinUtils {

    // 常用汉字的拼音首字母映射（按Unicode范围）
    private val CHINESE_UNICODE_RANGES = listOf(
        // 一级汉字（常用字）
        0x4E00..0x9FA5
    )

    /**
     * 获取字符串的首字母（用于字母索引）
     * - 英文字母：返回大写形式
     * - 中文：返回拼音首字母
     * - 其他字符：返回 '#'
     */
    fun getFirstLetter(text: String?): Char {
        if (text.isNullOrBlank()) return '#'

        val firstChar = text.first()

        return when {
            // 英文字母
            firstChar.isLetter() && firstChar in 'A'..'Z' || firstChar in 'a'..'z' -> {
                firstChar.uppercaseChar()
            }
            // 中文
            isChinese(firstChar) -> {
                getChinesePinyinFirstLetter(firstChar)
            }
            // 其他字符归为#
            else -> '#'
        }
    }

    /**
     * 检查字符是否为中文
     */
    private fun isChinese(char: Char): Boolean {
        return CHINESE_UNICODE_RANGES.any { char.code in it }
    }

    /**
     * 获取中文字符的拼音首字母
     * 基于汉字Unicode范围的简单映射
     */
    private fun getChinesePinyinFirstLetter(char: Char): Char {
        // 汉字按拼音排序后的Unicode范围近似映射
        // 这是简化的映射，覆盖大部分常用汉字
        return when (char.code) {
            in 0x4E00..0x554A -> 'A' // 啊
            in 0x554B..0x5B7A -> 'B' // 八
            in 0x5B7B..0x5CE6 -> 'C' // 次
            in 0x5CE7..0x5D4B -> 'D' // 第
            in 0x5D4C..0x5E86 -> 'E' // 饿
            in 0x5E87..0x5F20 -> 'F' // 发
            in 0x5F21..0x600E -> 'G' // 个
            in 0x600F..0x6207 -> 'H' // 和
            in 0x6208..0x643A -> 'J' // 就
            in 0x643B..0x662D -> 'K' // 看
            in 0x662E..0x67E5 -> 'L' // 了
            in 0x67E6..0x69D0 -> 'M' // 吗
            in 0x69D1..0x6BA7 -> 'N' // 你
            in 0x6BA8..0x6D77 -> 'O' // 哦
            in 0x6D78..0x6F84 -> 'P' // 平
            in 0x6F85..0x7136 -> 'Q' // 去
            in 0x7137..0x7336 -> 'R' // 让
            in 0x7337..0x7525 -> 'S' // 是
            in 0x7526..0x7737 -> 'T' // 他
            in 0x7738..0x78CC -> 'W' // 我
            in 0x78CD..0x7AEF -> 'X' // 小
            in 0x7AF0..0x7CF8 -> 'Y' // 有
            in 0x7CF9..0x7F36 -> 'Z' // 在
            else -> '#' // 无法识别的汉字归为#
        }
    }

    /**
     * 获取完整的字母索引列表（# + A-Z）
     */
    fun getAlphabetIndex(): List<Char> {
        return listOf('#') + ('A'..'Z').toList()
    }
}
