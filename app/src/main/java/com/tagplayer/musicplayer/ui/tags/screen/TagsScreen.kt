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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
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
 */
@Composable
private fun VerticalScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (itemCount == 0) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }

    // 计算滚动进度（基于像素距离）
    val scrollProgress by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.totalItemsCount == 0 || itemCount == 0) return@derivedStateOf 0f

            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            val visibleItems = layoutInfo.visibleItemsInfo

            if (visibleItems.isEmpty()) return@derivedStateOf 0f

            // 计算总内容高度估算
            // 使用第一个可见项目的位置 + 当前滚动偏移来估算
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()

            // 可见区域的高度（像素）
            val visibleContentHeight = lastItem.offset + lastItem.size - firstItem.offset

            // 平均每个项目的高度
            val avgItemHeight = if (visibleItems.size > 1) {
                visibleContentHeight.toFloat() / visibleItems.size
            } else {
                firstItem.size.toFloat()
            }

            // 估算总内容高度
            val estimatedTotalHeight = avgItemHeight * itemCount

            // 最大可滚动距离
            val maxScrollDistance = estimatedTotalHeight - viewportHeight

            if (maxScrollDistance <= 0f) return@derivedStateOf 0f

            // 当前滚动位置
            val currentScrollOffset = firstItem.offset.toFloat() - listState.firstVisibleItemScrollOffset

            // 计算进度（0到1）
            val progress = (-currentScrollOffset / maxScrollDistance).coerceIn(0f, 1f)
            progress
        }
    }

    // 滚动条高度（相对于可用高度的百分比）
    val scrollbarHeightPercent by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.totalItemsCount == 0 || itemCount == 0) return@derivedStateOf 0.1f

            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            val visibleItems = layoutInfo.visibleItemsInfo

            if (visibleItems.isEmpty()) return@derivedStateOf 0.1f

            // 计算平均项目高度
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            val visibleContentHeight = lastItem.offset + lastItem.size - firstItem.offset
            val avgItemHeight = if (visibleItems.size > 1) {
                visibleContentHeight.toFloat() / visibleItems.size
            } else {
                firstItem.size.toFloat()
            }

            // 估算总内容高度
            val estimatedTotalHeight = avgItemHeight * itemCount

            // 滚动条高度比例 = 视口高度 / 总内容高度
            (viewportHeight / estimatedTotalHeight).coerceIn(0.1f, 1f)
        }
    }

    // 是否需要显示滚动条
    val showScrollbar = scrollbarHeightPercent < 0.95f

    if (!showScrollbar) return

    // 计算滑块偏移量
    val thumbOffsetY by remember(scrollProgress, scrollbarHeightPercent) {
        derivedStateOf {
            scrollProgress * (1f - scrollbarHeightPercent)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(20.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .pointerInput(itemCount) {
                // 拖拽手势处理
                var lastY = 0f

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        when (event.type) {
                            PointerEventType.Press -> {
                                isDragging = true
                                lastY = event.changes.first().position.y
                                event.changes.forEach { it.consume() }
                            }
                            PointerEventType.Release -> {
                                isDragging = false
                                event.changes.forEach { it.consume() }
                            }
                            PointerEventType.Move -> {
                                if (isDragging && itemCount > 0) {
                                    val change = event.changes.first()
                                    val currentY = change.position.y
                                    val deltaY = currentY - lastY
                                    lastY = currentY

                                    // 获取滚动条轨道高度
                                    val layoutInfo = listState.layoutInfo
                                    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                                    val trackHeight = viewportHeight - with(density) { 16.dp.toPx() } * 2

                                    if (trackHeight > 0 && scrollbarHeightPercent < 1f) {
                                        // 计算总可滚动距离（像素）
                                        val visibleItems = layoutInfo.visibleItemsInfo
                                        if (visibleItems.isNotEmpty()) {
                                            val firstItem = visibleItems.first()
                                            val lastItem = visibleItems.last()
                                            val visibleContentHeight = lastItem.offset + lastItem.size - firstItem.offset
                                            val avgItemHeight = if (visibleItems.size > 1) {
                                                visibleContentHeight.toFloat() / visibleItems.size
                                            } else {
                                                firstItem.size.toFloat()
                                            }
                                            val estimatedTotalHeight = avgItemHeight * itemCount
                                            val maxScrollDistance = estimatedTotalHeight - viewportHeight

                                            // 将拖拽距离转换为列表滚动距离
                                            val scrollRatio = maxScrollDistance / trackHeight
                                            val scrollDelta = -deltaY * scrollRatio

                                            coroutineScope.launch {
                                                listState.scroll {
                                                    scrollBy(scrollDelta)
                                                }
                                            }
                                        }
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
                .fillMaxHeight(scrollbarHeightPercent)
                .graphicsLayer { translationY = size.height * thumbOffsetY }
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