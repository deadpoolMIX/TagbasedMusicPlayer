package com.tagplayer.musicplayer.ui.artist.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.ui.artist.viewmodel.ArtistDetailViewModel
import com.tagplayer.musicplayer.ui.components.SongItem
import com.tagplayer.musicplayer.ui.components.TagSelectionDialog
import com.tagplayer.musicplayer.ui.player.viewmodel.PlayerViewModel
import com.tagplayer.musicplayer.ui.tags.viewmodel.TagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    tagViewModel: TagViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedSongs by viewModel.selectedSongs.collectAsState()

    // 当前播放歌曲ID
    val playbackState by playerViewModel.playbackState.collectAsState()
    val currentPlayingSongId = playbackState.currentSongId

    // 批量添加标签对话框状态
    var showBatchTagSelection by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isMultiSelectMode) "已选择 ${selectedSongs.size} 首" else artistName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isMultiSelectMode) {
                            Text(
                                text = "${songs.size} 首歌曲",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        // 播放按钮 - 播放该艺术家的所有歌曲
                        if (songs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    playerViewModel.setQueue(songs, 0)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "播放全部",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
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
                    onCancel = { viewModel.exitMultiSelectMode() }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        // 歌曲列表
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
                            playerViewModel.setQueue(songs, songs.indexOf(song))
                        }
                    },
                    onLongClick = {
                        if (!isMultiSelectMode) {
                            viewModel.enterMultiSelectMode(song)
                        }
                    },
                    onMoreClick = {
                        // TODO: 显示操作菜单
                    }
                )
            }
        }
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
}

/**
 * 多选模式底部操作栏
 */
@Composable
private fun MultiSelectBottomBar(
    selectedCount: Int,
    onAddTags: () -> Unit,
    onCancel: () -> Unit
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
                    imageVector = Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加标签")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 取消按钮
            androidx.compose.material3.Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("取消")
            }
        }
    }
}