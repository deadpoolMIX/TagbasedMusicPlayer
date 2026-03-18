package com.tagplayer.musicplayer.ui.filter.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.local.entity.Tag
import com.tagplayer.musicplayer.ui.components.SongItem
import com.tagplayer.musicplayer.ui.components.TagPickerDialog
import com.tagplayer.musicplayer.ui.filter.viewmodel.FilterBox
import com.tagplayer.musicplayer.ui.filter.viewmodel.FilterState
import com.tagplayer.musicplayer.ui.filter.viewmodel.FilterViewModel
import com.tagplayer.musicplayer.ui.player.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val filterState by viewModel.filterState.collectAsState()

    // 当前播放歌曲ID
    val playbackState by playerViewModel.playbackState.collectAsState()
    val currentPlayingSongId = playbackState.currentSongId

    var showTagSelector by remember { mutableStateOf<FilterBox?>(null) }
    var showSavePlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "筛选",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    if (filterState.filteredSongs.isNotEmpty()) {
                        IconButton(onClick = { showSavePlaylistDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "保存为歌单"
                            )
                        }
                    }
                    if (hasActiveFilters(filterState)) {
                        TextButton(onClick = { viewModel.clearAllFilters() }) {
                            Text("清除")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 筛选条件区域
            item {
                // 逻辑公式展示
                FilterFormula()

                Spacer(modifier = Modifier.height(16.dp))

                // 框A - 包含标签
                FilterBoxSection(
                    title = "框 A",
                    subtitle = "必须包含以下所有标签",
                    tags = filterState.boxATags,
                    onAddClick = { showTagSelector = FilterBox.A },
                    onRemoveTag = { tag -> viewModel.removeTagFromBox(tag, FilterBox.A) },
                    onClear = { viewModel.clearBox(FilterBox.A) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 并集符号
                OperatorSymbol("+")

                Spacer(modifier = Modifier.height(8.dp))

                // 框B - 可选标签
                FilterBoxSection(
                    title = "框 B",
                    subtitle = "可选，满足框B任意标签的歌曲也会加入结果",
                    tags = filterState.boxBTags,
                    onAddClick = { showTagSelector = FilterBox.B },
                    onRemoveTag = { tag -> viewModel.removeTagFromBox(tag, FilterBox.B) },
                    onClear = { viewModel.clearBox(FilterBox.B) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 差集符号
                OperatorSymbol("-")

                Spacer(modifier = Modifier.height(8.dp))

                // 框C - 排除标签
                FilterBoxSection(
                    title = "框 C",
                    subtitle = "排除包含以下任意标签的歌曲",
                    tags = filterState.boxCTags,
                    onAddClick = { showTagSelector = FilterBox.C },
                    onRemoveTag = { tag -> viewModel.removeTagFromBox(tag, FilterBox.C) },
                    onClear = { viewModel.clearBox(FilterBox.C) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))

                // 结果统计
                FilterResultHeader(
                    count = filterState.filteredSongs.size,
                    hasActiveFilters = hasActiveFilters(filterState),
                    onPlayAll = {
                        if (filterState.filteredSongs.isNotEmpty()) {
                            playerViewModel.setQueue(filterState.filteredSongs, 0)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 歌曲列表
            if (filterState.filteredSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyFilterState(hasActiveFilters(filterState))
                    }
                }
            } else {
                items(
                    items = filterState.filteredSongs,
                    key = { it.id }
                ) { song ->
                    val isPlaying = song.id == currentPlayingSongId
                    SongItem(
                        song = song,
                        isPlaying = isPlaying,
                        onClick = {
                            // 使用 ID 查找索引，避免对象比较问题
                            val index = filterState.filteredSongs.indexOfFirst { it.id == song.id }
                            if (index >= 0) {
                                playerViewModel.setQueue(filterState.filteredSongs, index)
                            }
                        },
                        onMoreClick = {
                            // TODO: 显示操作菜单
                        }
                    )
                }
            }
        }
    }

    // 标签选择器弹窗 - 复用 TagPickerDialog 组件
    showTagSelector?.let { box ->
        val excludeTagIds = when (box) {
            FilterBox.A -> filterState.boxATags.map { it.id }
            FilterBox.B -> filterState.boxBTags.map { it.id }
            FilterBox.C -> filterState.boxCTags.map { it.id }
        }

        TagPickerDialog(
            title = when (box) {
                FilterBox.A -> "添加到框A"
                FilterBox.B -> "添加到框B"
                FilterBox.C -> "添加到框C"
            },
            excludeTagIds = excludeTagIds,
            onDismiss = { showTagSelector = null },
            onTagSelected = { tag ->
                viewModel.addTagToBox(tag, box)
                showTagSelector = null
            }
        )
    }

    // 保存为歌单弹窗
    if (showSavePlaylistDialog) {
        SavePlaylistDialog(
            songCount = filterState.filteredSongs.size,
            onDismiss = { showSavePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.saveAsPlaylist(name) { success ->
                    showSavePlaylistDialog = false
                    // TODO: 显示成功/失败提示
                }
            }
        )
    }
}

@Composable
private fun FilterFormula() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "筛选逻辑",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "(A ∩ A2 ∩ ...) ∪ (B ∪ B2 ∪ ...) - C",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "框A交集 ∪ 框B并集 - 框C",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBoxSection(
    title: String,
    subtitle: String,
    tags: List<Tag>,
    onAddClick: () -> Unit,
    onRemoveTag: (Tag) -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (tags.isNotEmpty()) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("清除", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 标签列表
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 添加按钮
                Surface(
                    modifier = Modifier.clickable(onClick = onAddClick),
                    shape = RoundedCornerShape(9999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "添加标签",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // 已选标签
                tags.forEach { tag ->
                    FilterTagChip(
                        tag = tag,
                        onRemove = { onRemoveTag(tag) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTagChip(
    tag: Tag,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = tag.color?.let { androidx.compose.ui.graphics.Color(it) }
            ?: MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "# ${tag.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = tag.color?.let {
                    val hsl = FloatArray(3)
                    ColorUtils.colorToHSL(it, hsl)
                    if (hsl[2] > 0.5f) androidx.compose.ui.graphics.Color.Black
                    else androidx.compose.ui.graphics.Color.White
                } ?: MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "移除",
                modifier = Modifier.size(14.dp),
                tint = tag.color?.let {
                    val hsl = FloatArray(3)
                    ColorUtils.colorToHSL(it, hsl)
                    if (hsl[2] > 0.5f) androidx.compose.ui.graphics.Color.Black
                    else androidx.compose.ui.graphics.Color.White
                } ?: MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun OperatorSymbol(symbol: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FilterResultHeader(
    count: Int,
    hasActiveFilters: Boolean,
    onPlayAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (hasActiveFilters) "筛选结果" else "全部歌曲",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count 首歌曲",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (count > 0) {
            Button(
                onClick = onPlayAll,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("播放全部")
            }
        }
    }
}

@Composable
private fun EmptyFilterState(hasActiveFilters: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.RemoveCircleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        if (hasActiveFilters) {
            Text(
                text = "没有找到匹配的歌曲",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "尝试调整筛选条件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "曲库为空",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "请先扫描添加歌曲",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SavePlaylistDialog(
    songCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存为歌单") },
        text = {
            Column {
                Text(
                    text = "将 $songCount 首歌曲保存为新歌单",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("歌单名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun hasActiveFilters(state: FilterState): Boolean {
    return state.boxATags.isNotEmpty() || state.boxBTags.isNotEmpty() || state.boxCTags.isNotEmpty()
}
