package com.tagplayer.musicplayer.data.repository

import com.tagplayer.musicplayer.data.local.database.SongDao
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.scanner.MusicScanner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao,
    private val musicScanner: MusicScanner
) {
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    fun getRecentlyPlayedSongs(): Flow<List<Song>> = songDao.getRecentlyPlayedSongs()

    suspend fun getSongById(songId: Long): Song? = songDao.getSongById(songId)

    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    fun getSongCount(): Flow<Int> = songDao.getSongCount()

    suspend fun insertSong(song: Song) = songDao.insertSong(song)

    suspend fun insertSongs(songs: List<Song>) = songDao.insertSongs(songs)

    suspend fun deleteSong(song: Song) = songDao.deleteSong(song)

    suspend fun deleteSongById(songId: Long) = songDao.deleteSongById(songId)

    suspend fun incrementPlayCount(songId: Long) {
        songDao.incrementPlayCount(songId, System.currentTimeMillis())
    }

    /**
     * 扫描所有歌曲
     */
    suspend fun scanAndSaveSongs() {
        musicScanner.scanAllSongs().collect { scannedSongs ->
            songDao.insertSongs(scannedSongs)
        }
    }

    /**
     * 扫描指定文件夹内的歌曲
     */
    suspend fun scanFolder(path: String): List<Song> {
        val songs = musicScanner.scanSongsByPath(path)
        songDao.insertSongs(songs)
        return songs
    }

    suspend fun syncSongs() {
        // Get current songs from MediaStore
        musicScanner.scanAllSongs().collect { scannedSongs ->
            val scannedIds = scannedSongs.map { it.id }.toSet()

            // Delete songs that no longer exist in MediaStore
            // This requires getting all songs from DB and comparing
            // Implementation depends on specific sync strategy
            songDao.insertSongs(scannedSongs)
        }
    }
}
