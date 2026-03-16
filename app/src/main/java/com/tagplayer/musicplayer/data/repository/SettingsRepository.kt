package com.tagplayer.musicplayer.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tagplayer.musicplayer.data.local.database.MusicDatabase
import com.tagplayer.musicplayer.data.local.entity.Playlist
import com.tagplayer.musicplayer.data.local.entity.PlaylistSong
import com.tagplayer.musicplayer.data.local.entity.ScanFolder
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.local.entity.SongTag
import com.tagplayer.musicplayer.data.local.entity.Tag
import com.tagplayer.musicplayer.ui.settings.viewmodel.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val database: MusicDatabase
) {
    private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
    private val DEFAULT_SORT_KEY = intPreferencesKey("default_sort")

    // 主题模式
    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { preferences ->
            val mode = preferences[THEME_MODE_KEY] ?: 2 // 默认跟随系统
            ThemeMode.entries[mode]
        }

    // 设置主题模式
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.ordinal
        }
    }

    // 默认排序
    val defaultSort: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[DEFAULT_SORT_KEY] ?: 0 // 默认按添加时间降序
        }

    // 设置默认排序
    suspend fun setDefaultSort(sortOrdinal: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_SORT_KEY] = sortOrdinal
        }
    }

    // 导出备份
    suspend fun exportBackup(context: Context, uri: Uri) {
        val backupData = createBackupData()
        val jsonString = Json.encodeToString(backupData)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(jsonString)
            }
        }
    }

    // 导入备份
    suspend fun importBackup(context: Context, uri: Uri) {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } ?: throw IllegalStateException("无法读取文件")

        val backupData = Json.decodeFromString<BackupData>(jsonString)
        restoreBackupData(backupData)
    }

    // 创建备份数据
    private suspend fun createBackupData(): BackupData {
        val songDao = database.songDao()
        val tagDao = database.tagDao()
        val playlistDao = database.playlistDao()

        // 获取所有歌曲用于转换 songId -> 稳定标识
        val allSongs = songDao.getAllSongsList()
        val songMap = allSongs.associateBy { it.id }

        // 获取原始关联数据
        val originalSongTags = tagDao.getAllSongTagsList()
        val originalPlaylistSongs = playlistDao.getAllPlaylistSongsList()

        // 转换为跨设备兼容格式
        val songTagBackups = originalSongTags.mapNotNull { songTag ->
            val song = songMap[songTag.songId]
            if (song != null) {
                SongTagBackup(
                    songPath = song.filePath,
                    songTitle = song.title,
                    songArtist = song.artist,
                    tagName = tagDao.getTagById(songTag.tagId)?.name ?: return@mapNotNull null,
                    addedAt = songTag.addedAt
                )
            } else null
        }

        val playlistSongBackups = originalPlaylistSongs.mapNotNull { playlistSong ->
            val song = songMap[playlistSong.songId]
            val playlist = playlistDao.getPlaylistById(playlistSong.playlistId)
            if (song != null && playlist != null) {
                PlaylistSongBackup(
                    songPath = song.filePath,
                    songTitle = song.title,
                    songArtist = song.artist,
                    playlistName = playlist.name,
                    sortOrder = playlistSong.sortOrder,
                    addedAt = playlistSong.addedAt
                )
            } else null
        }

        return BackupData(
            version = 2, // 新版本号
            exportTime = System.currentTimeMillis(),
            tags = tagDao.getAllTagsList(),
            songTagBackups = songTagBackups,
            playlists = playlistDao.getAllPlaylistsList(),
            playlistSongBackups = playlistSongBackups,
            scanFolders = database.scanFolderDao().getAllFoldersList(),
            // 保留旧格式用于向后兼容
            songTags = originalSongTags,
            playlistSongs = originalPlaylistSongs
        )
    }

    // 恢复备份数据
    private suspend fun restoreBackupData(data: BackupData) {
        val songDao = database.songDao()
        val tagDao = database.tagDao()
        val playlistDao = database.playlistDao()

        // 获取当前设备的所有歌曲
        val allSongs = songDao.getAllSongsList()

        // 先清空现有数据（保留歌曲数据，因为它是从MediaStore扫描的）
        database.withTransaction {
            tagDao.deleteAllSongTags()
            playlistDao.deleteAllPlaylistSongs()
            playlistDao.deleteAllPlaylists()
            tagDao.deleteAllTags()
            database.scanFolderDao().deleteAllFolders()
        }

        // 恢复标签
        data.tags.forEach { tagDao.insertTag(it) }
        // 建立标签名称到ID的映射
        val tagNameToId = tagDao.getAllTagsList().associate { it.name to it.id }

        // 恢复歌单
        data.playlists.forEach { playlistDao.insertPlaylist(it) }
        // 建立歌单名称到ID的映射
        val playlistNameToId = playlistDao.getAllPlaylistsList().associate { it.name to it.id }

        // 恢复扫描文件夹
        data.scanFolders.forEach { database.scanFolderDao().insertFolder(it) }

        // 根据备份版本选择恢复策略
        if (data.version >= 2 && data.songTagBackups != null && data.playlistSongBackups != null) {
            // 新版本：使用跨设备兼容格式
            restoreSongTagsWithMatching(data.songTagBackups, allSongs, tagNameToId)
            restorePlaylistSongsWithMatching(data.playlistSongBackups, allSongs, playlistNameToId)
            Log.d("SettingsRepository", "使用新版本备份恢复（跨设备兼容）")
        } else {
            // 旧版本：尝试直接恢复，但可能失败
            var matchedCount = 0
            data.songTags.forEach { songTag ->
                val songExists = allSongs.any { it.id == songTag.songId }
                if (songExists) {
                    tagDao.insertSongTag(songTag)
                    matchedCount++
                }
            }
            data.playlistSongs.forEach { playlistSong ->
                val songExists = allSongs.any { it.id == playlistSong.songId }
                if (songExists) {
                    playlistDao.insertPlaylistSong(playlistSong)
                    matchedCount++
                }
            }
            Log.d("SettingsRepository", "使用旧版本备份恢复，匹配成功 $matchedCount 条")
        }
    }

    // 使用混合匹配恢复歌曲标签
    private suspend fun restoreSongTagsWithMatching(
        songTagBackups: List<SongTagBackup>,
        allSongs: List<Song>,
        tagNameToId: Map<String, Long>
    ) {
        val songDao = database.songDao()
        var pathMatched = 0
        var titleMatched = 0
        var failed = 0

        songTagBackups.forEach { backup ->
            val song = findMatchingSong(backup.songPath, backup.songTitle, backup.songArtist, allSongs, songDao)
            val tagId = tagNameToId[backup.tagName]

            if (song != null && tagId != null) {
                val songTag = SongTag(
                    songId = song.id,
                    tagId = tagId,
                    addedAt = backup.addedAt
                )
                database.tagDao().insertSongTag(songTag)
                // 统计匹配方式
                if (song.filePath == backup.songPath) pathMatched++ else titleMatched++
            } else {
                failed++
            }
        }

        Log.d("SettingsRepository", "标签恢复: 路径匹配=$pathMatched, 标题匹配=$titleMatched, 失败=$failed")
    }

    // 使用混合匹配恢复歌单歌曲
    private suspend fun restorePlaylistSongsWithMatching(
        playlistSongBackups: List<PlaylistSongBackup>,
        allSongs: List<Song>,
        playlistNameToId: Map<String, Long>
    ) {
        val songDao = database.songDao()
        var pathMatched = 0
        var titleMatched = 0
        var failed = 0

        playlistSongBackups.forEach { backup ->
            val song = findMatchingSong(backup.songPath, backup.songTitle, backup.songArtist, allSongs, songDao)
            val playlistId = playlistNameToId[backup.playlistName]

            if (song != null && playlistId != null) {
                val playlistSong = PlaylistSong(
                    playlistId = playlistId,
                    songId = song.id,
                    sortOrder = backup.sortOrder,
                    addedAt = backup.addedAt
                )
                database.playlistDao().insertPlaylistSong(playlistSong)
                if (song.filePath == backup.songPath) pathMatched++ else titleMatched++
            } else {
                failed++
            }
        }

        Log.d("SettingsRepository", "歌单恢复: 路径匹配=$pathMatched, 标题匹配=$titleMatched, 失败=$failed")
    }

    // 混合匹配歌曲：优先路径精确匹配，其次标题+歌手匹配
    private suspend fun findMatchingSong(
        songPath: String,
        songTitle: String,
        songArtist: String,
        allSongs: List<Song>,
        songDao: com.tagplayer.musicplayer.data.local.database.SongDao
    ): Song? {
        // 1. 优先使用路径精确匹配
        val pathMatched = allSongs.find { it.filePath == songPath }
        if (pathMatched != null) return pathMatched

        // 2. 其次使用标题+歌手精确匹配
        val titleMatched = allSongs.find {
            it.title == songTitle && it.artist == songArtist
        }
        if (titleMatched != null) return titleMatched

        // 3. 最后尝试模糊匹配（标题相同即可）
        return allSongs.find { it.title == songTitle }
    }
}

@Serializable
data class BackupData(
    val version: Int,
    val exportTime: Long,
    val tags: List<Tag>,
    val songTags: List<SongTag>, // 保留用于向后兼容
    val playlists: List<Playlist>,
    val playlistSongs: List<PlaylistSong>, // 保留用于向后兼容
    val scanFolders: List<ScanFolder>,
    // 新版本跨设备兼容格式
    val songTagBackups: List<SongTagBackup>? = null,
    val playlistSongBackups: List<PlaylistSongBackup>? = null
)

/**
 * 歌曲标签备份（跨设备兼容）
 */
@Serializable
data class SongTagBackup(
    val songPath: String,      // 文件路径作为主要标识
    val songTitle: String,     // 标题作为备用标识
    val songArtist: String,    // 歌手作为备用标识
    val tagName: String,       // 使用标签名称而非ID
    val addedAt: Long
)

/**
 * 歌单歌曲备份（跨设备兼容）
 */
@Serializable
data class PlaylistSongBackup(
    val songPath: String,
    val songTitle: String,
    val songArtist: String,
    val playlistName: String,  // 使用歌单名称而非ID
    val sortOrder: Int,
    val addedAt: Long
)
