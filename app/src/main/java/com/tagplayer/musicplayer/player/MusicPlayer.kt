package com.tagplayer.musicplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.tagplayer.musicplayer.PlaybackService
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.repository.SongRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    // MediaController 连接 Future
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // 后备本地播放器（在 Service 连接前使用）
    private var localPlayer: ExoPlayer? = null

    // 当前使用的播放器（优先使用 MediaController）
    private val player: Player?
        get() = mediaController ?: localPlayer

    // 位置更新任务
    private var positionUpdateJob: Job? = null

    // 连接状态
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playerEvents = Channel<PlayerEvent>(Channel.BUFFERED)
    val playerEvents: Flow<PlayerEvent> = _playerEvents.receiveAsFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val playbackQueue = PlaybackQueue()

    // 标记是否已同步队列到 Player
    private var isQueueSynced = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    _playerEvents.trySend(PlayerEvent.TrackEnded)
                    // Media3 会自动处理下一首，我们只需要更新内部状态
                    handleTrackEnded()
                }
                Player.STATE_READY -> {
                    _duration.value = player?.duration ?: 0L
                }
                else -> {}
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                val songId = it.mediaId.toLongOrNull() ?: return
                // 从 Player 获取当前索引，更新内部状态
                val playerIndex = player?.currentMediaItemIndex ?: 0
                _currentIndex.value = playerIndex
                playbackQueue.jumpToSong(playerIndex)

                val song = playbackQueue.getCurrentSong()
                _playbackState.value = _playbackState.value.copy(
                    currentSong = song,
                    currentSongId = songId,
                    currentIndex = playerIndex
                )
                _playerEvents.trySend(PlayerEvent.TrackChanged(songId))

                // 记录播放
                scope.launch {
                    withContext(Dispatchers.IO) {
                        songRepository.incrementPlayCount(songId)
                    }
                }
            }
        }
    }

    init {
        initializeLocalPlayer()
        connectToService()
    }

    private fun initializeLocalPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        localPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(playerListener)
            }
    }

    private fun connectToService() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))

        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)
                _isConnected.value = true

                // 如果已有队列，同步到新的 MediaController
                if (isQueueSynced && playbackQueue.getQueue().isNotEmpty()) {
                    syncQueueToPlayer(playbackQueue.getCurrentIndex())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isConnected.value = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun releasePlayer() {
        stopPositionUpdates()
        mediaController?.removeListener(playerListener)
        localPlayer?.removeListener(playerListener)
        localPlayer?.release()
        localPlayer = null

        controllerFuture?.let {
            if (!it.isCancelled && !it.isDone) {
                it.cancel(true)
            }
        }
        mediaController?.release()
        mediaController = null
        controllerFuture = null
        _isConnected.value = false
        isQueueSynced = false
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun playPauseToggle() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun seekForward(milliseconds: Long = 10000) {
        player?.let {
            val newPosition = (it.currentPosition + milliseconds).coerceAtMost(it.duration)
            it.seekTo(newPosition)
        }
    }

    fun seekBackward(milliseconds: Long = 10000) {
        player?.let {
            val newPosition = (it.currentPosition - milliseconds).coerceAtLeast(0)
            it.seekTo(newPosition)
        }
    }

    fun playNext() {
        val currentIndex = player?.currentMediaItemIndex ?: 0
        val queueSize = playbackQueue.getQueue().size
        if (currentIndex < queueSize - 1) {
            player?.seekToDefaultPosition(currentIndex + 1)
        } else if (playbackState.value.repeatMode == RepeatMode.ALL) {
            // 列表循环模式下，跳到第一首
            player?.seekToDefaultPosition(0)
        }
        player?.play()
    }

    fun playPrevious() {
        val currentIndex = player?.currentMediaItemIndex ?: 0
        if (currentIndex > 0) {
            // 直接跳到上一首歌曲，不管播放进度
            player?.seekToDefaultPosition(currentIndex - 1)
        } else if (playbackState.value.repeatMode == RepeatMode.ALL) {
            // 列表循环模式下，跳到最后一首
            val lastIndex = playbackQueue.getQueue().size - 1
            if (lastIndex >= 0) {
                player?.seekToDefaultPosition(lastIndex)
            }
        }
        player?.play()
    }

    /**
     * 设置播放队列并开始播放
     * 关键：将整个队列一次性注入 Player，让 Media3 获得播放列表上下文
     */
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        // 保持用户设置的随机状态，不再重置

        // 1. 更新内部队列
        playbackQueue.setQueue(songs, startIndex)
        _queue.value = playbackQueue.getQueue()
        _currentIndex.value = playbackQueue.getCurrentIndex()

        val currentSong = playbackQueue.getCurrentSong()
        if (currentSong != null) {
            _playbackState.value = _playbackState.value.copy(
                currentSong = currentSong,
                currentSongId = currentSong.id,
                currentIndex = startIndex
            )

            // 2. 将整个队列转换为 MediaItem 列表
            val mediaItems = songs.map { song -> createMediaItem(song) }

            // 3. 一次性注入完整队列到 Player（不使用 Player 的 shuffle）
            player?.let { p ->
                p.shuffleModeEnabled = false
                p.setMediaItems(mediaItems, startIndex, 0L)
                p.prepare()
                p.play()
            }

            isQueueSynced = true

            // 记录播放
            scope.launch {
                withContext(Dispatchers.IO) {
                    songRepository.incrementPlayCount(currentSong.id)
                }
            }
        }
    }

    /**
     * 播放指定索引的歌曲
     * 使用 seekToDefaultPosition 跳转，无需重新设置队列
     */
    fun playAtIndex(index: Int) {
        if (index < 0 || index >= playbackQueue.getQueue().size) return

        playbackQueue.jumpToSong(index)
        _currentIndex.value = index

        val song = playbackQueue.getCurrentSong()
        if (song != null) {
            _playbackState.value = _playbackState.value.copy(
                currentSong = song,
                currentSongId = song.id,
                currentIndex = index
            )

            // 使用 Player 的跳转功能
            player?.seekToDefaultPosition(index)
            player?.play()
        }
    }

    fun addToQueue(song: Song) {
        playbackQueue.addToQueue(song)
        _queue.value = playbackQueue.getQueue()

        // 同步到 Player
        player?.addMediaItem(createMediaItem(song))
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun addToQueueNext(song: Song) {
        val insertIndex = playbackQueue.getCurrentIndex() + 1
        playbackQueue.addToQueueNext(song)
        _queue.value = playbackQueue.getQueue()

        // 同步到 Player
        player?.addMediaItem(insertIndex, createMediaItem(song))
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun removeFromQueue(index: Int) {
        if (index < 0 || index >= playbackQueue.getQueue().size) return

        val wasCurrentIndex = index == playbackQueue.getCurrentIndex()

        playbackQueue.removeFromQueue(index)
        _queue.value = playbackQueue.getQueue()
        _currentIndex.value = playbackQueue.getCurrentIndex()
        _playbackState.value = _playbackState.value.copy(
            currentIndex = playbackQueue.getCurrentIndex()
        )

        // 同步到 Player
        player?.removeMediaItem(index)

        // 如果删除的是当前播放的歌曲，播放器会自动跳到下一首
        if (wasCurrentIndex) {
            val newSong = playbackQueue.getCurrentSong()
            if (newSong != null) {
                _playbackState.value = _playbackState.value.copy(
                    currentSong = newSong,
                    currentSongId = newSong.id
                )
            }
        }

        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun clearQueue() {
        playbackQueue.clear()
        _queue.value = emptyList()
        player?.clearMediaItems()
        isQueueSynced = false
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun getQueue(): List<Song> {
        return playbackQueue.getQueue()
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        if (fromIndex < 0 || fromIndex >= playbackQueue.getQueue().size) return
        if (toIndex < 0 || toIndex >= playbackQueue.getQueue().size) return

        playbackQueue.moveSong(fromIndex, toIndex)
        _queue.value = playbackQueue.getQueue()
        _currentIndex.value = playbackQueue.getCurrentIndex()
        _playbackState.value = _playbackState.value.copy(
            currentIndex = playbackQueue.getCurrentIndex()
        )

        // 同步到 Player
        player?.moveMediaItem(fromIndex, toIndex)
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun setRepeatMode(mode: RepeatMode) {
        playbackQueue.setRepeatMode(mode)
        _playbackState.value = _playbackState.value.copy(repeatMode = mode)
        updatePlayerRepeatMode(mode)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        if (playbackQueue.isShuffleEnabled() == enabled) return

        // 保存当前播放状态
        val wasPlaying = player?.isPlaying == true
        val currentPosition = player?.currentPosition ?: 0L

        // 保存当前播放的歌曲ID，用于在打乱后重新定位
        val currentSongId = playbackQueue.getCurrentSong()?.id

        playbackQueue.setShuffleEnabled(enabled)
        _playbackState.value = _playbackState.value.copy(isShuffling = enabled)
        _queue.value = playbackQueue.getQueue()

        // 不使用 Media3 的内置随机，而是重新同步打乱后的队列
        // 这样 Player 和 PlaybackQueue 的顺序完全一致
        val songs = playbackQueue.getQueue()
        if (songs.isNotEmpty()) {
            // 找到当前歌曲在新队列中的位置
            val newIndex = if (currentSongId != null) {
                songs.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)
            } else {
                0
            }

            val mediaItems = songs.map { createMediaItem(it) }
            // 使用 setMediaItems 并指定位置，保持当前播放位置
            player?.setMediaItems(mediaItems, newIndex, currentPosition)
            // 必须调用 prepare() 来准备播放
            player?.prepare()
            _currentIndex.value = newIndex

            // 如果之前正在播放，立即恢复播放
            if (wasPlaying) {
                player?.playWhenReady = true
            }
        }
    }

    fun updatePosition() {
        player?.let {
            _currentPosition.value = it.currentPosition
        }
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = scope.launch {
            while (true) {
                updatePosition()
                delay(500L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    /**
     * 创建 MediaItem
     */
    private fun createMediaItem(song: Song): MediaItem {
        val albumArtUri = if (song.albumId > 0) {
            android.net.Uri.parse("content://media/external/audio/albumart/${song.albumId}")
        } else null

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.filePath)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(albumArtUri)
                    .build()
            )
            .build()
    }

    /**
     * 同步队列到 Player（用于 MediaController 重连时）
     */
    private fun syncQueueToPlayer(startIndex: Int) {
        val songs = playbackQueue.getQueue()
        if (songs.isEmpty()) return

        val mediaItems = songs.map { createMediaItem(it) }
        player?.let { p ->
            p.shuffleModeEnabled = false
            p.setMediaItems(mediaItems, startIndex, 0L)
        }
    }

    /**
     * 处理曲目结束
     * Media3 会自动播放下一首，这里只处理边界情况
     */
    private fun handleTrackEnded() {
        // Media3 已自动处理下一首，更新内部状态
        val playerIndex = player?.currentMediaItemIndex ?: -1
        if (playerIndex >= 0) {
            _currentIndex.value = playerIndex
            playbackQueue.jumpToSong(playerIndex)
        }
    }

    private fun updatePlayerRepeatMode(mode: RepeatMode) {
        player?.let {
            when (mode) {
                RepeatMode.OFF -> {
                    it.repeatMode = Player.REPEAT_MODE_OFF
                }
                RepeatMode.ONE -> {
                    it.repeatMode = Player.REPEAT_MODE_ONE
                }
                RepeatMode.ALL -> {
                    it.repeatMode = Player.REPEAT_MODE_ALL
                }
            }
        }
    }
}