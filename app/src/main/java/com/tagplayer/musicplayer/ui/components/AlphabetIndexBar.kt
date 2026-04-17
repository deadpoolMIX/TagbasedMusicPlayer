package com.tagplayer.musicplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 字母索引栏通用组件
 * 
 * 优化点：
 * 1. 使用 PointerEventPass.Initial 提前拦截事件，解决初始加载失效问题
 * 2. 移除子项 clickable，改为在 pointerInput 中直接处理按下事件，实现“按住即滑”，大幅提升灵敏度
 * 3. 增加事件消耗 (consume)，避免与 LazyColumn 等父容器滚动冲突
 */
@Composable
fun AlphabetIndexBar(
    letters: List<Char>,
    enabledLetters: Set<Char>,
    currentSelectedLetter: Char?,
    onLetterSelected: (Char) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp, horizontal = 4.dp)
            .pointerInput(letters, enabledLetters) {
                awaitPointerEventScope {
                    while (true) {
                        // 1. 灵敏度优化：使用 Initial 通道提前拦截按下事件
                        val downEvent = awaitPointerEvent(PointerEventPass.Initial)
                        val downChange = downEvent.changes.firstOrNull { it.pressed } ?: continue

                        // 获取高度并计算
                        val currentHeight = size.height
                        if (currentHeight <= 0) continue

                        // 标记事件已处理，防止父容器（如 LazyColumn）拦截
                        downChange.consume()
                        
                        // 触发开始状态
                        onDragStart()

                        // 处理首次按下位置
                        val processIndex = { y: Float ->
                            val index = calculateLetterIndex(y, letters.size, currentHeight.toFloat())
                            if (index in letters.indices) {
                                val letter = letters[index]
                                if (letter in enabledLetters) {
                                    onLetterSelected(letter)
                                }
                            }
                        }

                        processIndex(downChange.position.y)

                        // 2. 灵敏度优化：持续跟踪移动
                        var isPressed = true
                        while (isPressed) {
                            val moveEvent = awaitPointerEvent(PointerEventPass.Initial)
                            val moveChange = moveEvent.changes.firstOrNull { it.pressed }
                            
                            if (moveChange != null) {
                                moveChange.consume()
                                processIndex(moveChange.position.y)
                            } else {
                                isPressed = false
                            }
                        }

                        // 结束状态
                        onDragEnd()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { letter ->
                val isSelected = letter == currentSelectedLetter
                val isEnabled = letter in enabledLetters
                Text(
                    text = letter.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (isSelected) 14.sp else 10.sp,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * 计算触摸位置对应的字母索引
 */
private fun calculateLetterIndex(y: Float, letterCount: Int, totalHeight: Float): Int {
    if (totalHeight <= 0 || letterCount <= 0) return 0
    val itemHeight = totalHeight / letterCount
    val index = (y / itemHeight).toInt().coerceIn(0, letterCount - 1)
    return index
}
