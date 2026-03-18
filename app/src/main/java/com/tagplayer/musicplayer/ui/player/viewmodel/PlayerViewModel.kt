package com.tagplayer.musicplayer.ui.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.player.MusicPlayer
import com.tagplayer.musicplayer.player.PlaybackState
import com.tagplayer.musicplayer.player.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = musicPlayer.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaybackState()
        )

    val currentPosition: StateFlow<Long> = musicPlayer.currentPosition
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val duration: StateFlow<Long> = musicPlayer.duration
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    // 播放队列
    val queue: StateFlow<List<Song>> = musicPlayer.queue
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 当前播放索引
    val currentIndex: StateFlow<Int> = musicPlayer.currentIndex
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun playPauseToggle() {
        musicPlayer.playPauseToggle()
    }

    fun play() {
        musicPlayer.play()
    }

    fun playNext() {
        musicPlayer.playNext()
    }

    fun playPrevious() {
        musicPlayer.playPrevious()
    }

    fun seekTo(position: Long) {
        musicPlayer.seekTo(position)
    }

    fun setRepeatMode(mode: RepeatMode) {
        musicPlayer.setRepeatMode(mode)
    }

    fun toggleShuffle() {
        val currentShuffle = playbackState.value.isShuffling
        musicPlayer.setShuffleEnabled(!currentShuffle)
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        musicPlayer.setQueue(songs, startIndex)
    }

    fun playAtIndex(index: Int) {
        musicPlayer.playAtIndex(index)
    }

    fun addToQueue(song: Song) {
        musicPlayer.addToQueue(song)
    }

    fun addToQueueNext(song: Song) {
        musicPlayer.addToQueueNext(song)
    }

    fun removeFromQueue(index: Int) {
        musicPlayer.removeFromQueue(index)
    }

    fun clearQueue() {
        musicPlayer.clearQueue()
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        musicPlayer.moveSong(fromIndex, toIndex)
    }

    /**
     * 随机播放歌曲列表
     */
    fun playRandom(songs: List<Song>) {
        if (songs.isEmpty()) return
        val randomIndex = (0 until songs.size).random()
        musicPlayer.setQueue(songs, randomIndex)
        // 开启随机播放模式
        musicPlayer.setShuffleEnabled(true)
    }
}
