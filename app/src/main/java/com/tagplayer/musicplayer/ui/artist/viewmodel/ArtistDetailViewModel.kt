package com.tagplayer.musicplayer.ui.artist.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.repository.Artist
import com.tagplayer.musicplayer.data.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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

    init {
        // 获取艺术家信息
        viewModelScope.launch {
            artistRepository.getArtists().collect { artists ->
                _artistInfo.value = artists.find { it.name == artistName }
            }
        }
    }
}
