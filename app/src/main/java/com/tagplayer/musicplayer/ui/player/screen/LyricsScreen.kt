package com.tagplayer.musicplayer.ui.player.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
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
    val lyrics = remember(currentSong) {
        currentSong?.lyrics?.let { LyricsParser.parseLrc(it) } ?: emptyList()
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 当前歌词行索引
    var currentLineIndex by remember { mutableIntStateOf(-1) }

    // 更新当前歌词行
    LaunchedEffect(currentPosition, lyrics) {
        val newIndex = LyricsParser.getCurrentLineIndex(lyrics, currentPosition)
        if (newIndex != currentLineIndex && newIndex >= 0) {
            currentLineIndex = newIndex
            // 自动滚动到当前行
            scope.launch {
                listState.animateScrollToItem(
                    index = newIndex,
                    scrollOffset = -200 // 向上偏移，让当前行在中间
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("歌词") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
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
        ) {
            if (lyrics.isEmpty()) {
                // 无歌词提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无歌词",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 歌词列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部占位
                    item { Spacer(modifier = Modifier.height(200.dp)) }

                    itemsIndexed(
                        items = lyrics,
                        key = { index, line -> "${line.time}_$index" }
                    ) { index, line ->
                        val isCurrentLine = index == currentLineIndex
                        val alpha by animateFloatAsState(
                            targetValue = if (isCurrentLine) 1f else 0.5f,
                            label = "lyric_alpha"
                        )
                        val fontSize by animateFloatAsState(
                            targetValue = if (isCurrentLine) 18f else 16f,
                            label = "lyric_size"
                        )

                        Text(
                            text = line.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 12.dp)
                                .alpha(alpha)
                                .clickable {
                                    // 点击歌词跳转到对应时间
                                    viewModel.seekTo(line.time)
                                },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSize.sp
                            ),
                            color = if (isCurrentLine) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textAlign = TextAlign.Center
                        )
                    }

                    // 底部占位
                    item { Spacer(modifier = Modifier.height(200.dp)) }
                }

                // 当前播放信息
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentSong?.title ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = currentSong?.artist ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
