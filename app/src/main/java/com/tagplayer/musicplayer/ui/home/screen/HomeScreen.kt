package com.tagplayer.musicplayer.ui.home.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PlayArrow
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
    onNavigateToSearch: () -> Unit = {},
    scrollToCurrentSongRequest: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    tagViewModel: TagViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val groupedSongs by viewModel.groupedSongs.collectAsState()
    val letterToIndexMap by viewModel.letterToIndexMap.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedSong by viewModel.selectedSong.collectAsState()
    val showActionSheet by viewModel.showActionSheet.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val showFolderManager by viewModel.showFolderManager.collectAsState()
    val showSortDialog by viewModel.showSortDialog.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val scanFolders by viewModel.scanFolders.collectAsState()

    // 多选模式状态
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedSongs by viewModel.selectedSongs.collectAsState()

    // 当前播放歌曲ID
    val playbackState by playerViewModel.playbackState.collectAsState()
    val currentPlayingSongId = playbackState.currentSongId

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

    // 滚动到当前播放歌曲
    LaunchedEffect(scrollToCurrentSongRequest) {
        if (scrollToCurrentSongRequest > 0 && currentPlayingSongId != null) {
            if (isTitleSortMode) {
                // 标题排序模式：需要在分组列表中查找
                var targetIndex = 0
                var found = false
                for ((letter, songList) in groupedSongs) {
                    // 加上分组标题
                    targetIndex++
                    for (song in songList) {
                        if (song.id == currentPlayingSongId) {
                            found = true
                            break
                        }
                        targetIndex++
                    }
                    if (found) break
                }
                if (found) {
                    listState.animateScrollToItem(targetIndex)
                }
            } else {
                // 其他模式：普通列表
                val index = songs.indexOfFirst { it.id == currentPlayingSongId }
                if (index >= 0) {
                    listState.animateScrollToItem(index)
                }
            }
        }
    }

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

    // 删除文件权限请求启动器 (Android 11+)
    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 用户授权，继续删除
            viewModel.onDeletePermissionGranted()
        } else {
            // 用户拒绝，取消删除
            viewModel.cancelDeletePermissionRequest()
        }
    }

    // 删除文件权限请求状态
    val deletePermissionIntentSender by viewModel.deletePermissionIntentSender.collectAsState()
    val songPendingDelete by viewModel.songPendingDelete.collectAsState()

    // 当有删除权限请求时，启动它
    LaunchedEffect(deletePermissionIntentSender) {
        deletePermissionIntentSender?.let { intentSender ->
            try {
                val request = IntentSenderRequest.Builder(intentSender).build()
                deletePermissionLauncher.launch(request)
                viewModel.clearDeletePermissionIntentSender()
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.cancelDeletePermissionRequest()
            }
        }
    }

    // 标签选择对话框状态
    var showTagSelection by remember { mutableStateOf(false) }

    // 批量添加标签对话框状态
    var showBatchTagSelection by remember { mutableStateOf(false) }

    // 批量删除确认对话框状态
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    // 首次启动检查权限（只更新状态，不自动申请）
    LaunchedEffect(Unit) {
        viewModel.updatePermissionState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isMultiSelectMode) "已选择 ${selectedSongs.size} 首"
                               else "我的音乐",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    if (isMultiSelectMode) {
                        // 全选按钮
                        IconButton(onClick = { viewModel.selectAllSongs(songs) }) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "全选"
                            )
                        }
                        // 退出多选模式按钮
                        IconButton(onClick = { viewModel.exitMultiSelectMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "取消"
                            )
                        }
                    } else {
                        // 搜索按钮
                        IconButton(onClick = onNavigateToSearch) {
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
                        // 随机播放按钮
                        IconButton(onClick = { playerViewModel.playRandom(songs) }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "随机播放"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (isMultiSelectMode) {
                MultiSelectBottomBar(
                    selectedCount = selectedSongs.size,
                    onAddTags = { showBatchTagSelection = true },
                    onDelete = { showBatchDeleteConfirm = true },
                    onCancel = { viewModel.exitMultiSelectMode() }
                )
            }
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
            }

            // 歌曲列表
            if (songs.isEmpty()) {
                EmptyState(
                    isSearching = false,
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
                                val isSelected = song in selectedSongs
                                val isPlaying = song.id == currentPlayingSongId
                                SongItem(
                                    song = song,
                                    isSelected = isSelected,
                                    isMultiSelectMode = isMultiSelectMode,
                                    isPlaying = isPlaying,
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            viewModel.toggleSongSelection(song)
                                        } else {
                                            playerViewModel.setQueue(songs, songs.indexOf(song))
                                        }
                                    },
                                    onLongClick = {
                                        if (!isMultiSelectMode) {
                                            viewModel.enterMultiSelectMode(song)
                                        }
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
                        val isSelected = song in selectedSongs
                        val isPlaying = song.id == currentPlayingSongId
                        SongItem(
                            song = song,
                            isSelected = isSelected,
                            isMultiSelectMode = isMultiSelectMode,
                            isPlaying = isPlaying,
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSongSelection(song)
                                } else {
                                    // 播放选中的歌曲
                                    playerViewModel.setQueue(songs, songs.indexOf(song))
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    viewModel.enterMultiSelectMode(song)
                                }
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
            onDelete = { deleteFile ->
                viewModel.deleteSong(selectedSong!!, deleteFile)
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

    // 批量添加标签对话框
    if (showBatchTagSelection && selectedSongs.isNotEmpty()) {
        TagSelectionDialog(
            songs = selectedSongs.toList(),
            onDismiss = {
                showBatchTagSelection = false
                viewModel.exitMultiSelectMode()
            },
            viewModel = tagViewModel
        )
    }

    // 批量删除确认对话框
    if (showBatchDeleteConfirm) {
        BatchDeleteConfirmDialog(
            count = selectedSongs.size,
            onDismiss = { showBatchDeleteConfirm = false },
            onConfirm = { deleteFiles ->
                viewModel.deleteSelectedSongs(deleteFiles)
                showBatchDeleteConfirm = false
            }
        )
    }
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

/**
 * 多选模式底部操作栏
 */
@Composable
private fun MultiSelectBottomBar(
    selectedCount: Int,
    onAddTags: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 添加标签按钮
            androidx.compose.material3.OutlinedButton(
                onClick = onAddTags,
                enabled = selectedCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加标签")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 删除按钮
            androidx.compose.material3.Button(
                onClick = onDelete,
                enabled = selectedCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("删除")
            }
        }
    }
}

/**
 * 批量删除确认对话框
 */
@Composable
private fun BatchDeleteConfirmDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var deleteFiles by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除歌曲") },
        text = {
            Column {
                Text(
                    text = "确定要删除选中的 $count 首歌曲吗？",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deleteFiles = !deleteFiles }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = deleteFiles,
                        onCheckedChange = { deleteFiles = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "同时删除本地文件",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(deleteFiles) }
            ) {
                Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}