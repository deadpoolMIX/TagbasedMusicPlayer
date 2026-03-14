package com.tagplayer.musicplayer.ui.tags.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.local.entity.Tag
import com.tagplayer.musicplayer.data.repository.SongRepository
import com.tagplayer.musicplayer.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val songRepository: SongRepository
) : ViewModel() {

    // 所有标签
    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 所有歌曲
    val allSongs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 标签数量
    val tagCount: StateFlow<Int> = tagRepository.getTagCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 筛选后的标签
    val filteredTags: StateFlow<List<Tag>> = combine(
        allTags,
        searchQuery
    ) { tags, query ->
        if (query.isBlank()) {
            tags
        } else {
            tags.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 选中的标签（用于显示歌曲）
    private val _selectedTag = MutableStateFlow<Tag?>(null)
    val selectedTag: StateFlow<Tag?> = _selectedTag.asStateFlow()

    // 选中标签的歌曲
    val selectedTagSongs: StateFlow<List<Song>> = _selectedTag
        .flatMapLatest { tag ->
            if (tag != null) {
                tagRepository.getSongsForTag(tag.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 是否显示创建标签对话框
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    // 是否显示编辑标签对话框
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 是否显示删除确认对话框
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    // 新标签/编辑标签名称
    private val _tagNameInput = MutableStateFlow("")
    val tagNameInput: StateFlow<String> = _tagNameInput.asStateFlow()

    // 当前编辑的标签
    private val _editingTag = MutableStateFlow<Tag?>(null)

    // 是否显示标签选择对话框（用于给歌曲添加标签）
    private val _showTagSelectionDialog = MutableStateFlow(false)
    val showTagSelectionDialog: StateFlow<Boolean> = _showTagSelectionDialog.asStateFlow()

    // 待添加标签的歌曲
    private val _songToTag = MutableStateFlow<Song?>(null)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun showCreateDialog() {
        _tagNameInput.value = ""
        _editingTag.value = null
        _showCreateDialog.value = true
    }

    fun dismissCreateDialog() {
        _showCreateDialog.value = false
        _tagNameInput.value = ""
    }

    fun showEditDialog(tag: Tag) {
        _editingTag.value = tag
        _tagNameInput.value = tag.name
        _showEditDialog.value = true
    }

    fun dismissEditDialog() {
        _showEditDialog.value = false
        _tagNameInput.value = ""
        _editingTag.value = null
    }

    fun showDeleteDialog(tag: Tag) {
        _editingTag.value = tag
        _showDeleteDialog.value = true
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = false
        _editingTag.value = null
    }

    fun onTagNameInputChange(name: String) {
        _tagNameInput.value = name
    }

    fun createTag() {
        val name = _tagNameInput.value.trim()
        if (name.isNotBlank()) {
            viewModelScope.launch {
                tagRepository.createTag(name)
                dismissCreateDialog()
            }
        }
    }

    fun updateTag() {
        val tag = _editingTag.value
        val newName = _tagNameInput.value.trim()
        if (tag != null && newName.isNotBlank() && newName != tag.name) {
            viewModelScope.launch {
                val updated = tag.copy(
                    name = newName,
                    updatedAt = System.currentTimeMillis()
                )
                tagRepository.updateTag(updated)
                dismissEditDialog()
            }
        } else {
            dismissEditDialog()
        }
    }

    fun deleteTag() {
        val tag = _editingTag.value
        if (tag != null) {
            viewModelScope.launch {
                tagRepository.deleteTag(tag)
                dismissDeleteDialog()
            }
        }
    }

    fun selectTag(tag: Tag) {
        _selectedTag.value = tag
    }

    fun clearSelectedTag() {
        _selectedTag.value = null
    }

    // 显示标签选择对话框（给歌曲添加标签）
    fun showTagSelectionDialog(song: Song) {
        _songToTag.value = song
        _showTagSelectionDialog.value = true
    }

    fun dismissTagSelectionDialog() {
        _showTagSelectionDialog.value = false
        _songToTag.value = null
    }

    // 给歌曲添加标签（直接指定歌曲和标签）
    fun addTagToSong(songId: Long, tagId: Long) {
        viewModelScope.launch {
            tagRepository.addTagToSong(songId, tagId)
        }
    }

    // 批量给多首歌曲添加标签
    fun addTagToSongs(tagId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            songIds.forEach { songId ->
                tagRepository.addTagToSong(songId, tagId)
            }
        }
    }

    // 给歌曲添加标签（从对话框中使用）
    fun addTagToSong(tag: Tag) {
        viewModelScope.launch {
            val song = _songToTag.value
            if (song != null) {
                tagRepository.addTagToSong(song.id, tag.id)
                dismissTagSelectionDialog()
            }
        }
    }

    // 从歌曲移除标签
    fun removeTagFromSong(songId: Long, tagId: Long) {
        viewModelScope.launch {
            tagRepository.removeTagFromSong(songId, tagId)
        }
    }

    // 获取歌曲的所有标签
    fun getSongTags(songId: Long, callback: (List<Tag>) -> Unit) {
        viewModelScope.launch {
            tagRepository.getTagsForSong(songId).collect { tags ->
                callback(tags)
            }
        }
    }

    // 检查歌曲是否有某个标签
    fun isSongTagged(songId: Long, tagId: Long, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exists = tagRepository.isSongTagged(songId, tagId)
            callback(exists)
        }
    }

    // 获取歌曲的所有标签（Flow方式）
    fun getSongTagsFlow(songId: Long): Flow<List<Tag>> {
        return tagRepository.getTagsForSong(songId)
    }

    // 创建新标签并添加到歌曲
    fun createTagAndAddToSong(songId: Long, name: String) {
        viewModelScope.launch {
            val tagId = tagRepository.createTag(name)
            tagRepository.addTagToSong(songId, tagId)
        }
    }
}
