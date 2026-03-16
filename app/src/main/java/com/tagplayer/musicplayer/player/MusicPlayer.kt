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
                val song = playbackQueue.getCurrentSong()
                _playbackState.value = _playbackState.value.copy(
                    currentSong = song,
                    currentSongId = songId
                )
                _playerEvents.trySend(PlayerEvent.TrackChanged(songId))
            }
        }
    }

    init {
        // 初始化本地播放器作为后备
        initializeLocalPlayer()
        // 连接到 PlaybackService
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

    /**
     * 连接到 PlaybackService
     * 使用 MediaController 异步连接，连接成功后系统自动生成通知栏播放器
     */
    private fun connectToService() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))

        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)
                _isConnected.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                // 连接失败，继续使用本地播放器
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

        // 释放 MediaController
        controllerFuture?.let {
            if (!it.isCancelled && !it.isDone) {
                it.cancel(true)
            }
        }
        mediaController?.release()
        mediaController = null
        controllerFuture = null
        _isConnected.value = false
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
        val nextSong = playbackQueue.getNextSong()
        if (nextSong != null) {
            playSong(nextSong)
        }
    }

    fun playPrevious() {
        val prevSong = playbackQueue.getPreviousSong()
        if (prevSong != null) {
            playSong(prevSong)
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        playbackQueue.setQueue(songs, startIndex)
        _queue.value = playbackQueue.getQueue()
        _currentIndex.value = playbackQueue.getCurrentIndex()
        val currentSong = playbackQueue.getCurrentSong()
        if (currentSong != null) {
            _playbackState.value = _playbackState.value.copy(
                currentSong = currentSong,
                currentSongId = currentSong.id
            )
            playSong(currentSong)
        }
    }

    fun playAtIndex(index: Int) {
        val song = playbackQueue.playAtIndex(index)
        _currentIndex.value = index
        if (song != null) {
            _playbackState.value = _playbackState.value.copy(
                currentSong = song,
                currentSongId = song.id,
                currentIndex = index
            )
            playSong(song)
        }
    }

    fun addToQueue(song: Song) {
        playbackQueue.addToQueue(song)
        _queue.value = playbackQueue.getQueue()
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun addToQueueNext(song: Song) {
        playbackQueue.addToQueueNext(song)
        _queue.value = playbackQueue.getQueue()
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun removeFromQueue(index: Int) {
        playbackQueue.removeFromQueue(index)
        _queue.value = playbackQueue.getQueue()
        _currentIndex.value = playbackQueue.getCurrentIndex()
        _playbackState.value = _playbackState.value.copy(
            currentIndex = playbackQueue.getCurrentIndex()
        )
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun clearQueue() {
        playbackQueue.clear()
        _queue.value = emptyList()
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun getQueue(): List<Song> {
        return playbackQueue.getQueue()
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        playbackQueue.moveSong(fromIndex, toIndex)
        _queue.value = playbackQueue.getQueue()
        _currentIndex.value = playbackQueue.getCurrentIndex()
        _playbackState.value = _playbackState.value.copy(
            currentIndex = playbackQueue.getCurrentIndex()
        )
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun setRepeatMode(mode: RepeatMode) {
        playbackQueue.setRepeatMode(mode)
        _playbackState.value = _playbackState.value.copy(repeatMode = mode)
        updatePlayerRepeatMode(mode)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        playbackQueue.setShuffleEnabled(enabled)
        _playbackState.value = _playbackState.value.copy(isShuffling = enabled)
        player?.shuffleModeEnabled = enabled
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

    private fun playSong(song: Song) {
        _currentIndex.value = playbackQueue.getCurrentIndex()

        val albumArtUri = if (song.albumId > 0) {
            android.net.Uri.parse("content://media/external/audio/albumart/${song.albumId}")
        } else null

        val mediaItem = MediaItem.Builder()
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

        player?.let {
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                songRepository.incrementPlayCount(song.id)
            }
        }
    }

    private fun handleTrackEnded() {
        when (playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                player?.seekTo(0)
                player?.play()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                playNext()
            }
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