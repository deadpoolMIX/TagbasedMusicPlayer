package com.tagplayer.musicplayer.player

import com.tagplayer.musicplayer.data.local.entity.Song

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val currentSongId: Long? = null,
    val currentIndex: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffling: Boolean = true,  // 默认开启随机播放
    val position: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class RepeatMode {
    OFF,      // 顺序播放
    ONE,      // 单曲循环
    ALL       // 列表循环
}

sealed class PlayerEvent {
    data class TrackChanged(val songId: Long) : PlayerEvent()
    data object TrackEnded : PlayerEvent()
    data object QueueUpdated : PlayerEvent()
    data class Error(val message: String) : PlayerEvent()
}
