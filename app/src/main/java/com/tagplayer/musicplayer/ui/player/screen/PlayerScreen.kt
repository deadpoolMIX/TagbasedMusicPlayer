package com.tagplayer.musicplayer.ui.player.screen

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.List
import com.tagplayer.musicplayer.data.local.entity.Tag
import com.tagplayer.musicplayer.player.RepeatMode
import com.tagplayer.musicplayer.ui.components.TagSelectionDialog
import com.tagplayer.musicplayer.ui.player.viewmodel.PlayerViewModel
import com.tagplayer.musicplayer.ui.playlist.viewmodel.PlaylistViewModel
import com.tagplayer.musicplayer.ui.tags.viewmodel.TagViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    onNavigateToLyrics: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {},
    onNavigateToArtistDetail: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
    tagViewModel: TagViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    var showTagDialog by remember { mutableStateOf(false) }

    val currentSong = playbackState.currentSong

    // 收藏状态 - 使用 collectAsState 监听变化
    val favoriteSongIds by playlistViewModel.favoriteSongIds.collectAsState()
    val isFavorite = currentSong?.let { favoriteSongIds.contains(it.id) } ?: false

    // 获取当前歌曲的标签
    val songTags = remember(currentSong) {
        if (currentSong != null) {
            tagViewModel.getSongTagsFlow(currentSong.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // 滑动关闭手势状态 - 使用offset让页面向下滑出
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = swipeOffset,
        label = "offset"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    // 下滑箭头按钮
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "关闭"
                        )
                    }
                },
                actions = {
                    // 收藏按钮
                    IconButton(
                        onClick = {
                            currentSong?.let { playlistViewModel.toggleFavorite(it) }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "已收藏" else "收藏",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
        if (currentSong == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无播放歌曲",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .offset(y = animatedOffset.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (swipeOffset > 200) {
                                    onBackClick()
                                }
                                swipeOffset = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount > 0) {
                                    swipeOffset += dragAmount
                                }
                            }
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Album Art - 点击进入歌词
                AlbumArt(
                    filePath = currentSong.filePath,
                    albumId = currentSong.albumId,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToLyrics() }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Song Info
                Text(
                    text = currentSong.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentSong.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (currentSong.artist.isNotBlank() && currentSong.artist != "<unknown>") {
                                onNavigateToArtistDetail(currentSong.artist)
                            }
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 标签快捷位
                TagChipsRow(
                    tags = songTags.value,
                    onAddTagClick = { showTagDialog = true },
                    onTagClick = {
                        // 点击标签打开标签管理弹窗
                        showTagDialog = true
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Progress Bar - 修复拖动后不更新问题
                ProgressBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeek = { position -> viewModel.seekTo(position) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Control Buttons
                ControlButtons(
                    isPlaying = playbackState.isPlaying,
                    isShuffling = playbackState.isShuffling,
                    repeatMode = playbackState.repeatMode,
                    onPlayPause = { viewModel.playPauseToggle() },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onModeChange = {
                        // 循环切换：顺序 → 随机 → 列表循环 → 单曲循环 → 顺序
                        when {
                            playbackState.isShuffling -> {
                                // 当前是随机，切换到列表循环
                                viewModel.setShuffleEnabled(false)
                                viewModel.setRepeatMode(RepeatMode.ALL)
                            }
                            playbackState.repeatMode == RepeatMode.ALL -> {
                                // 当前是列表循环，切换到单曲循环
                                viewModel.setRepeatMode(RepeatMode.ONE)
                            }
                            playbackState.repeatMode == RepeatMode.ONE -> {
                                // 当前是单曲循环，切换到顺序
                                viewModel.setRepeatMode(RepeatMode.OFF)
                            }
                            else -> {
                                // 当前是顺序，切换到随机
                                viewModel.setShuffleEnabled(true)
                            }
                        }
                    },
                    onQueueClick = onNavigateToQueue
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // 标签选择弹窗
        if (showTagDialog && currentSong != null) {
            TagSelectionDialog(
                song = currentSong,
                onDismiss = { showTagDialog = false }
            )
        }
    }
}

@Composable
private fun AlbumArt(
    filePath: String,
    albumId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 使用 produceState 异步加载专辑封面（优先内嵌封面）
    val bitmap = produceState<Bitmap?>(initialValue = null, filePath, albumId) {
        value = withContext(Dispatchers.IO) {
            // 优先从文件提取内嵌封面
            loadEmbeddedAlbumArt(context, filePath)
                // fallback 到 albumId 封面
                ?: loadPlayerAlbumArt(context, albumId)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap.value != null) {
            Image(
                bitmap = bitmap.value!!.asImageBitmap(),
                contentDescription = "专辑封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 从音频文件中提取内嵌封面
 */
@Suppress("DEPRECATION")
private fun loadEmbeddedAlbumArt(context: android.content.Context, filePath: String): Bitmap? {
    if (filePath.isBlank()) return null

    return try {
        val retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val embeddedPicture = retriever.embeddedPicture
        retriever.release()

        if (embeddedPicture != null) {
            BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

private fun loadPlayerAlbumArt(context: android.content.Context, albumId: Long): Bitmap? {
    if (albumId <= 0) return null

    return try {
        val uri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun TagChipsRow(
    tags: List<Tag>,
    onAddTagClick: () -> Unit,
    onTagClick: (Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // 显示已有标签
        items(tags) { tag ->
            TagChip(
                tag = tag,
                onClick = { onTagClick(tag) }
            )
        }
        // 添加按钮
        item {
            AddTagChip(onClick = onAddTagClick)
        }
    }
}

@Composable
private fun TagChip(
    tag: Tag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${tag.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AddTagChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(9999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加标签",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    // 状态分离：isDragging 控制是否拦截底层进度更新
    var isDragging by remember { mutableStateOf(false) }
    // UI 显示的进度值（独立状态）
    var sliderValue by remember { mutableFloatStateOf(0f) }

    // 监听真实进度，仅在非拖拽时同步
    LaunchedEffect(currentPosition) {
        if (!isDragging && duration > 0) {
            sliderValue = currentPosition.toFloat() / duration
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                isDragging = true
                sliderValue = newValue
            },
            onValueChangeFinished = {
                onSeek((sliderValue * duration).toLong())
                isDragging = false
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration((sliderValue * duration).toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ControlButtons(
    isPlaying: Boolean,
    isShuffling: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onModeChange: () -> Unit,
    onQueueClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 播放队列按钮（左侧）
        IconButton(
            onClick = onQueueClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "播放队列",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Previous
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "上一首",
                modifier = Modifier.size(32.dp)
            )
        }

        // Play/Pause
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Next
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "下一首",
                modifier = Modifier.size(32.dp)
            )
        }

        // 合并的播放模式按钮（右侧）：顺序 → 随机 → 列表循环 → 单曲循环
        IconButton(
            onClick = onModeChange,
            modifier = Modifier.size(48.dp)
        ) {
            when {
                isShuffling -> {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "随机播放",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                repeatMode == RepeatMode.ALL -> {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "列表循环",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                repeatMode == RepeatMode.ONE -> {
                    Icon(
                        imageVector = Icons.Default.RepeatOne,
                        contentDescription = "单曲循环",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "顺序播放",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}
