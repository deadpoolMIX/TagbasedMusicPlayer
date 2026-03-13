package com.tagplayer.musicplayer.ui.artist.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.repository.Artist
import com.tagplayer.musicplayer.ui.artist.viewmodel.ArtistViewModel
import com.tagplayer.musicplayer.util.AlphabetIndexUtils
import kotlinx.coroutines.awaitCancellation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistListScreen(
    onBackClick: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsState()
    val listState = rememberLazyListState()

    // 使用新的分组工具类按首字母分组艺术家
    val groupedArtists = remember(artists) {
        AlphabetIndexUtils.groupByFirstLetter(artists) { it.name }
    }

    // 字母索引 A-Z + #（固定显示）
    val alphabetIndex = remember { AlphabetIndexUtils.getAlphabetIndex() }

    // 可用的字母（根据实际数据过滤）
    val availableLetters = remember(groupedArtists) {
        groupedArtists.keys
    }

    // 字母到索引位置的映射（用于滚动定位）
    val letterToIndexMap = remember(groupedArtists) {
        AlphabetIndexUtils.calculateLetterToIndexMap(groupedArtists)
    }

    // 当前选中的字母（用于气泡提示）
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "艺术家",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
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
            // 主列表
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                groupedArtists.forEach { (letter, artistList) ->
                    // 分组标题（字母）
                    item(key = "header_$letter") {
                        LetterHeader(letter = letter)
                    }

                    // 该字母下的艺术家
                    items(
                        items = artistList,
                        key = { it.name }
                    ) { artist ->
                        ArtistItem(
                            artist = artist,
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }
            }

            // 右侧字母索引栏 - 显示固定的 A-Z + #
            AlphabetIndexBar(
                letters = alphabetIndex,
                enabledLetters = availableLetters,
                currentSelectedLetter = selectedLetter,
                onLetterSelected = { letter ->
                    selectedLetter = letter
                    // 滚动到对应位置
                    letterToIndexMap[letter]?.let { index ->
                        kotlinx.coroutines.runBlocking {
                            listState.scrollToItem(index)
                        }
                    }
                },
                onDragStart = { isDragging = true },
                onDragEnd = {
                    isDragging = false
                    selectedLetter = null
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            // 中央气泡提示
            AnimatedVisibility(
                visible = selectedLetter != null && isDragging,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                selectedLetter?.let { letter ->
                    LetterBubble(letter = letter)
                }
            }
        }
    }
}

@Composable
private fun LetterHeader(
    letter: Char,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ArtistItem(
    artist: Artist,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 艺术家头像
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 艺术家信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${artist.songCount} 首歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 字母索引栏组件
 * 使用底层 pointerInput + awaitPointerEventScope 实现手势处理
 * 在手势协程内部实时通过 size.height 获取高度，避免闭包陷阱
 */
@Composable
private fun AlphabetIndexBar(
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
            // 关键：使用 Unit 作为 key，确保只初始化一次
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // 等待手指按下 - 使用 Main 事件传递
                        val downEvent = awaitPointerEvent(PointerEventPass.Main)
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                            ?: continue

                        // 关键：在手势协程内部实时获取尺寸
                        val currentHeight = size.height
                        if (currentHeight <= 0) continue

                        // 开始拖拽状态
                        onDragStart()

                        // 处理按下位置
                        val initialY = downChange.position.y
                        val initialIndex = calculateLetterIndex(
                            initialY,
                            letters.size,
                            currentHeight.toFloat()
                        )
                        if (initialIndex in letters.indices) {
                            val letter = letters[initialIndex]
                            if (letter in enabledLetters) {
                                onLetterSelected(letter)
                            }
                        }

                        // 持续跟踪移动直到手指抬起
                        var isPressed = true
                        while (isPressed) {
                            val moveEvent = awaitPointerEvent(PointerEventPass.Main)

                            // 再次实时获取高度（可能在拖拽过程中有变化）
                            val height = size.height
                            if (height <= 0) continue

                            for (change in moveEvent.changes) {
                                if (change.pressed) {
                                    // 手指仍在按压，更新位置
                                    val y = change.position.y
                                    val index = calculateLetterIndex(
                                        y,
                                        letters.size,
                                        height.toFloat()
                                    )
                                    if (index in letters.indices) {
                                        val letter = letters[index]
                                        if (letter in enabledLetters) {
                                            onLetterSelected(letter)
                                        }
                                    }
                                } else {
                                    // 手指抬起
                                    isPressed = false
                                }
                            }
                        }

                        // 拖拽结束
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
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .then(
                            if (isEnabled) {
                                Modifier.clickable { onLetterSelected(letter) }
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}

/**
 * 计算触摸位置对应的字母索引
 * 纯函数，无状态依赖
 */
private fun calculateLetterIndex(y: Float, letterCount: Int, totalHeight: Float): Int {
    if (totalHeight <= 0 || letterCount <= 0) return 0
    val itemHeight = totalHeight / letterCount
    val index = (y / itemHeight).toInt().coerceIn(0, letterCount - 1)
    return index
}

@Composable
private fun LetterBubble(
    letter: Char,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
