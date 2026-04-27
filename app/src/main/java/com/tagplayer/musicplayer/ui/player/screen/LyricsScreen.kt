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
import androidx.compose.runtime.DisposableEffect
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

    // 追踪组件是否活跃（用于阻止触摸事件穿透）
    var isActive by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        onDispose {
            isActive = false
        }
    }

    // 实时加载歌词（从 .lrc 文件或内嵌歌词）
    val context = androidx.compose.ui.platform.LocalContext.current
    var loadedLyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }

    LaunchedEffect(currentSong?.filePath) {
        currentSong?.filePath?.let { filePath ->
            val lyricsContent = LyricsParser.getLyrics(context, filePath)
            loadedLyrics = if (!lyricsContent.isNullOrBlank()) {
                LyricsParser.parseLyrics(lyricsContent)
            } else {
                emptyList()
            }
        }
    }

    // 解析歌词
    val lyrics = loadedLyrics

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
    var isInitialScrollDone by remember { mutableStateOf(false) }

    // 首次进入时，不使用动画直接定位到当前播放句
    LaunchedEffect(lyrics) {
        if (lyrics.isNotEmpty()) {
            isInitialScrollDone = false
            // 等待视图布局完成
            while (listState.layoutInfo.viewportSize.height == 0) {
                kotlinx.coroutines.delay(50)
            }
            val initialIndex = LyricsParser.getCurrentLineIndex(lyrics, currentPosition)
            if (initialIndex >= 0) {
                currentLineIndex = initialIndex
                val viewportHeight = listState.layoutInfo.viewportSize.height
                val itemHeight = 100 // 初次可能未测量出实际高度，给个默认值
                // 负值表示从顶部向下偏移。为了抵消顶部标题栏，向上偏移一行(减去 lineOffset)
                val lineOffset = 150 // 大约一行歌词的高度
                val targetOffset = -(viewportHeight / 2 - itemHeight / 2 - lineOffset)
                listState.scrollToItem(initialIndex + 1, targetOffset)
            }
            isInitialScrollDone = true
        }
    }

    // 更新当前歌词行并平滑滚动（跳过初始化阶段）
    LaunchedEffect(currentPosition, lyrics, isUserScrolling, isInitialScrollDone) {
        if (lyrics.isEmpty() || isUserScrolling || !isInitialScrollDone) return@LaunchedEffect

        val newIndex = LyricsParser.getCurrentLineIndex(lyrics, currentPosition)
        if (newIndex != currentLineIndex && newIndex >= 0) {
            currentLineIndex = newIndex

            val viewportHeight = listState.layoutInfo.viewportSize.height
            if (viewportHeight > 0) {
                scope.launch {
                    val targetIndex = newIndex + 1
                    val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
                    val itemHeight = itemInfo?.size ?: 100

                    // 负值表示从顶部向下偏移。为了让歌词在正中央偏上一行
                    val lineOffset = 150
                    val targetOffset = -(viewportHeight / 2 - itemHeight / 2 - lineOffset)
                    listState.animateScrollToItem(
                        index = targetIndex,
                        scrollOffset = targetOffset
                    )                }
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
                    isActive = isActive,
                    onLineClick = { line ->
                        viewModel.seekTo(line.timestampMs)
                        viewModel.play()
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
    isActive: Boolean,
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
        // 顶部留白，让第一行歌词也能滚动到中央
        item {
            Spacer(modifier = Modifier.height(400.dp))
        }

        itemsIndexed(
            items = lyrics,
            key = { index, line -> "${line.timestampMs}_$index" }
        ) { index, line ->
            val isCurrentLine = index == currentLineIndex
            val distance = kotlin.math.abs(index - currentLineIndex)

            // 支持多行文本（双语歌词用换行符分隔）
            val displayText = line.text
            val lines = displayText.lines().filter { it.isNotBlank() }

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .alpha(
                        when {
                            isCurrentLine -> 1f
                            distance == 1 -> 0.7f
                            distance == 2 -> 0.5f
                            else -> 0.3f
                        }
                    )
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        enabled = isActive
                    ) { onLineClick(line) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 按行显示歌词（支持双语）
                lines.forEachIndexed { textIndex, textLine ->
                    val isTranslation = textIndex > 0

                    val fontSize = if (isTranslation) {
                        if (isCurrentLine) 16.sp else 14.sp
                    } else {
                        if (isCurrentLine) 20.sp else 18.sp
                    }

                    val fontWeight = if (isCurrentLine) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal

                    Text(
                        text = textLine,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize,
                            fontWeight = fontWeight
                        ),
                        color = if (isCurrentLine) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
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