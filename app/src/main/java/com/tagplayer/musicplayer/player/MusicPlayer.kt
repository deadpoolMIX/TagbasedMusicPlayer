package com.tagplayer.musicplayer.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.tagplayer.musicplayer.data.repository.SongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository
) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // 位置更新任务
    private var positionUpdateJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playerEvents = Channel<PlayerEvent>(Channel.BUFFERED)
    val playerEvents: Flow<PlayerEvent> = _playerEvents.receiveAsFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<com.tagplayer.musicplayer.data.local.entity.Song>>(emptyList())
    val queue: StateFlow<List<com.tagplayer.musicplayer.data.local.entity.Song>> = _queue.asStateFlow()

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
                    _duration.value = exoPlayer?.duration ?: 0L
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

    @androidx.annotation.OptIn(UnstableApi::class)
    fun initializePlayer(): ExoPlayer {
        if (exoPlayer != null) return exoPlayer!!

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(playerListener)
            }

        return exoPlayer!!
    }

    fun releasePlayer() {
        stopPositionUpdates()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun playPauseToggle() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    fun seekForward(milliseconds: Long = 10000) {
        exoPlayer?.let {
            val newPosition = (it.currentPosition + milliseconds).coerceAtMost(it.duration)
            it.seekTo(newPosition)
        }
    }

    fun seekBackward(milliseconds: Long = 10000) {
        exoPlayer?.let {
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

    fun setQueue(songs: List<com.tagplayer.musicplayer.data.local.entity.Song>, startIndex: Int = 0) {
        // 确保播放器已初始化
        initializePlayer()

        playbackQueue.setQueue(songs, startIndex)
        _queue.value = playbackQueue.getQueue()
        _currentIndex.value = playbackQueue.getCurrentIndex()
        val currentSong = playbackQueue.getCurrentSong()
        if (currentSong != null) {
            // 立即更新状态，让 MiniPlayer 显示
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

    fun addToQueue(song: com.tagplayer.musicplayer.data.local.entity.Song) {
        playbackQueue.addToQueue(song)
        _queue.value = playbackQueue.getQueue()
        _playerEvents.trySend(PlayerEvent.QueueUpdated)
    }

    fun addToQueueNext(song: com.tagplayer.musicplayer.data.local.entity.Song) {
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

    fun getQueue(): List<com.tagplayer.musicplayer.data.local.entity.Song> {
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
        updateExoPlayerRepeatMode(mode)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        playbackQueue.setShuffleEnabled(enabled)
        _playbackState.value = _playbackState.value.copy(isShuffling = enabled)
        exoPlayer?.shuffleModeEnabled = enabled
    }

    fun updatePosition() {
        exoPlayer?.let {
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

    private fun playSong(song: com.tagplayer.musicplayer.data.local.entity.Song) {
        _currentIndex.value = playbackQueue.getCurrentIndex()
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.filePath)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build()
            )
            .build()

        exoPlayer?.let {
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }

        // 更新播放记录
        scope.launch {
            withContext(Dispatchers.IO) {
                songRepository.incrementPlayCount(song.id)
            }
        }
    }

    private fun handleTrackEnded() {
        when (playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                playNext()
            }
        }
    }

    private fun updateExoPlayerRepeatMode(mode: RepeatMode) {
        exoPlayer?.let {
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
