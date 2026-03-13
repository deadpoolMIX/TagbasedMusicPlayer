package com.tagplayer.musicplayer.ui.album.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagplayer.musicplayer.data.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    val albums = artistRepository.getAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getSongsByAlbum(albumName: String, artistName: String) =
        artistRepository.getSongsByAlbum(albumName, artistName)
}
