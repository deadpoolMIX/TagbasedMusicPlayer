package com.tagplayer.musicplayer.ui.player.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.ui.player.viewmodel.PlayerViewModel
import com.tagplayer.musicplayer.util.LyricLine
import com.tagplayer.musicplayer.util.LyricsParser
import kotlinx.coroutines.launch

/**
 * 全屏歌词页面
 * 功能：
 * - 逐行高亮显示
 * - 点击歌词跳转
 * - 歌词滚动同步
 * - 下拉/上滑关闭手势
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val currentSong = playbackState.currentSong

    // 解析歌词
    val lyrics = remember(currentSong?.lyrics) {
        currentSong?.lyrics?.let { LyricsParser.parseLyrics(it) } ?: emptyList()
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 当前歌词行索引
    var currentLineIndex by remember { mutableIntStateOf(-1) }

    // 是否正在手动滚动（手动滚动时暂停自动滚动）
    var isUserScrolling by remember { mutableStateOf(false) }
    var userScrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 更新当前歌词行并自动滚动
    LaunchedEffect(currentPosition, lyrics, isUserScrolling) {
        if (lyrics.isEmpty() || isUserScrolling) return@LaunchedEffect

        val newIndex = LyricsParser.getCurrentLineIndex(lyrics, currentPosition)
        if (newIndex != currentLineIndex && newIndex >= 0) {
            currentLineIndex = newIndex

            // 自动滚动到当前行（居中显示）
            scope.launch {
                listState.animateScrollToItem(
                    index = newIndex,
                    scrollOffset = -300 // 负值让当前行偏上，更接近视觉中心
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentSong?.title ?: "未知歌曲",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong?.artist ?: "未知艺术家",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            // 可选：下拉关闭功能
                        },
                        onVerticalDrag = { _, _ ->
                            // 可选：下拉手势处理
                        }
                    )
                }
        ) {
            when {
                lyrics.isEmpty() -> {
                    // 无歌词提示
                    EmptyLyricsContent()
                }
                else -> {
                    // 歌词列表
                    LyricsList(
                        lyrics = lyrics,
                        currentLineIndex = currentLineIndex,
                        listState = listState,
                        onLineClick = { line ->
                            // 点击歌词跳转到对应时间
                            viewModel.seekTo(line.timestampMs)
                        },
                        onUserScroll = {
                            // 用户手动滚动时暂停自动滚动
                            isUserScrolling = true
                            userScrollJob?.cancel()
                            userScrollJob = scope.launch {
                                kotlinx.coroutines.delay(3000) // 3秒后恢复自动滚动
                                isUserScrolling = false
                            }
                        }
                    )
                }
            }

            // 顶部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                Color.Transparent
                            )
                        )
                    )
            )

            // 底部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }
    }
}

/**
 * 歌词列表组件
 */
@Composable
private fun LyricsList(
    lyrics: List<LyricLine>,
    currentLineIndex: Int,
    listState: LazyListState,
    onLineClick: (LyricLine) -> Unit,
    onUserScroll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // 监听滚动状态
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            onUserScroll()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部占位，让第一行歌词能居中显示
        item {
            Spacer(modifier = Modifier.height(250.dp))
        }

        itemsIndexed(
            items = lyrics,
            key = { index, line -> "${line.timestampMs}_$index" }
        ) { index, line ->
            val isCurrentLine = index == currentLineIndex
            val distance = kotlin.math.abs(index - currentLineIndex)

            LyricLineItem(
                line = line,
                isCurrentLine = isCurrentLine,
                distanceFromCurrent = distance,
                onClick = { onLineClick(line) }
            )
        }

        // 底部占位
        item {
            Spacer(modifier = Modifier.height(300.dp))
        }
    }
}

/**
 * 单行歌词组件
 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    isCurrentLine: Boolean,
    distanceFromCurrent: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 动画属性
    val alpha = remember { Animatable(0.5f) }
    val fontSize = remember { Animatable(16f) }

    LaunchedEffect(isCurrentLine, distanceFromCurrent) {
        val targetAlpha = when {
            isCurrentLine -> 1f
            distanceFromCurrent == 1 -> 0.7f
            distanceFromCurrent == 2 -> 0.5f
            else -> 0.3f.coerceAtLeast(0.2f)
        }
        val targetFontSize = if (isCurrentLine) 18f else 16f

        alpha.animateTo(
            targetValue = targetAlpha,
            animationSpec = tween(durationMillis = 300, easing = LinearEasing)
        )
        fontSize.animateTo(
            targetValue = targetFontSize,
            animationSpec = tween(durationMillis = 300, easing = LinearEasing)
        )
    }

    Text(
        text = line.text.ifEmpty { "♪" },
        modifier = modifier
            .fillMaxWidth(0.85f)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .alpha(alpha.value)
            .clickable(enabled = line.text.isNotEmpty()) { onClick() },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize.value.sp
        ),
        color = if (isCurrentLine) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        textAlign = TextAlign.Center,
        maxLines = 2
    )
}

/**
 * 无歌词提示组件
 */
@Composable
private fun EmptyLyricsContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无歌词",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "支持的歌词来源：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• 音频文件内嵌歌词",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "• 同目录同名 .lrc 文件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}