package com.tagplayer.musicplayer.ui.filter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.local.entity.Tag
import com.tagplayer.musicplayer.data.repository.FilterRepository
import com.tagplayer.musicplayer.data.repository.TagRepository
import com.tagplayer.musicplayer.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilterState(
    val boxATags: List<Tag> = emptyList(),
    val boxBTags: List<Tag> = emptyList(),
    val boxCTags: List<Tag> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FilterViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
    private val tagRepository: TagRepository,
    private val songRepository: com.tagplayer.musicplayer.data.repository.SongRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _boxATagIds = MutableStateFlow<List<Long>>(emptyList())
    private val _boxBTagIds = MutableStateFlow<List<Long>>(emptyList())
    private val _boxCTagIds = MutableStateFlow<List<Long>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val allTags = tagRepository.getAllTags()

    val filterState = combine(
        _boxATagIds,
        _boxBTagIds,
        _boxCTagIds,
        allTags,
        _isLoading
    ) { aIds, bIds, cIds, tags, loading ->
        FilterState(
            boxATags = tags.filter { it.id in aIds },
            boxBTags = tags.filter { it.id in bIds },
            boxCTags = tags.filter { it.id in cIds },
            isLoading = loading
        )
    }.flatMapLatest { state ->
        if (state.boxATags.isEmpty() && state.boxBTags.isEmpty() && state.boxCTags.isEmpty()) {
            // 没有筛选条件时显示所有歌曲
            songRepository.getAllSongs().combine(flowOf(state)) { songs, s ->
                s.copy(filteredSongs = songs)
            }
        } else {
            filterRepository.filterSongs(
                boxATags = state.boxATags.map { it.id },
                boxBTags = state.boxBTags.map { it.id },
                boxCTags = state.boxCTags.map { it.id }
            ).combine(flowOf(state)) { songs, s ->
                s.copy(filteredSongs = songs)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterState())

    fun addTagToBox(tag: Tag, box: FilterBox) {
        when (box) {
            FilterBox.A -> {
                if (tag.id !in _boxATagIds.value) {
                    _boxATagIds.value = _boxATagIds.value + tag.id
                }
            }
            FilterBox.B -> {
                if (tag.id !in _boxBTagIds.value) {
                    _boxBTagIds.value = _boxBTagIds.value + tag.id
                }
            }
            FilterBox.C -> {
                if (tag.id !in _boxCTagIds.value) {
                    _boxCTagIds.value = _boxCTagIds.value + tag.id
                }
            }
        }
    }

    fun removeTagFromBox(tag: Tag, box: FilterBox) {
        when (box) {
            FilterBox.A -> _boxATagIds.value = _boxATagIds.value - tag.id
            FilterBox.B -> _boxBTagIds.value = _boxBTagIds.value - tag.id
            FilterBox.C -> _boxCTagIds.value = _boxCTagIds.value - tag.id
        }
    }

    fun clearBox(box: FilterBox) {
        when (box) {
            FilterBox.A -> _boxATagIds.value = emptyList()
            FilterBox.B -> _boxBTagIds.value = emptyList()
            FilterBox.C -> _boxCTagIds.value = emptyList()
        }
    }

    fun clearAllFilters() {
        _boxATagIds.value = emptyList()
        _boxBTagIds.value = emptyList()
        _boxCTagIds.value = emptyList()
    }

    fun saveAsPlaylist(name: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val songs = filterState.value.filteredSongs
                if (songs.isEmpty()) {
                    onComplete(false)
                    return@launch
                }
                val playlistId = playlistRepository.createPlaylist(name)
                songs.forEach { song ->
                    playlistRepository.addSongToPlaylist(playlistId, song.id)
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun getFilteredSongIds(): List<Long> {
        return filterState.value.filteredSongs.map { it.id }
    }
}

enum class FilterBox {
    A, B, C
}
