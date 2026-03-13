package com.tagplayer.musicplayer.ui.playlist.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.local.entity.Playlist
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.ui.components.SongActionSheet
import com.tagplayer.musicplayer.ui.components.SongItem
import com.tagplayer.musicplayer.ui.components.TagSelectionDialog
import com.tagplayer.musicplayer.ui.player.viewmodel.PlayerViewModel
import com.tagplayer.musicplayer.ui.playlist.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBackClick: () -> Unit,
    onAddSongsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    tagViewModel: com.tagplayer.musicplayer.ui.tags.viewmodel.TagViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    val playlist = playlists.find { it.id == playlistId }
    val playlistSongs by viewModel.playlistSongs.collectAsState()

    // 操作菜单状态
    var showActionSheet by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    // 标签选择对话框状态
    var showTagSelection by remember { mutableStateOf(false) }

    // 加载歌单和歌曲
    androidx.compose.runtime.LaunchedEffect(playlistId) {
        val targetPlaylist = playlists.find { it.id == playlistId }
        if (targetPlaylist != null) {
            viewModel.selectPlaylist(targetPlaylist)
        }
    }

    // 监听歌单变化，更新选中的歌单
    androidx.compose.runtime.LaunchedEffect(playlists, playlistId) {
        val targetPlaylist = playlists.find { it.id == playlistId }
        if (targetPlaylist != null && targetPlaylist.id != viewModel.selectedPlaylist.value?.id) {
            viewModel.selectPlaylist(targetPlaylist)
        }
    }

    if (playlist == null) {
        // 歌单不存在，显示错误
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("歌单不存在")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlist.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    // 添加歌曲按钮
                    IconButton(onClick = onAddSongsClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加歌曲",
                            tint = MaterialTheme.colorScheme.onSurface
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 歌单信息卡片
            PlaylistHeader(
                playlist = playlist,
                songCount = playlistSongs.size,
                onPlayAll = {
                    if (playlistSongs.isNotEmpty()) {
                        playerViewModel.setQueue(playlistSongs, 0)
                    }
                }
            )

            // 歌曲列表
            if (playlistSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "歌单为空",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "点击歌曲的\"更多\"按钮添加到歌单",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = playlistSongs,
                        key = { it.id }
                    ) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                playerViewModel.setQueue(playlistSongs, playlistSongs.indexOf(song))
                            },
                            onMoreClick = {
                                selectedSong = song
                                showActionSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 操作菜单
    if (showActionSheet && selectedSong != null) {
        SongActionSheet(
            song = selectedSong!!,
            onDismiss = {
                showActionSheet = false
                selectedSong = null
            },
            onPlayNext = {
                playerViewModel.addToQueueNext(selectedSong!!)
                showActionSheet = false
                selectedSong = null
            },
            onAddToPlaylist = {
                // 已在歌单中，可以切换到其他歌单
                viewModel.showAddToPlaylistDialog(selectedSong!!)
                showActionSheet = false
                selectedSong = null
            },
            onAddTag = {
                showTagSelection = true
                showActionSheet = false
            },
            onViewArtist = {
                // TODO: 导航到艺术家详情
                showActionSheet = false
                selectedSong = null
            },
            onViewAlbum = {
                // TODO: 导航到专辑详情
                showActionSheet = false
                selectedSong = null
            },
            onDelete = {
                // 从歌单中移除
                viewModel.removeSongFromPlaylist(playlist.id, selectedSong!!)
                showActionSheet = false
                selectedSong = null
            }
        )
    }

    // 标签选择对话框
    if (showTagSelection && selectedSong != null) {
        TagSelectionDialog(
            song = selectedSong!!,
            onDismiss = {
                showTagSelection = false
                selectedSong = null
            },
            viewModel = tagViewModel
        )
    }
}

@Composable
private fun PlaylistHeader(
    playlist: Playlist,
    songCount: Int,
    onPlayAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面占位 - 缩小尺寸
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 歌单信息 - 水平排列，更紧凑
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$songCount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "首歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 播放全部按钮
            if (songCount > 0) {
                IconButton(
                    onClick = onPlayAll,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放全部",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
