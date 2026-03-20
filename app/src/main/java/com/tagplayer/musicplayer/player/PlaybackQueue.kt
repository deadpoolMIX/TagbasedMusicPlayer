package com.tagplayer.musicplayer.player

import com.tagplayer.musicplayer.data.local.entity.Song

class PlaybackQueue {
    private var originalQueue: MutableList<Song> = mutableListOf()
    private var shuffledQueue: MutableList<Song> = mutableListOf()
    private var currentIndex: Int = -1
    private var repeatMode: RepeatMode = RepeatMode.OFF
    private var isShuffling: Boolean = true  // 默认开启随机播放

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        originalQueue = songs.toMutableList()
        shuffledQueue = originalQueue.toMutableList()
        currentIndex = if (songs.isNotEmpty()) startIndex.coerceIn(0, songs.size - 1) else -1
        if (isShuffling) {
            // 随机模式下，打乱队列但确保用户点击的歌曲在索引0
            shuffleQueueWithStartSong(startIndex)
            currentIndex = 0
        }
    }

    fun getQueue(): List<Song> {
        return (if (isShuffling) shuffledQueue else originalQueue).toList()
    }

    fun getCurrentSong(): Song? {
        val queue = if (isShuffling) shuffledQueue else originalQueue
        return if (currentIndex in queue.indices) queue[currentIndex] else null
    }

    fun getCurrentIndex(): Int = currentIndex

    fun getNextSong(): Song? {
        val queue = if (isShuffling) shuffledQueue else originalQueue
        if (queue.isEmpty()) return null

        return when (repeatMode) {
            RepeatMode.ONE -> getCurrentSong()
            RepeatMode.OFF -> {
                // 随机模式下自动循环，顺序模式下播放到末尾停止
                if (isShuffling) {
                    currentIndex = (currentIndex + 1) % queue.size
                    queue[currentIndex]
                } else {
                    currentIndex++
                    if (currentIndex >= queue.size) {
                        currentIndex = -1
                        null
                    } else {
                        queue[currentIndex]
                    }
                }
            }
            RepeatMode.ALL -> {
                currentIndex = (currentIndex + 1) % queue.size
                queue[currentIndex]
            }
        }
    }

    fun getPreviousSong(): Song? {
        val queue = if (isShuffling) shuffledQueue else originalQueue
        if (queue.isEmpty()) return null

        return when (repeatMode) {
            RepeatMode.ONE -> getCurrentSong()
            RepeatMode.OFF, RepeatMode.ALL -> {
                currentIndex--
                if (currentIndex < 0) {
                    currentIndex = queue.size - 1
                }
                queue[currentIndex]
            }
        }
    }

    fun addToQueue(song: Song) {
        originalQueue.add(song)
        if (isShuffling) {
            shuffledQueue.add(song)
        }
    }

    fun addToQueueNext(song: Song) {
        val insertIndex = (currentIndex + 1).coerceAtMost(originalQueue.size)
        originalQueue.add(insertIndex, song)
        if (isShuffling) {
            shuffledQueue.add(insertIndex, song)
        }
    }

    fun removeFromQueue(index: Int): Boolean {
        if (index !in originalQueue.indices) return false

        originalQueue.removeAt(index)
        if (index in shuffledQueue.indices) {
            shuffledQueue.removeAt(index)
        }

        if (index < currentIndex) {
            currentIndex--
        } else if (index == currentIndex) {
            if (currentIndex >= originalQueue.size) {
                currentIndex = originalQueue.size - 1
            }
        }

        return true
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in originalQueue.indices || toIndex !in originalQueue.indices) return
        if (fromIndex == toIndex) return

        val song = originalQueue.removeAt(fromIndex)
        originalQueue.add(toIndex, song)

        if (isShuffling) {
            val shuffledSong = shuffledQueue.removeAt(fromIndex)
            shuffledQueue.add(toIndex, shuffledSong)
        }

        // Update currentIndex
        when {
            fromIndex == currentIndex -> currentIndex = toIndex
            fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex--
            fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex++
        }
    }

    fun clear() {
        originalQueue.clear()
        shuffledQueue.clear()
        currentIndex = -1
    }

    fun setRepeatMode(mode: RepeatMode) {
        repeatMode = mode
    }

    fun getRepeatMode(): RepeatMode = repeatMode

    fun setShuffleEnabled(enabled: Boolean) {
        if (isShuffling == enabled) return
        isShuffling = enabled
        if (enabled) {
            shuffleQueue()
        }
    }

    fun isShuffleEnabled(): Boolean = isShuffling

    private fun shuffleQueue() {
        if (originalQueue.isEmpty()) return

        val currentSong = getCurrentSong()
        shuffledQueue = originalQueue.toMutableList()
        shuffledQueue.shuffle()

        // Keep current song at current position
        if (currentSong != null) {
            val newIndex = shuffledQueue.indexOf(currentSong)
            if (newIndex != -1 && newIndex != currentIndex) {
                shuffledQueue.removeAt(newIndex)
                shuffledQueue.add(currentIndex.coerceIn(0, shuffledQueue.size), currentSong)
            }
        }
    }

    /**
     * 打乱队列并确保指定索引的歌曲在位置0
     * 用于设置新队列时保持用户点击的歌曲为当前播放歌曲
     */
    private fun shuffleQueueWithStartSong(startIndex: Int) {
        if (originalQueue.isEmpty() || startIndex !in originalQueue.indices) return

        val startSong = originalQueue[startIndex]
        shuffledQueue = originalQueue.toMutableList()
        shuffledQueue.shuffle()

        // 将用户点击的歌曲移到索引0
        val startSongIndex = shuffledQueue.indexOf(startSong)
        if (startSongIndex != -1 && startSongIndex != 0) {
            shuffledQueue.removeAt(startSongIndex)
            shuffledQueue.add(0, startSong)
        }
    }

    fun jumpToSong(index: Int): Song? {
        val queue = if (isShuffling) shuffledQueue else originalQueue
        if (index !in queue.indices) return null
        currentIndex = index
        return queue[currentIndex]
    }

    fun jumpToSong(song: Song): Song? {
        val queue = if (isShuffling) shuffledQueue else originalQueue
        val index = queue.indexOf(song)
        if (index == -1) return null
        currentIndex = index
        return song
    }

    fun playAtIndex(index: Int): Song? {
        return jumpToSong(index)
    }
}
