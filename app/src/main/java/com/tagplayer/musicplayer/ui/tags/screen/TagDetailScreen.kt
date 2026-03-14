package com.tagplayer.musicplayer.ui.tags.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.LocalOffer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.local.entity.Tag
import com.tagplayer.musicplayer.ui.components.SongActionSheet
import com.tagplayer.musicplayer.ui.components.SongItem
import com.tagplayer.musicplayer.ui.components.TagSelectionDialog
import com.tagplayer.musicplayer.ui.player.viewmodel.PlayerViewModel
import com.tagplayer.musicplayer.ui.tags.viewmodel.TagViewModel
import com.tagplayer.musicplayer.ui.theme.AppDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailScreen(
    tagId: Long,
    onBackClick: () -> Unit,
    onAddSongsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val allTags by viewModel.allTags.collectAsState()
    val tag = allTags.find { it.id == tagId }
    val tagSongs by viewModel.selectedTagSongs.collectAsState()

    // 操作菜单状态
    var showActionSheet by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    // 标签选择对话框状态
    var showTagSelection by remember { mutableStateOf(false) }

    // 加载标签
    LaunchedEffect(tagId, allTags) {
        val targetTag = allTags.find { it.id == tagId }
        if (targetTag != null) {
            viewModel.selectTag(targetTag)
        }
    }

    // 页面离开时清除选中标签
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedTag()
        }
    }

    if (tag == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("标签不存在")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = tag.name,
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
            // 标签信息卡片
            TagHeader(
                tag = tag,
                songCount = tagSongs.size,
                onPlayAll = {
                    if (tagSongs.isNotEmpty()) {
                        playerViewModel.setQueue(tagSongs, 0)
                    }
                }
            )

            // 歌曲列表
            if (tagSongs.isEmpty()) {
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
                            text = "暂无歌曲",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "在歌曲的\"更多\"菜单中添加此标签",
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
                        items = tagSongs,
                        key = { it.id }
                    ) { song ->
                        SongItem(
                            song = song,
                            onClick = {
                                playerViewModel.setQueue(tagSongs, tagSongs.indexOf(song))
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
                // TODO: 显示添加到歌单对话框
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
                // 从标签中移除
                viewModel.removeTagFromSong(selectedSong!!.id, tagId)
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
            }
        )
    }
}

@Composable
private fun TagHeader(
    tag: Tag,
    songCount: Int,
    onPlayAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.HeaderHorizontalPadding, vertical = AppDimensions.HeaderVerticalPadding),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.HeaderInternalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标签图标
            Box(
                modifier = Modifier
                    .size(AppDimensions.HeaderIconSize)
                    .clip(RoundedCornerShape(AppDimensions.HeaderIconCornerRadius))
                    .background(
                        tag.color?.let {
                            androidx.compose.ui.graphics.Color(it)
                        } ?: MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimensions.HeaderIconInternalSize),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 标签信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$songCount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
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
                    modifier = Modifier.size(AppDimensions.HeaderPlayButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放全部",
                        modifier = Modifier.size(AppDimensions.HeaderPlayIconSize),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
