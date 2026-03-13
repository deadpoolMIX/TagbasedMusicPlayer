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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.repository.Artist
import com.tagplayer.musicplayer.ui.artist.viewmodel.ArtistViewModel
import com.tagplayer.musicplayer.util.PinyinUtils
import kotlinx.coroutines.delay

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

    // 按首字母分组艺术家（使用拼音）
    val groupedArtists = remember(artists) {
        artists.groupBy { artist ->
            PinyinUtils.getFirstLetter(artist.name)
        }.toSortedMap(compareBy { it })
    }

    // 字母索引只显示 # 和 A-Z（固定显示）
    val alphabetIndex = remember { PinyinUtils.getAlphabetIndex() }

    // 可用的字母（根据实际数据过滤）
    val availableLetters = remember(groupedArtists) {
        alphabetIndex.filter { letter ->
            groupedArtists.containsKey(letter)
        }
    }

    // 字母到索引位置的映射（用于滚动定位）
    val letterToIndexMap = remember(groupedArtists) {
        val map = mutableMapOf<Char, Int>()
        var index = 0
        groupedArtists.forEach { (letter, artistList) ->
            map[letter] = index
            index += artistList.size + 1 // +1 为分组标题
        }
        map
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

            // 右侧字母索引栏 - 显示固定的 # + A-Z
            AlphabetIndexBar(
                letters = alphabetIndex,
                enabledLetters = groupedArtists.keys,
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

@Composable
private fun AlphabetIndexBar(
    letters: List<Char>,
    enabledLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSelectedIndex by remember { mutableIntStateOf(-1) }
    var isDragging by remember { mutableStateOf(false) }

    // 计算当前位置对应的字母索引
    fun updateSelectionFromY(y: Float, height: Float) {
        if (height <= 0) return
        val index = calculateLetterIndex(y, letters.size, height)
        if (index in letters.indices) {
            val letter = letters[index]
            if (letter in enabledLetters && index != currentSelectedIndex) {
                currentSelectedIndex = index
                onLetterSelected(letter)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp, horizontal = 4.dp)
            .pointerInput(Unit) {
                // 使用 pointerInput 的 lambda 直接处理事件
                awaitPointerEventScope {
                    while (true) {
                        // 等待手指按下
                        val down = awaitPointerEvent(PointerEventPass.Initial)
                            .changes
                            .firstOrNull { it.pressed }
                            ?: continue

                        // 手指按下，立即触发
                        isDragging = true
                        onDragStart()
                        updateSelectionFromY(down.position.y, size.height.toFloat())

                        // 循环处理移动和抬起
                        var active = true
                        while (active) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { change: PointerInputChange ->
                                if (change.pressed) {
                                    // 手指还在按，更新位置
                                    updateSelectionFromY(change.position.y, size.height.toFloat())
                                } else {
                                    // 手指抬起
                                    active = false
                                }
                            }
                        }

                        // 手指抬起，结束状态
                        isDragging = false
                        currentSelectedIndex = -1
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
            letters.forEachIndexed { index, letter ->
                val isSelected = index == currentSelectedIndex && isDragging
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
                            if (isEnabled && !isDragging) {
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
            .clip(CircleShape)
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
