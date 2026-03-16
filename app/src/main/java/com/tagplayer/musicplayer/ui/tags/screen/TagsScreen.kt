package com.tagplayer.musicplayer.ui.tags.screen

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tagplayer.musicplayer.data.local.entity.Tag
import com.tagplayer.musicplayer.ui.tags.viewmodel.TagViewModel
import com.tagplayer.musicplayer.ui.theme.AppDimensions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    modifier: Modifier = Modifier,
    viewModel: TagViewModel = hiltViewModel(),
    onTagClick: (Tag) -> Unit = {}
) {
    val tags by viewModel.filteredTags.collectAsState()
    val tagCount by viewModel.tagCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val tagNameInput by viewModel.tagNameInput.collectAsState()
    val focusManager = LocalFocusManager.current

    // 搜索对话框状态
    var showSearchDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "标签管理",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                    // 搜索按钮
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // 添加标签按钮
                    IconButton(onClick = { viewModel.showCreateDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "创建标签",
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
            // 标签数量
            Text(
                text = "$tagCount 个标签",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )

            // 标签列表
            if (tags.isEmpty()) {
                EmptyTagsState()
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            // 点击列表空白区域时清除焦点
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClickLabel = null,
                                onClick = { focusManager.clearFocus() }
                            ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 32.dp, // 为滚动条留出空间
                            top = 16.dp,
                            bottom = 16.dp
                        )
                    ) {
                        items(
                            items = tags,
                            key = { it.id }
                        ) { tag ->
                            TagItem(
                                tag = tag,
                                viewModel = viewModel,
                                onClick = {
                                    // 先清除焦点，再触发点击事件
                                    focusManager.clearFocus()
                                    // 如果有搜索词，清除搜索词
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.onSearchQueryChange("")
                                    }
                                    onTagClick(tag)
                                },
                                onEdit = {
                                    focusManager.clearFocus()
                                    viewModel.showEditDialog(tag)
                                },
                                onDelete = {
                                    focusManager.clearFocus()
                                    viewModel.showDeleteDialog(tag)
                                }
                            )
                        }
                    }

                    // 快速滚动条
                    VerticalScrollbar(
                        listState = listState,
                        itemCount = tags.size,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
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

    // 创建标签对话框
    if (showCreateDialog) {
        TagDialog(
            title = "创建标签",
            name = tagNameInput,
            onNameChange = viewModel::onTagNameInputChange,
            onDismiss = viewModel::dismissCreateDialog,
            onConfirm = viewModel::createTag
        )
    }

    // 编辑标签对话框
    if (showEditDialog) {
        TagDialog(
            title = "编辑标签",
            name = tagNameInput,
            onNameChange = viewModel::onTagNameInputChange,
            onDismiss = viewModel::dismissEditDialog,
            onConfirm = viewModel::updateTag
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("删除标签") },
            text = { Text("确定要删除这个标签吗？该标签将从所有歌曲中移除。") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteTag,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("取消")
                }
            }
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
        title = { Text("搜索标签") },
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索标签...") },
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
private fun TagItem(
    tag: Tag,
    viewModel: TagViewModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val songCount by viewModel.getTagSongCount(tag.id).collectAsState(initial = 0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClickLabel = "查看标签详情",
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.ListItemPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标+名称+数量
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标签图标
                Box(
                    modifier = Modifier
                        .size(AppDimensions.ListItemIconSize)
                        .clip(RoundedCornerShape(AppDimensions.ListItemIconCornerRadius))
                        .background(
                            androidx.compose.ui.graphics.Color(
                                tag.color ?: MaterialTheme.colorScheme.primaryContainer.hashCode()
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.ListItemIconInternalSize),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(AppDimensions.ListItemSpacing))

                // 标签名称
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 歌曲数量
                Text(
                    text = "$songCount 首",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 编辑按钮
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(AppDimensions.ListItemActionButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(AppDimensions.ListItemActionIconSize)
                )
            }

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(AppDimensions.ListItemActionButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(AppDimensions.ListItemActionIconSize)
                )
            }
        }
    }
}

@Composable
private fun EmptyTagsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Text(
                text = "暂无标签",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "点击右上角按钮创建标签",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TagDialog(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("标签名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.trim().isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 垂直滚动条组件（支持拖拽）
 *
 * 关键设计：
 * 1. 基于索引的精确进度计算（含小数部分）
 * 2. 状态分离：拖拽时滑块位置由手势驱动，否则由列表状态驱动
 * 3. 切断死循环：拖拽期间忽略列表滚动带来的状态回传
 */
@Composable
private fun VerticalScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (itemCount == 0) return

    val coroutineScope = rememberCoroutineScope()

    // 状态分离：拖拽状态
    var isDragging by remember { mutableStateOf(false) }
    // 拖拽时的进度值（由手势驱动）
    var dragProgress by remember { mutableStateOf(0f) }
    // 轨道高度（像素）
    var trackHeightPx by remember { mutableStateOf(0) }

    // 计算可见项目数量
    val visibleItemCount = listState.layoutInfo.visibleItemsInfo.size

    // 不需要滚动条的情况
    if (visibleItemCount >= itemCount) return

    // ========== 1. 精确的进度计算（基于 Index 含小数） ==========
    val listProgress by remember {
        derivedStateOf {
            if (itemCount == 0) return@derivedStateOf 0f

            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            // 获取可见项目的高度（假设高度大致相同）
            val itemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1

            // 精确索引 = 整数部分 + 小数部分（偏移量/项目高度）
            val exactIndex = firstVisibleIndex + (firstVisibleOffset.toFloat() / itemHeight.toFloat())

            // 进度 = 精确索引 / 总项目数
            (exactIndex / itemCount.toFloat()).coerceIn(0f, 1f)
        }
    }

    // 滑块高度比例
    val thumbHeightPercent = visibleItemCount.toFloat() / itemCount.toFloat()

    // ========== 2. 状态分离：选择数据源 ==========
    // 拖拽时用手势驱动的进度，否则用列表状态驱动的进度
    val displayProgress = if (isDragging) dragProgress else listProgress

    // ========== 3. 滑块位置计算 ==========
    // 滑块Y坐标 = 进度 * (轨道高度 - 滑块高度)
    val thumbOffsetY = (trackHeightPx * (1f - thumbHeightPercent) * displayProgress).toInt()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(20.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .onSizeChanged { trackHeightPx = it.height }
            .pointerInput(itemCount, trackHeightPx) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        when (event.type) {
                            PointerEventType.Press -> {
                                // 开始拖拽：记录初始进度
                                isDragging = true
                                dragProgress = displayProgress
                                event.changes.forEach { it.consume() }
                            }
                            PointerEventType.Release -> {
                                // 结束拖拽
                                isDragging = false
                                event.changes.forEach { it.consume() }
                            }
                            PointerEventType.Move -> {
                                if (isDragging && trackHeightPx > 0) {
                                    val change = event.changes.first()

                                    // 手指在轨道上的Y坐标
                                    val touchY = change.position.y

                                    // 可滚动的轨道高度 = 总轨道高度 - 滑块高度
                                    val scrollableTrackHeight = trackHeightPx * (1f - thumbHeightPercent)

                                    // 计算新进度（0-1）
                                    val newProgress = (touchY / scrollableTrackHeight).coerceIn(0f, 1f)

                                    // 更新拖拽进度（由手势驱动）
                                    dragProgress = newProgress

                                    // 反向计算目标索引
                                    val targetIndex = (newProgress * itemCount).toInt()
                                        .coerceIn(0, itemCount - 1)

                                    // 驱动列表滚动
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }

                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // 滚动条背景轨道
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isDragging)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
        )

        // 滚动条滑块
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(thumbHeightPercent)
                .offset { IntOffset(0, thumbOffsetY) }
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isDragging)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
        )
    }
}