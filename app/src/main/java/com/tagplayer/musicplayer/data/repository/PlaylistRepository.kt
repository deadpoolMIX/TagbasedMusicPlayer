package com.tagplayer.musicplayer.data.repository

import com.tagplayer.musicplayer.data.local.database.PlaylistDao
import com.tagplayer.musicplayer.data.local.entity.Playlist
import com.tagplayer.musicplayer.data.local.entity.PlaylistSong
import com.tagplayer.musicplayer.data.local.entity.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun getPlaylistById(playlistId: Long): Playlist? = playlistDao.getPlaylistById(playlistId)

    fun getPlaylistCount(): Flow<Int> = playlistDao.getPlaylistCount()

    suspend fun createPlaylist(name: String): Long {
        val now = System.currentTimeMillis()
        val playlist = Playlist(
            name = name,
            createdAt = now,
            updatedAt = now
        )
        return playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: Playlist) = playlistDao.updatePlaylist(playlist)

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)

    suspend fun deletePlaylistById(playlistId: Long) = playlistDao.deletePlaylistById(playlistId)

    // Playlist-Song operations
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        // 检查歌曲是否已在歌单中
        if (isSongInPlaylist(playlistId, songId)) {
            return
        }

        // 获取当前歌曲数量作为排序序号
        val playlist = getPlaylistById(playlistId)
        val sortOrder = playlist?.songCount ?: 0

        val playlistSong = PlaylistSong(
            playlistId = playlistId,
            songId = songId,
            sortOrder = sortOrder,
            addedAt = System.currentTimeMillis()
        )
        playlistDao.addSongToPlaylist(playlistSong)

        // 更新歌单的歌曲数量和更新时间
        playlist?.let {
            val updated = it.copy(
                songCount = it.songCount + 1,
                updatedAt = System.currentTimeMillis()
            )
            updatePlaylist(updated)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylistById(playlistId, songId)

        // 更新歌单的歌曲数量
        val playlist = getPlaylistById(playlistId)
        playlist?.let {
            val updated = it.copy(
                songCount = (it.songCount - 1).coerceAtLeast(0),
                updatedAt = System.currentTimeMillis()
            )
            updatePlaylist(updated)
        }
    }

    suspend fun clearPlaylist(playlistId: Long) {
        playlistDao.clearPlaylist(playlistId)
    }

    fun getPlaylistSongCount(playlistId: Long): Flow<Int> =
        playlistDao.getPlaylistSongCount(playlistId)

    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> =
        playlistDao.getSongsInPlaylist(playlistId)

    suspend fun updateSongOrder(playlistId: Long, songId: Long, newSortOrder: Int) {
        playlistDao.updateSongOrder(playlistId, songId, newSortOrder)
    }

    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean =
        playlistDao.isSongInPlaylist(playlistId, songId)
}
