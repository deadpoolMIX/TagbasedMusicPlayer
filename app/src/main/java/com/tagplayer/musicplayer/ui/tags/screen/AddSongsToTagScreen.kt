package com.tagplayer.musicplayer.ui.tags.screen

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.ui.tags.viewmodel.TagViewModel
import com.tagplayer.musicplayer.util.AlphabetIndexUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToTagScreen(
    tagId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagViewModel = hiltViewModel()
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val tagSongs by viewModel.selectedTagSongs.collectAsState()
    var selectedSongs by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // 过滤掉已有此标签的歌曲
    val tagSongIds = tagSongs.map { it.id }.toSet()
    val availableSongs = allSongs
        .filter { it.id !in tagSongIds }
        .filter { song ->
            searchQuery.isBlank() ||
            song.title.contains(searchQuery, ignoreCase = true) ||
            song.artist.contains(searchQuery, ignoreCase = true)
        }

    // 按首字母分组排序
    val groupedSongs = remember(availableSongs) {
        AlphabetIndexUtils.groupByFirstLetter(availableSongs) { it.title }
    }
    val letterToIndexMap = remember(groupedSongs) {
        AlphabetIndexUtils.calculateLetterToIndexMap(groupedSongs)
    }

    // 字母索引相关状态
    val alphabetIndex = remember { AlphabetIndexUtils.getAlphabetIndex() }
    val availableLetters = remember(groupedSongs) { groupedSongs.keys }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "添加歌曲",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "已选择 ${selectedSongs.size} 首",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (selectedSongs.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.addTagToSongs(tagId, selectedSongs.toList())
                                // 清除搜索词并关闭键盘
                                searchQuery = ""
                                focusManager.clearFocus()
                                onBackClick()
                            }
                        ) {
                            Text("添加 (${selectedSongs.size})")
                        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索歌曲...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 歌曲列表
                if (availableSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "未找到匹配歌曲" else "没有可添加的歌曲",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // 带字母索引的分组列表
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(end = 32.dp)
                        ) {
                            groupedSongs.forEach { (letter, songList) ->
                                // 分组标题
                                item(key = "header_$letter") {
                                    LetterHeader(letter = letter)
                                }

                                // 该字母下的歌曲
                                items(
                                    items = songList,
                                    key = { it.id }
                                ) { song ->
                                    val isSelected = song.id in selectedSongs
                                    SelectableSongItem(
                                        song = song,
                                        isSelected = isSelected,
                                        onToggle = {
                                            selectedSongs = if (isSelected) {
                                                selectedSongs - song.id
                                            } else {
                                                selectedSongs + song.id
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // 右侧字母索引栏
                        AlphabetIndexBar(
                            letters = alphabetIndex,
                            enabledLetters = availableLetters,
                            currentSelectedLetter = selectedLetter,
                            onLetterSelected = { letter ->
                                selectedLetter = letter
                                letterToIndexMap[letter]?.let { index ->
                                    scope.launch {
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
                        androidx.compose.animation.AnimatedVisibility(
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

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // 底部按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            viewModel.addTagToSongs(tagId, selectedSongs.toList())
                            // 清除搜索词并关闭键盘
                            searchQuery = ""
                            focusManager.clearFocus()
                            onBackClick()
                        },
                        enabled = selectedSongs.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("添加 (${selectedSongs.size})")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableSongItem(
    song: Song,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择指示器
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 歌曲信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 分组标题组件
 */
@Composable
private fun LetterHeader(
    letter: Char,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * 字母索引栏组件
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
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent(PointerEventPass.Main)
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                            ?: continue

                        val currentHeight = size.height
                        if (currentHeight <= 0) continue

                        onDragStart()

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

                        var isPressed = true
                        while (isPressed) {
                            val moveEvent = awaitPointerEvent(PointerEventPass.Main)

                            val height = size.height
                            if (height <= 0) continue

                            for (change in moveEvent.changes) {
                                if (change.pressed) {
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
                                    isPressed = false
                                }
                            }
                        }

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
 */
private fun calculateLetterIndex(y: Float, letterCount: Int, totalHeight: Float): Int {
    if (totalHeight <= 0 || letterCount <= 0) return 0
    val itemHeight = totalHeight / letterCount
    val index = (y / itemHeight).toInt().coerceIn(0, letterCount - 1)
    return index
}

/**
 * 中央气泡提示组件
 */
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