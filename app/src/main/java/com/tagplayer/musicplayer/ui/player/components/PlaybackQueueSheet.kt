package com.tagplayer.musicplayer.ui.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tagplayer.musicplayer.data.local.entity.Song
import kotlinx.coroutines.launch

@Composable
fun PlaybackQueueSheet(
    isVisible: Boolean,
    queue: List<Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onMoveSong: (fromIndex: Int, toIndex: Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.slideInVertically { it },
        exit = androidx.compose.animation.slideOutVertically { it }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                DraggableQueueContent(
                    queue = queue,
                    currentIndex = currentIndex,
                    onDismiss = onDismiss,
                    onSongClick = onSongClick,
                    onRemoveSong = onRemoveSong,
                    onMoveSong = onMoveSong,
                    onClearQueue = onClearQueue
                )
            }
        }
    }
}

@Composable
private fun DraggableQueueContent(
    queue: List<Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onMoveSong: (fromIndex: Int, toIndex: Int) -> Unit,
    onClearQueue: () -> Unit
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (swipeOffset > 300) {
                            onDismiss()
                        }
                        swipeOffset = 0f
                        isDragging = false
                    },
                    onVerticalDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                        change.consume()
                        if (dragAmount > 0) {
                            swipeOffset += dragAmount
                            isDragging = true
                        }
                    }
                )
            }
            .padding(16.dp)
    ) {
        // 拖动指示条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "播放队列 (${queue.size})",
                style = MaterialTheme.typography.titleMedium
            )
            Row {
                TextButton(onClick = onClearQueue) {
                    Text("清空")
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Queue List with drag support
        ReorderableQueueList(
            queue = queue,
            currentIndex = currentIndex,
            onSongClick = onSongClick,
            onRemoveSong = onRemoveSong,
            onMoveSong = onMoveSong
        )
    }
}

@Composable
private fun ReorderableQueueList(
    queue: List<Song>,
    currentIndex: Int,
    onSongClick: (Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onMoveSong: (fromIndex: Int, toIndex: Int) -> Unit
) {
    // 使用 SnapshotStateList 来管理可变的列表状态
    val items = remember { mutableStateListOf<Song>() }

    // 当外部 queue 变化时，同步到内部列表
    androidx.compose.runtime.LaunchedEffect(queue) {
        if (items.toList() != queue) {
            items.clear()
            items.addAll(queue)
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 拖拽状态
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    val itemHeight = 56.dp  // 预估每项高度
    val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemHeight.toPx() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { index, song -> "${song.id}_$index" }
        ) { index, song ->
            val isDragged = draggedIndex == index
            val elevation by animateFloatAsState(
                targetValue = if (isDragged) 8f else 0f,
                label = "elevation"
            )

            val scale by animateFloatAsState(
                targetValue = if (isDragged) 1.02f else 1f,
                label = "scale"
            )

            QueueItem(
                song = song,
                isPlaying = index == currentIndex,
                isDragged = isDragged,
                elevation = elevation,
                scale = scale,
                dragOffsetY = if (isDragged) dragOffsetY else 0f,
                onClick = { onSongClick(index) },
                onRemove = { onRemoveSong(index) },
                onDragStart = { offsetY ->
                    draggedIndex = index
                    dragStartY = offsetY
                    dragOffsetY = 0f
                },
                onDrag = { dragAmountY ->
                    dragOffsetY += dragAmountY

                    // 计算目标位置
                    val totalDrag = dragOffsetY
                    val itemsDragged = (totalDrag / itemHeightPx).toInt()
                    val targetIndex = (draggedIndex + itemsDragged).coerceIn(0, items.size - 1)

                    if (targetIndex != draggedIndex) {
                        // 在列表中交换位置
                        val item = items.removeAt(draggedIndex)
                        items.add(targetIndex, item)

                        // 通知外部
                        onMoveSong(draggedIndex, targetIndex)

                        // 更新拖拽索引
                        draggedIndex = targetIndex
                        // 重置偏移量，避免跳跃
                        dragOffsetY = 0f
                    }
                },
                onDragEnd = {
                    draggedIndex = -1
                    dragOffsetY = 0f
                    dragStartY = 0f
                },
                modifier = if (isDragged) Modifier.zIndex(1f) else Modifier
            )
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    isPlaying: Boolean,
    isDragged: Boolean,
    elevation: Float,
    scale: Float,
    dragOffsetY: Float,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = dragOffsetY
                shadowElevation = elevation
            }
            .background(
                if (isDragged) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 拖拽手柄（三条杠图标）
        Box(
            modifier = Modifier
                .size(40.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            onDragStart(offset.y)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.y)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "拖动排序",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 正在播放指示器
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }

        // 音乐图标
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isPlaying) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 歌曲信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 删除按钮
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}
