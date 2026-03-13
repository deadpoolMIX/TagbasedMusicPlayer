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

    fun playPauseToggle() {
        musicPlayer.playPauseToggle()
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

    fun getQueue(): List<Song> {
        return musicPlayer.getQueue()
    }
}
