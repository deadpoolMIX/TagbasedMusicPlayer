package com.tagplayer.musicplayer.data.repository

import com.tagplayer.musicplayer.data.local.database.SongDao
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.util.ArtistNameParser
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

    /**
     * 获取所有艺术家列表（支持多歌手解析）
     * 使用内存倒排索引：Map<歌手名, List<Song>>
     */
    fun getArtists(): Flow<List<Artist>> {
        return songDao.getAllSongs().map { songs ->
            // 构建倒排索引
            val artistIndex = buildArtistIndex(songs)

            // 转换为 Artist 列表
            artistIndex.map { (artistName, artistSongs) ->
                Artist(
                    name = artistName,
                    songCount = artistSongs.size,
                    songs = artistSongs
                )
            }.sortedBy { it.name.lowercase() }
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

    /**
     * 获取指定艺术家的歌曲（精确匹配，支持多歌手解析）
     */
    fun getSongsByArtist(artistName: String): Flow<List<Song>> {
        return songDao.getAllSongs().map { songs ->
            // 使用倒排索引精确匹配
            val artistIndex = buildArtistIndex(songs)
            artistIndex[artistName] ?: emptyList()
        }
    }

    fun getSongsByAlbum(albumName: String, artistName: String): Flow<List<Song>> {
        return songDao.getAllSongs().map { songs ->
            songs.filter { it.album == albumName && it.artist == artistName }
        }
    }

    /**
     * 构建歌手倒排索引
     * @return Map<精确歌手名, 该歌手的所有歌曲>
     */
    private fun buildArtistIndex(songs: List<Song>): Map<String, List<Song>> {
        val index = mutableMapOf<String, MutableList<Song>>()

        songs.forEach { song ->
            // 解析歌手字符串，可能包含多个歌手
            val artistNames = ArtistNameParser.parse(song.artist)

            artistNames.forEach { artistName ->
                // 将歌曲添加到每个歌手的列表中
                index.getOrPut(artistName) { mutableListOf() }.add(song)
            }
        }

        return index
    }
}
