package com.tagplayer.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 全局统一垂直滚动条组件
 *
 * 关键设计：
 * 1. 基于索引的精确进度计算（含小数部分）- 线性滑动
 * 2. 状态分离：拖拽时滑块位置由手势驱动，否则由列表状态驱动
 * 3. 切断死循环：拖拽期间忽略列表滚动带来的状态回传
 * 4. 触控顶层：点击滑动条不穿透到下层界面
 *
 * 统一规格：
 * - 宽度: 20dp（含padding，实际内容约12dp）
 * - 触控层级: 顶层覆盖，阻止事件穿透
 * - 滑动逻辑: 线性对应
 */
@Composable
fun VerticalScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (itemCount == 0) return

    val coroutineScope = rememberCoroutineScope()

    // 状态分离：拖拽状态
    var isDragging by remember { mutableStateOf(false) }
    // 拖拽时的进度值（由手势驱动）
    var dragProgress by remember { mutableStateOf(0f) }
    // 轨道高度（像素）
    var trackHeightPx by remember { mutableStateOf(0) }

    // 计算可见项目数量
    val visibleItemCount = listState.layoutInfo.visibleItemsInfo.size

    // 不需要滚动条的情况
    if (visibleItemCount >= itemCount) return

    // ========== 1. 精确的进度计算（基于 Index 含小数）==========
    val listProgress by remember {
        derivedStateOf {
            if (itemCount == 0) return@derivedStateOf 0f

            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            // 获取可见项目的高度（假设高度大致相同）
            val itemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1

            // 精确索引 = 整数部分 + 小数部分（偏移量/项目高度）
            val exactIndex = firstVisibleIndex + (firstVisibleOffset.toFloat() / itemHeight.toFloat())

            // 进度 = 精确索引 / 总项目数（线性对应）
            (exactIndex / itemCount.toFloat()).coerceIn(0f, 1f)
        }
    }

    // 滑块高度比例
    val thumbHeightPercent = visibleItemCount.toFloat() / itemCount.toFloat()

    // ========== 2. 状态分离：选择数据源 ==========
    // 拖拽时用手势驱动的进度，否则用列表状态驱动的进度
    val displayProgress = if (isDragging) dragProgress else listProgress

    // ========== 3. 滑块位置计算 ==========
    // 滑块Y坐标 = 进度 * (轨道高度 - 滑块高度)
    val thumbOffsetY = (trackHeightPx * (1f - thumbHeightPercent) * displayProgress).toInt()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(20.dp)  // 统一宽度
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .onSizeChanged { trackHeightPx = it.height }
            // 触控顶层：消费所有事件，阻止穿透到下层界面
            .pointerInput(itemCount, trackHeightPx) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        when (event.type) {
                            PointerEventType.Press -> {
                                // 开始拖拽：记录初始进度
                                isDragging = true
                                dragProgress = displayProgress
                                // 消费事件阻止穿透
                                event.changes.forEach { it.consume() }
                            }
                            PointerEventType.Release -> {
                                // 结束拖拽
                                isDragging = false
                                event.changes.forEach { it.consume() }
                            }
                            PointerEventType.Move -> {
                                if (isDragging && trackHeightPx > 0) {
                                    val change = event.changes.first()

                                    // 手指在轨道上的Y坐标
                                    val touchY = change.position.y

                                    // 可滚动的轨道高度 = 总轨道高度 - 滑块高度
                                    val scrollableTrackHeight = trackHeightPx * (1f - thumbHeightPercent)

                                    // 计算新进度（0-1，线性对应）
                                    val newProgress = (touchY / scrollableTrackHeight).coerceIn(0f, 1f)

                                    // 更新拖拽进度（由手势驱动）
                                    dragProgress = newProgress

                                    // 反向计算目标索引
                                    val targetIndex = (newProgress * itemCount).toInt()
                                        .coerceIn(0, itemCount - 1)

                                    // 驱动列表滚动
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }

                                    // 消费事件阻止穿透
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // 滚动条背景轨道
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isDragging)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
        )

        // 滚动条滑块
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(thumbHeightPercent)
                .offset { IntOffset(0, thumbOffsetY) }
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isDragging)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
        )
    }
}