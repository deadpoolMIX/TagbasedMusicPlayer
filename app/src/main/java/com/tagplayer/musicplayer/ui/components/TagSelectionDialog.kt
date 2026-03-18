package com.tagplayer.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.local.entity.Tag
import com.tagplayer.musicplayer.ui.tags.viewmodel.TagViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelectionDialog(
    song: Song,
    onDismiss: () -> Unit,
    viewModel: TagViewModel = hiltViewModel()
) {
    TagSelectionDialog(
        songs = listOf(song),
        onDismiss = onDismiss,
        viewModel = viewModel
    )
}

/**
 * 标签选择弹窗（筛选页面专用）
 * 支持多选，确认后批量添加标签
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPickerDialog(
    title: String = "选择标签",
    excludeTagIds: List<Long> = emptyList(),
    onTagsSelected: (List<Tag>) -> Unit,
    onDismiss: () -> Unit,
    viewModel: TagViewModel = hiltViewModel()
) {
    val allTags by viewModel.allTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredTags by viewModel.filteredTags.collectAsState()
    val focusManager = LocalFocusManager.current

    // 多选状态
    val selectedTags = remember { mutableStateListOf<Tag>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // 已选标签预览
                if (selectedTags.isNotEmpty()) {
                    Text(
                        text = "已选 ${selectedTags.size} 个",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        selectedTags.forEach { tag ->
                            SelectedTagChip(
                                tag = tag,
                                onRemove = { selectedTags.remove(tag) }
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }

                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("搜索标签...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 可用标签列表（排除已选标签）
                val availableTags = if (searchQuery.isBlank()) {
                    allTags.filter { tag -> tag.id !in excludeTagIds && selectedTags.none { it.id == tag.id } }
                } else {
                    filteredTags.filter { tag -> tag.id !in excludeTagIds && selectedTags.none { it.id == tag.id } }
                }

                if (availableTags.isEmpty() && selectedTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "暂无可用标签" else "未找到匹配标签",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    if (availableTags.isNotEmpty()) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "匹配结果" else "可选标签",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 200.dp)
                        ) {
                            items(availableTags, key = { it.id }) { tag ->
                                TagSelectableItem(
                                    tag = tag,
                                    isSelected = false,
                                    onClick = {
                                        selectedTags.add(tag)
                                        viewModel.onSearchQueryChange("")
                                        focusManager.clearFocus()
                                    }
                                )
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
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            if (selectedTags.isNotEmpty()) {
                                onTagsSelected(selectedTags.toList())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = selectedTags.isNotEmpty()
                    ) {
                        Text(if (selectedTags.isEmpty()) "确认" else "确认(${selectedTags.size})")
                    }
                }
            }
        }
    }
}

@Composable
private fun TagSelectableItem(
    tag: Tag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelectionDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    viewModel: TagViewModel = hiltViewModel()
) {
    val isBatchMode = songs.size > 1
    val song = songs.firstOrNull()

    val allTags by viewModel.allTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredTags by viewModel.filteredTags.collectAsState()

    // 单首歌曲模式：从数据库读取当前歌曲的标签
    val songTags by if (isBatchMode) {
        remember { mutableStateOf(emptyList<Tag>()) }
    } else if (song != null) {
        viewModel.getSongTagsFlow(song.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<Tag>()) }
    }

    // 批量模式：维护本次会话已添加的标签列表
    val addedTags = remember { mutableStateListOf<Tag>() }

    // 用于显示的"已选标签"（批量模式用 addedTags，单首模式用 songTags）
    val displayedTags = if (isBatchMode) addedTags.toList() else songTags

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBatchMode) "批量添加标签 (${songs.size}首)" else "管理标签",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // 当前标签 / 已添加标签
                if (displayedTags.isNotEmpty()) {
                    Text(
                        text = if (isBatchMode) "已添加标签" else "当前标签",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayedTags.forEach { tag ->
                            SelectedTagChip(
                                tag = tag,
                                onRemove = {
                                    if (isBatchMode) {
                                        // 批量模式：从所有歌曲移除标签，并从 addedTags 中移除
                                        viewModel.removeTagFromSongs(tag.id, songs.map { it.id })
                                        addedTags.remove(tag)
                                    } else {
                                        // 单首歌曲模式
                                        song?.let { viewModel.removeTagFromSong(it.id, tag.id) }
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("搜索标签...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 创建新标签选项（当有搜索词时）
                val trimmedQuery = searchQuery.trim()
                if (trimmedQuery.isNotBlank() && allTags.none { it.name.equals(trimmedQuery, ignoreCase = true) }) {
                    CreateTagRow(
                        name = trimmedQuery,
                        onClick = {
                            if (isBatchMode) {
                                // 批量模式：创建标签并添加到所有歌曲
                                viewModel.createTagAndAddToSongs(songs.map { it.id }, trimmedQuery) { newTag ->
                                    // 回调：将新标签加入 addedTags
                                    addedTags.add(newTag)
                                }
                            } else {
                                // 单首歌曲模式
                                song?.let { viewModel.createTagAndAddToSong(it.id, trimmedQuery) }
                            }
                            // 清除搜索词并关闭键盘
                            viewModel.onSearchQueryChange("")
                            focusManager.clearFocus()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 标签列表（过滤掉已显示的标签）
                val availableTags = if (searchQuery.isBlank()) {
                    allTags.filter { tag -> displayedTags.none { it.id == tag.id } }
                } else {
                    filteredTags.filter { tag -> displayedTags.none { it.id == tag.id } }
                }

                if (availableTags.isNotEmpty()) {
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "匹配结果",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } else {
                        Text(
                            text = "常用标签",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 200.dp)
                    ) {
                        items(availableTags, key = { it.id }) { tag ->
                            TagListItem(
                                tag = tag,
                                onClick = {
                                    if (isBatchMode) {
                                        // 批量模式：添加标签到所有歌曲，并记录到 addedTags
                                        viewModel.addTagToSongs(tag.id, songs.map { it.id })
                                        addedTags.add(tag)
                                    } else {
                                        // 单首歌曲模式
                                        song?.let { viewModel.addTagToSong(it.id, tag.id) }
                                    }
                                    // 清除搜索词并关闭键盘
                                    viewModel.onSearchQueryChange("")
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }
                } else if (searchQuery.isNotBlank() && trimmedQuery.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "无匹配标签",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("确认保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedTagChip(
    tag: Tag,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable(onClick = onRemove)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "# ${tag.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "移除",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CreateTagRow(
    name: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "创建新标签",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "\"$name\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TagListItem(
    tag: Tag,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
