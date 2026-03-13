package com.tagplayer.musicplayer.ui.playlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagplayer.musicplayer.data.local.entity.Playlist
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.repository.PlaylistRepository
import com.tagplayer.musicplayer.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository
) : ViewModel() {

    // 歌单列表
    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 歌单数量
    val playlistCount: StateFlow<Int> = playlistRepository.getPlaylistCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 是否显示创建歌单对话框
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    // 新歌单名称
    private val _newPlaylistName = MutableStateFlow("")
    val newPlaylistName: StateFlow<String> = _newPlaylistName.asStateFlow()

    // 选中的歌单（用于详情页）
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    // 歌单中的歌曲
    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

    // 是否显示添加到歌单对话框
    private val _showAddToPlaylistDialog = MutableStateFlow(false)
    val showAddToPlaylistDialog: StateFlow<Boolean> = _showAddToPlaylistDialog.asStateFlow()

    // 待添加的歌曲
    private val _songToAdd = MutableStateFlow<Song?>(null)
    val songToAdd: StateFlow<Song?> = _songToAdd.asStateFlow()

    // 所有歌曲（用于添加歌曲时选择）
    val allSongs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun showCreateDialog() {
        _newPlaylistName.value = ""
        _showCreateDialog.value = true
    }

    fun dismissCreateDialog() {
        _showCreateDialog.value = false
        _newPlaylistName.value = ""
    }

    fun onNewPlaylistNameChange(name: String) {
        _newPlaylistName.value = name
    }

    fun createPlaylist() {
        val name = _newPlaylistName.value.trim()
        if (name.isNotBlank()) {
            viewModelScope.launch {
                playlistRepository.createPlaylist(name)
                dismissCreateDialog()
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist)
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        // 取消之前的收集
        _playlistSongs.value = emptyList()
        // 启动新的收集
        viewModelScope.launch {
            playlistRepository.getSongsInPlaylist(playlist.id)
                .collect { songs ->
                    _playlistSongs.value = songs
                }
        }
    }

    fun showAddToPlaylistDialog(song: Song) {
        _songToAdd.value = song
        _showAddToPlaylistDialog.value = true
    }

    fun dismissAddToPlaylistDialog() {
        _showAddToPlaylistDialog.value = false
        _songToAdd.value = null
    }

    fun addSongToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val song = _songToAdd.value
            if (song != null) {
                playlistRepository.addSongToPlaylist(playlistId, song.id)
                dismissAddToPlaylistDialog()
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, song.id)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            songIds.forEach { songId ->
                playlistRepository.addSongToPlaylist(playlistId, songId)
            }
        }
    }

    companion object {
        const val FAVORITES_PLAYLIST_ID = -1L
    }

    // "我喜欢"歌单中的歌曲ID集合
    private val _favoriteSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongIds: StateFlow<Set<Long>> = _favoriteSongIds.asStateFlow()

    init {
        // 加载"我喜欢"歌单的歌曲
        viewModelScope.launch {
            playlistRepository.getSongsInPlaylist(FAVORITES_PLAYLIST_ID)
                .collect { songs ->
                    _favoriteSongIds.value = songs.map { it.id }.toSet()
                }
        }
    }

    fun isSongFavorite(songId: Long): Boolean {
        return _favoriteSongIds.value.contains(songId)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            if (isSongFavorite(song.id)) {
                playlistRepository.removeSongFromPlaylist(FAVORITES_PLAYLIST_ID, song.id)
            } else {
                playlistRepository.addSongToPlaylist(FAVORITES_PLAYLIST_ID, song.id)
            }
        }
    }
}
