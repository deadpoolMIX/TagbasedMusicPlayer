package com.tagplayer.musicplayer.ui.album.screen

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.repository.Album
import com.tagplayer.musicplayer.ui.album.viewmodel.AlbumViewModel
import com.tagplayer.musicplayer.util.AlphabetIndexUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
    onBackClick: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var columnCount by remember { mutableIntStateOf(4) }
    var sortType by remember { mutableStateOf(AlbumSortType.BY_TITLE) }
    val gridState = rememberLazyGridState()

    // 应用排序并按首字母分组
    val sortedAlbums = remember(albums, sortType) {
        when (sortType) {
            AlbumSortType.BY_TITLE -> albums.sortedBy {
                AlphabetIndexUtils.getFirstLetter(it.name).toString() + it.name.lowercase()
            }
            AlbumSortType.BY_YEAR -> albums.sortedBy { it.songs.firstOrNull()?.dateAdded ?: 0L }
            AlbumSortType.BY_COUNT -> albums.sortedByDescending { it.songCount }
        }
    }

    // 按首字母分组（使用新的工具类）
    val groupedAlbums = remember(sortedAlbums) {
        AlphabetIndexUtils.groupByFirstLetter(sortedAlbums) { it.name }
    }

    // 字母索引 A-Z + #（固定显示）
    val alphabetIndex = remember { AlphabetIndexUtils.getAlphabetIndex() }

    // 可用的字母（根据实际数据过滤）
    val availableLetters = remember(groupedAlbums) {
        groupedAlbums.keys
    }

    // 字母到网格索引的映射
    val letterToIndexMap = remember(groupedAlbums, columnCount) {
        AlphabetIndexUtils.calculateLetterToGridIndexMap(groupedAlbums, columnCount)
    }

    // 当前选中的字母（用于气泡提示）
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "专辑",
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
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
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
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columnCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = sortedAlbums,
                    key = { "${it.name}_${it.artist}" }
                ) { album ->
                    AlbumItem(
                        album = album,
                        onClick = { onAlbumClick(album) }
                    )
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
                            gridState.scrollToItem(index)
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

    // 设置对话框
    if (showSettingsDialog) {
        AlbumSettingsDialog(
            currentColumnCount = columnCount,
            currentSortType = sortType,
            onDismiss = { showSettingsDialog = false },
            onColumnCountChange = { columnCount = it },
            onSortTypeChange = { sortType = it }
        )
    }
}

@Composable
private fun AlbumItem(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // 专辑封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 专辑名称
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 歌曲数量
        Text(
            text = "${album.songCount} 首",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class AlbumSortType {
    BY_TITLE, BY_YEAR, BY_COUNT
}

@Composable
private fun AlbumSettingsDialog(
    currentColumnCount: Int,
    currentSortType: AlbumSortType,
    onDismiss: () -> Unit,
    onColumnCountChange: (Int) -> Unit,
    onSortTypeChange: (AlbumSortType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("专辑列表设置") },
        text = {
            Column {
                // 排序选项
                Text(
                    text = "排序",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                SortOption(
                    text = "标题",
                    selected = currentSortType == AlbumSortType.BY_TITLE,
                    onClick = { onSortTypeChange(AlbumSortType.BY_TITLE) }
                )
                SortOption(
                    text = "年份",
                    selected = currentSortType == AlbumSortType.BY_YEAR,
                    onClick = { onSortTypeChange(AlbumSortType.BY_YEAR) }
                )
                SortOption(
                    text = "数量",
                    selected = currentSortType == AlbumSortType.BY_COUNT,
                    onClick = { onSortTypeChange(AlbumSortType.BY_COUNT) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 列数选项
                Text(
                    text = "最小列数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                ColumnCountOption(
                    count = 2,
                    selected = currentColumnCount == 2,
                    onClick = { onColumnCountChange(2) }
                )
                ColumnCountOption(
                    count = 3,
                    selected = currentColumnCount == 3,
                    onClick = { onColumnCountChange(3) }
                )
                ColumnCountOption(
                    count = 4,
                    selected = currentColumnCount == 4,
                    onClick = { onColumnCountChange(4) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun SortOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

@Composable
private fun ColumnCountOption(
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = count.toString())
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
