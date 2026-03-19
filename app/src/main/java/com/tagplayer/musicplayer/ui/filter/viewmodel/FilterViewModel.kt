package com.tagplayer.musicplayer.ui.filter.viewmodel

import androidx.lifecycle.SavedStateHandle
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

private const val KEY_BOX_A_TAG_IDS = "box_a_tag_ids"
private const val KEY_BOX_B_TAG_IDS = "box_b_tag_ids"
private const val KEY_BOX_C_TAG_IDS = "box_c_tag_ids"

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
    private val playlistRepository: PlaylistRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 使用 SavedStateHandle 保存标签ID，确保导航返回后状态保持
    private val _boxATagIds = savedStateHandle.getStateFlow(KEY_BOX_A_TAG_IDS, emptyList<Long>())
    private val _boxBTagIds = savedStateHandle.getStateFlow(KEY_BOX_B_TAG_IDS, emptyList<Long>())
    private val _boxCTagIds = savedStateHandle.getStateFlow(KEY_BOX_C_TAG_IDS, emptyList<Long>())
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
                    savedStateHandle[KEY_BOX_A_TAG_IDS] = _boxATagIds.value + tag.id
                }
            }
            FilterBox.B -> {
                if (tag.id !in _boxBTagIds.value) {
                    savedStateHandle[KEY_BOX_B_TAG_IDS] = _boxBTagIds.value + tag.id
                }
            }
            FilterBox.C -> {
                if (tag.id !in _boxCTagIds.value) {
                    savedStateHandle[KEY_BOX_C_TAG_IDS] = _boxCTagIds.value + tag.id
                }
            }
        }
    }

    fun removeTagFromBox(tag: Tag, box: FilterBox) {
        when (box) {
            FilterBox.A -> savedStateHandle[KEY_BOX_A_TAG_IDS] = _boxATagIds.value - tag.id
            FilterBox.B -> savedStateHandle[KEY_BOX_B_TAG_IDS] = _boxBTagIds.value - tag.id
            FilterBox.C -> savedStateHandle[KEY_BOX_C_TAG_IDS] = _boxCTagIds.value - tag.id
        }
    }

    fun clearBox(box: FilterBox) {
        when (box) {
            FilterBox.A -> savedStateHandle[KEY_BOX_A_TAG_IDS] = emptyList<Long>()
            FilterBox.B -> savedStateHandle[KEY_BOX_B_TAG_IDS] = emptyList<Long>()
            FilterBox.C -> savedStateHandle[KEY_BOX_C_TAG_IDS] = emptyList<Long>()
        }
    }

    fun clearAllFilters() {
        savedStateHandle[KEY_BOX_A_TAG_IDS] = emptyList<Long>()
        savedStateHandle[KEY_BOX_B_TAG_IDS] = emptyList<Long>()
        savedStateHandle[KEY_BOX_C_TAG_IDS] = emptyList<Long>()
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
