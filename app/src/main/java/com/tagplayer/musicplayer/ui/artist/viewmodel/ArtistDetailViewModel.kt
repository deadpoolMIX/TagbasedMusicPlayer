package com.tagplayer.musicplayer.ui.artist.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.repository.Artist
import com.tagplayer.musicplayer.data.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val artistName: String = savedStateHandle.get<String>("artistName") ?: ""

    // 艺术家的歌曲列表
    val songs: StateFlow<List<Song>> = artistRepository.getSongsByArtist(artistName)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 艺术家信息
    private val _artistInfo = MutableStateFlow<Artist?>(null)
    val artistInfo: StateFlow<Artist?> = _artistInfo.asStateFlow()

    // 多选模式状态
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    // 多选模式下选中的歌曲集合
    private val _selectedSongs = MutableStateFlow<Set<Song>>(emptySet())
    val selectedSongs: StateFlow<Set<Song>> = _selectedSongs.asStateFlow()

    init {
        // 获取艺术家信息
        viewModelScope.launch {
            artistRepository.getArtists().collect { artists ->
                _artistInfo.value = artists.find { it.name == artistName }
            }
        }
    }

    // ==================== 多选模式相关方法 ====================

    /**
     * 进入多选模式
     */
    fun enterMultiSelectMode(initialSong: Song? = null) {
        _isMultiSelectMode.value = true
        if (initialSong != null) {
            _selectedSongs.value = setOf(initialSong)
        } else {
            _selectedSongs.value = emptySet()
        }
    }

    /**
     * 退出多选模式
     */
    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedSongs.value = emptySet()
    }

    /**
     * 切换歌曲选中状态
     */
    fun toggleSongSelection(song: Song) {
        val currentSet = _selectedSongs.value.toMutableSet()
        if (song in currentSet) {
            currentSet.remove(song)
        } else {
            currentSet.add(song)
        }
        _selectedSongs.value = currentSet

        // 如果没有任何选中的歌曲，退出多选模式
        if (currentSet.isEmpty()) {
            _isMultiSelectMode.value = false
        }
    }

    /**
     * 全选当前列表中的所有歌曲
     */
    fun selectAllSongs(allSongs: List<Song>) {
        _selectedSongs.value = allSongs.toSet()
    }

    /**
     * 取消所有选择
     */
    fun clearSelection() {
        _selectedSongs.value = emptySet()
    }
}
