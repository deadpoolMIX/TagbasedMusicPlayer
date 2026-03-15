package com.tagplayer.musicplayer.ui.home.screen

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.local.entity.ScanFolder
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.ui.components.SongActionSheet
import com.tagplayer.musicplayer.ui.components.SongItem
import com.tagplayer.musicplayer.ui.components.TagSelectionDialog
import com.tagplayer.musicplayer.ui.home.viewmodel.HomeViewModel
import com.tagplayer.musicplayer.ui.home.viewmodel.SortType
import com.tagplayer.musicplayer.ui.player.viewmodel.PlayerViewModel
import com.tagplayer.musicplayer.ui.playlist.viewmodel.PlaylistViewModel
import com.tagplayer.musicplayer.ui.tags.viewmodel.TagViewModel
import com.tagplayer.musicplayer.util.AlphabetIndexUtils
import com.tagplayer.musicplayer.util.PermissionUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    tagViewModel: TagViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val groupedSongs by viewModel.groupedSongs.collectAsState()
    val letterToIndexMap by viewModel.letterToIndexMap.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedSong by viewModel.selectedSong.collectAsState()
    val showActionSheet by viewModel.showActionSheet.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val showFolderManager by viewModel.showFolderManager.collectAsState()
    val showSortDialog by viewModel.showSortDialog.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val scanFolders by viewModel.scanFolders.collectAsState()

    // 搜索对话框状态
    var showSearchDialog by remember { mutableStateOf(false) }

    // 判断是否为标题排序模式（显示分组和索引栏）
    val isTitleSortMode = sortType == SortType.TITLE_ASC || sortType == SortType.TITLE_DESC

    // 字母索引列表
    val alphabetIndex = remember { AlphabetIndexUtils.getAlphabetIndex() }

    // 可用的字母（根据实际数据过滤）
    val availableLetters = remember(groupedSongs) {
        groupedSongs.keys
    }

    // 列表状态
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 当前选中的字母（用于气泡提示）
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // 权限申请启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // 文件夹选择器启动器 (Android 5.0+)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addScanFolderByUri(it)
        }
    }

    // 标签选择对话框状态
    var showTagSelection by remember { mutableStateOf(false) }

    // 首次启动检查权限（只更新状态，不自动申请）
    LaunchedEffect(Unit) {
        viewModel.updatePermissionState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的音乐",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    // 搜索按钮
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                    // 设置按钮
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
                    }
                    // 文件夹管理按钮
                    IconButton(onClick = { viewModel.showFolderManager() }) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "文件夹管理"
                        )
                    }
                    // 扫描按钮
                    IconButton(
                        onClick = { viewModel.checkPermissionAndScan() },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "扫描歌曲"
                            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 统计信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${songs.size} 首歌曲",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 排序按钮
                    IconButton(
                        onClick = { viewModel.showSortDialog() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "排序",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "搜索: \"$searchQuery\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                    )
                }
            }

            // 歌曲列表
            if (songs.isEmpty()) {
                EmptyState(
                    isSearching = searchQuery.isNotBlank(),
                    hasPermission = hasPermission,
                    onScanClick = { viewModel.checkPermissionAndScan() },
                    onGrantPermission = { viewModel.showPermissionDialog() },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isTitleSortMode && groupedSongs.isNotEmpty()) {
                // 标题排序模式：分组显示 + 字母索引栏
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
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
                                SongItem(
                                    song = song,
                                    onClick = {
                                        playerViewModel.setQueue(songs, songs.indexOf(song))
                                    },
                                    onMoreClick = {
                                        viewModel.onSongMoreClick(song)
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
            } else {
                // 非标题排序模式：普通列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = songs,
                        key = { it.id }
                    ) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                // 播放选中的歌曲
                                playerViewModel.setQueue(songs, songs.indexOf(song))
                            },
                            onMoreClick = {
                                viewModel.onSongMoreClick(song)
                            }
                        )
                    }
                }
            }
        }
    }

    // 搜索对话框
    if (showSearchDialog) {
        SearchDialog(
            initialQuery = searchQuery,
            onDismiss = { showSearchDialog = false },
            onSearch = { query ->
                viewModel.onSearchQueryChange(query)
                showSearchDialog = false
            }
        )
    }

    // 权限请求对话框
    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = { viewModel.dismissPermissionDialog() },
            onConfirm = {
                permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
            }
        )
    }

    // 文件夹管理对话框
    if (showFolderManager) {
        FolderManagerDialog(
            folders = scanFolders,
            onDismiss = { viewModel.dismissFolderManager() },
            onAddFolder = {
                folderPickerLauncher.launch(null)
            },
            onRemoveFolder = { folder ->
                viewModel.removeScanFolder(folder)
            }
        )
    }

    // 操作菜单
    if (showActionSheet && selectedSong != null) {
        SongActionSheet(
            song = selectedSong!!,
            onDismiss = viewModel::dismissActionSheet,
            onPlayNext = {
                playerViewModel.addToQueueNext(selectedSong!!)
                viewModel.dismissActionSheet()
            },
            onAddToPlaylist = {
                playlistViewModel.showAddToPlaylistDialog(selectedSong!!)
                viewModel.dismissActionSheet()
            },
            onAddTag = {
                // 先显示标签选择对话框，不要先 dismissActionSheet
                showTagSelection = true
            },
            onViewArtist = {
                // TODO: 导航到艺术家详情
                viewModel.dismissActionSheet()
            },
            onViewAlbum = {
                // TODO: 导航到专辑详情
                viewModel.dismissActionSheet()
            },
            onDelete = {
                viewModel.deleteSong(selectedSong!!)
            }
        )
    }

    // 添加到歌单对话框
    val showAddToPlaylistDialog by playlistViewModel.showAddToPlaylistDialog.collectAsState()
    val songToAdd by playlistViewModel.songToAdd.collectAsState()

    if (showAddToPlaylistDialog && songToAdd != null) {
        com.tagplayer.musicplayer.ui.playlist.screen.AddToPlaylistDialog(
            songTitle = songToAdd!!.title,
            onDismiss = playlistViewModel::dismissAddToPlaylistDialog
        )
    }

    // 标签选择对话框
    if (showTagSelection && selectedSong != null) {
        TagSelectionDialog(
            song = selectedSong!!,
            onDismiss = {
                showTagSelection = false
                viewModel.dismissActionSheet()
                viewModel.clearSelectedSong()
            },
            viewModel = tagViewModel
        )
    }

    // 排序对话框
    if (showSortDialog) {
        SortDialog(
            currentSortType = sortType,
            onDismiss = { viewModel.dismissSortDialog() },
            onSortSelected = { viewModel.onSortTypeChange(it) }
        )
    }
}

@Composable
private fun SearchDialog(
    initialQuery: String,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索歌曲、艺术家、专辑...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSearch(query) }) {
                Text("搜索")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SortDialog(
    currentSortType: SortType,
    onDismiss: () -> Unit,
    onSortSelected: (SortType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序方式") },
        text = {
            Column {
                SortType.values().forEach { sortType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSortSelected(sortType)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortType == currentSortType,
                            onClick = {
                                onSortSelected(sortType)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sortType.title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EmptyState(
    isSearching: Boolean,
    hasPermission: Boolean,
    onScanClick: () -> Unit,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (hasPermission) Icons.Default.Search else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Text(
                text = when {
                    isSearching -> "未找到匹配的歌曲"
                    !hasPermission -> "需要权限才能扫描音乐"
                    else -> "暂无歌曲"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = when {
                    isSearching -> ""
                    !hasPermission -> "请点击下方按钮授予权限"
                    else -> "点击下方按钮扫描本地音乐"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            if (!isSearching) {
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .clickable(onClick = if (hasPermission) onScanClick else onGrantPermission),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (hasPermission) Icons.Default.Refresh else Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = if (hasPermission) "扫描本地音乐" else "授予权限",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("需要权限") },
        text = {
            Text(
                "扫描本地音乐需要访问音频文件的权限。\n\n" +
                "授权后您可以：\n" +
                "• 全盘扫描所有音乐\n" +
                "• 添加指定文件夹扫描"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("授权")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FolderManagerDialog(
    folders: List<ScanFolder>,
    onDismiss: () -> Unit,
    onAddFolder: () -> Unit,
    onRemoveFolder: (ScanFolder) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("扫描文件夹") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (folders.isEmpty()) {
                    Text(
                        text = "未添加扫描文件夹，点击添加按钮选择音乐文件夹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "已添加 ${folders.size} 个文件夹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    folders.forEach { folder ->
                        FolderItem(
                            folder = folder,
                            onRemove = { onRemoveFolder(folder) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onAddFolder) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加文件夹")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun FolderItem(
    folder: ScanFolder,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = folder.getDisplayName(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
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