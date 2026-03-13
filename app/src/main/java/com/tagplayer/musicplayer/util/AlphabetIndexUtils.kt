package com.tagplayer.musicplayer.util

import android.icu.text.Transliterator

/**
 * 字母索引分组工具类
 * 使用 Android ICU Transliterator 进行中文转拼音
 */
object AlphabetIndexUtils {

    // 中文转拼音的 Transliterator 实例
    private val hanToLatin: Transliterator? by lazy {
        try {
            Transliterator.getInstance("Han-Latin")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取字符串的首字母（用于字母索引分组）
     * - 英文字母：返回大写形式 A-Z
     * - 中文汉字：转换为拼音，返回拼音首字母 A-Z
     * - 数字、日文（平假名/片假名）、其他字符：统一返回 '#'
     */
    fun getFirstLetter(text: String?): Char {
        if (text.isNullOrBlank()) return '#'

        val firstChar = text.first()

        return when {
            // 英文字母 A-Z 或 a-z
            firstChar.isLetter() && firstChar in 'A'..'Z' || firstChar in 'a'..'z' -> {
                firstChar.uppercaseChar()
            }
            // 中文汉字
            isChineseCharacter(firstChar) -> {
                getPinyinFirstLetter(firstChar)
            }
            // 数字、日文、韩文、符号等统一归为 #
            else -> '#'
        }
    }

    /**
     * 判断字符是否为中文汉字
     */
    private fun isChineseCharacter(char: Char): Boolean {
        // CJK 统一汉字范围
        return char.code in 0x4E00..0x9FFF ||
               char.code in 0x3400..0x4DBF ||
               char.code in 0x20000..0x2A6DF
    }

    /**
     * 获取中文汉字的拼音首字母
     * 使用 ICU Transliterator 将汉字转换为拼音
     */
    private fun getPinyinFirstLetter(char: Char): Char {
        return try {
            hanToLatin?.let { transliterator ->
                val pinyin = transliterator.transliterate(char.toString())
                // 提取拼音的第一个字母并转为大写
                pinyin.firstOrNull()?.uppercaseChar() ?: '#'
            } ?: '#'
        } catch (e: Exception) {
            '#'
        }
    }

    /**
     * 获取完整的字母索引列表（A-Z + #）
     * 按 A-Z 到 # 的顺序排列
     */
    fun getAlphabetIndex(): List<Char> {
        return ('A'..'Z').toList() + '#'
    }

    /**
     * 对数据源进行按首字母分组
     * @param items 原始数据列表
     * @param getName 提取名称的 lambda
     * @return 按首字母分组的 Map，key 为字母，value 为该字母下的数据列表
     */
    fun <T> groupByFirstLetter(
        items: List<T>,
        getName: (T) -> String
    ): Map<Char, List<T>> {
        return items.groupBy { item ->
            getFirstLetter(getName(item))
        }.toSortedMap(compareBy { it })
    }

    /**
     * 计算字母到列表索引的映射
     * 用于 LazyList 的 scrollToItem
     * @param groupedData 已分组的数据
     * @return Map<Char, Int> 字母到列表索引的映射
     */
    fun <T> calculateLetterToIndexMap(groupedData: Map<Char, List<T>>): Map<Char, Int> {
        val map = mutableMapOf<Char, Int>()
        var index = 0
        groupedData.forEach { (letter, list) ->
            map[letter] = index
            index += list.size + 1 // +1 为分组标题
        }
        return map
    }

    /**
     * 计算字母到网格索引的映射
     * 用于 LazyGrid 的 scrollToItem
     * @param groupedData 已分组的数据
     * @param columnCount 网格列数
     * @return Map<Char, Int> 字母到网格索引的映射
     */
    fun <T> calculateLetterToGridIndexMap(
        groupedData: Map<Char, List<T>>,
        columnCount: Int
    ): Map<Char, Int> {
        val map = mutableMapOf<Char, Int>()
        var index = 0
        groupedData.forEach { (letter, list) ->
            map[letter] = index
            // 网格布局需要按列数计算行数
            index += (list.size + columnCount - 1) / columnCount
        }
        return map
    }
}
