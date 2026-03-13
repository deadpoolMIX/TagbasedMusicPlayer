package com.tagplayer.musicplayer.data.repository

import com.tagplayer.musicplayer.data.local.database.SongDao
import com.tagplayer.musicplayer.data.local.entity.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Artist(
    val name: String,
    val songCount: Int,
    val songs: List<Song>
)

data class Album(
    val name: String,
    val artist: String,
    val albumId: Long,
    val songCount: Int,
    val songs: List<Song>
)

@Singleton
class ArtistRepository @Inject constructor(
    private val songDao: SongDao
) {
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    fun getArtists(): Flow<List<Artist>> {
        return songDao.getAllSongs().map { songs ->
            songs.groupBy { it.artist }
                .map { (artistName, artistSongs) ->
                    Artist(
                        name = artistName,
                        songCount = artistSongs.size,
                        songs = artistSongs
                    )
                }
                .sortedBy { it.name.lowercase() }
        }
    }

    fun getAlbums(): Flow<List<Album>> {
        return songDao.getAllSongs().map { songs ->
            songs.groupBy { Pair(it.album, it.artist) }
                .map { (key, albumSongs) ->
                    Album(
                        name = key.first,
                        artist = key.second,
                        albumId = albumSongs.firstOrNull()?.albumId ?: 0L,
                        songCount = albumSongs.size,
                        songs = albumSongs
                    )
                }
                .sortedBy { it.name.lowercase() }
        }
    }

    fun getSongsByArtist(artistName: String): Flow<List<Song>> {
        return songDao.getAllSongs().map { songs ->
            songs.filter { it.artist == artistName }
        }
    }

    fun getSongsByAlbum(albumName: String, artistName: String): Flow<List<Song>> {
        return songDao.getAllSongs().map { songs ->
            songs.filter { it.album == albumName && it.artist == artistName }
        }
    }
}
