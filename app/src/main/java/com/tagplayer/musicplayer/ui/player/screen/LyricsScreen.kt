package com.tagplayer.musicplayer.ui.player.screen

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.ui.player.viewmodel.PlayerViewModel
import com.tagplayer.musicplayer.util.LyricLine
import com.tagplayer.musicplayer.util.LyricsParser
import kotlinx.coroutines.launch

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

    // 下滑关闭手势
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedScale by animateFloatAsState(
        targetValue = 1f - (swipeOffset / 1000f).coerceIn(0f, 0.3f),
        label = "scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f - (swipeOffset / 500f).coerceIn(0f, 1f),
        label = "alpha"
    )

    // 是否正在手动滚动
    var isUserScrolling by remember { mutableStateOf(false) }

    // 获取屏幕高度用于计算居中偏移
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenHeightPx = with(LocalDensity.current) { screenHeightDp.toPx() }

    // 歌词项高度（用于居中计算）
    val itemHeightPx = with(LocalDensity.current) { 48.dp.toPx() }

    // 更新当前歌词行并自动滚动
    LaunchedEffect(currentPosition, lyrics, isUserScrolling) {
        if (lyrics.isEmpty() || isUserScrolling) return@LaunchedEffect

        val newIndex = LyricsParser.getCurrentLineIndex(lyrics, currentPosition)
        if (newIndex != currentLineIndex && newIndex >= 0) {
            currentLineIndex = newIndex

            // 计算居中偏移：屏幕高度的一半减去歌词项高度的一半
            // 这样当前歌词会显示在屏幕中央
            scope.launch {
                listState.animateScrollToItem(
                    index = newIndex,
                    scrollOffset = -(screenHeightPx / 2 - itemHeightPx / 2).toInt()
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
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "关闭"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .alpha(animatedAlpha)
                .scale(animatedScale)
                .pointerInput(listState.canScrollBackward) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (swipeOffset > 300) {
                                onBackClick()
                            }
                            swipeOffset = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (!listState.canScrollBackward && dragAmount > 0) {
                                change.consume()
                                swipeOffset += dragAmount
                            }
                        }
                    )
                }
        ) {
            if (lyrics.isEmpty()) {
                EmptyLyricsContent()
            } else {
                LyricsList(
                    lyrics = lyrics,
                    currentLineIndex = currentLineIndex,
                    listState = listState,
                    onLineClick = { line ->
                        viewModel.seekTo(line.timestampMs)
                    },
                    onUserScroll = {
                        isUserScrolling = true
                        scope.launch {
                            kotlinx.coroutines.delay(3000)
                            isUserScrolling = false
                        }
                    }
                )
            }

            // 顶部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
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
                    .height(60.dp)
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

@Composable
private fun LyricsList(
    lyrics: List<LyricLine>,
    currentLineIndex: Int,
    listState: LazyListState,
    onLineClick: (LyricLine) -> Unit,
    onUserScroll: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        // 不再需要顶部占位，居中由 scrollOffset 控制
        itemsIndexed(
            items = lyrics,
            key = { index, line -> "${line.timestampMs}_$index" }
        ) { index, line ->
            val isCurrentLine = index == currentLineIndex
            val distance = kotlin.math.abs(index - currentLineIndex)

            // 支持多行文本（双语歌词用换行符分隔）
            val displayText = line.text
            val lineCount = displayText.lines().filter { it.isNotBlank() }.size

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(horizontal = 16.dp, vertical = if (lineCount > 1) 8.dp else 12.dp)
                    .alpha(
                        when {
                            isCurrentLine -> 1f
                            distance == 1 -> 0.7f
                            distance == 2 -> 0.5f
                            else -> 0.35f
                        }
                    )
                    .clickable { onLineClick(line) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 按行显示歌词（支持双语）
                displayText.lines().filter { it.isNotBlank() }.forEach { textLine ->
                    Text(
                        text = textLine,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = if (isCurrentLine) 18.sp else 16.sp
                        ),
                        color = if (isCurrentLine) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        // 底部留白，让最后一行歌词也能滚动到中央
        item {
            Spacer(modifier = Modifier.height(400.dp))
        }
    }
}

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