package com.tagplayer.musicplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tagplayer.musicplayer.data.local.entity.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionSheet(
    song: Song,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddTag: () -> Unit,
    onViewArtist: () -> Unit,
    onViewAlbum: () -> Unit,
    onDelete: (deleteFile: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 确保弹窗展开到完整高度
    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            songTitle = song.title,
            onDismiss = { showDeleteDialog = false },
            onDeleteFromListOnly = {
                showDeleteDialog = false
                onDelete(false)
            },
            onDeleteWithFile = {
                showDeleteDialog = false
                onDelete(true)
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 使用垂直滚动确保内容可以滚动显示
                .verticalScroll(rememberScrollState())
        ) {
            // Song Info Header
            ListItem(
                headlineContent = {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                supportingContent = {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            HorizontalDivider()

            // Actions
            ActionItem(
                icon = Icons.Default.SkipNext,
                text = "下一首播放",
                onClick = onPlayNext
            )

            ActionItem(
                icon = Icons.Default.PlaylistAdd,
                text = "收藏到歌单",
                onClick = onAddToPlaylist
            )

            ActionItem(
                icon = Icons.Default.Label,
                text = "添加/编辑标签",
                onClick = onAddTag
            )

            ActionItem(
                icon = Icons.Default.Person,
                text = "歌手：${song.artist}",
                onClick = onViewArtist
            )

            ActionItem(
                icon = Icons.Default.Album,
                text = "专辑：${song.album}",
                onClick = onViewAlbum
            )

            HorizontalDivider()

            // Delete Action (Red)
            ListItem(
                headlineContent = {
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable(onClick = { showDeleteDialog = true }),
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 底部留出一些空间
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = text,
                maxLines = 1
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * 删除确认对话框
 */
@Composable
private fun DeleteConfirmDialog(
    songTitle: String,
    onDismiss: () -> Unit,
    onDeleteFromListOnly: () -> Unit,
    onDeleteWithFile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "删除歌曲")
        },
        text = {
            Text(text = "「$songTitle」\n\n请选择删除方式：")
        },
        confirmButton = {
            TextButton(onClick = onDeleteWithFile) {
                Text(
                    text = "同时删除本地文件",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onDeleteFromListOnly) {
                    Text("仅从列表移除")
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}
