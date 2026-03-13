package com.tagplayer.musicplayer.data.repository

import android.content.Context
import android.net.Uri
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
        return BackupData(
            version = 1,
            exportTime = System.currentTimeMillis(),
            tags = database.tagDao().getAllTagsList(),
            songTags = database.tagDao().getAllSongTagsList(),
            playlists = database.playlistDao().getAllPlaylistsList(),
            playlistSongs = database.playlistDao().getAllPlaylistSongsList(),
            scanFolders = database.scanFolderDao().getAllFoldersList()
        )
    }

    // 恢复备份数据
    private suspend fun restoreBackupData(data: BackupData) {
        // 先清空现有数据（保留歌曲数据，因为它是从MediaStore扫描的）
        database.withTransaction {
            // 清空标签关联
            database.tagDao().deleteAllSongTags()
            // 清空歌单歌曲关联
            database.playlistDao().deleteAllPlaylistSongs()
            // 清空歌单
            database.playlistDao().deleteAllPlaylists()
            // 清空标签
            database.tagDao().deleteAllTags()
            // 清空扫描文件夹
            database.scanFolderDao().deleteAllFolders()
        }

        // 恢复数据
        data.tags.forEach { database.tagDao().insertTag(it) }
        data.songTags.forEach { database.tagDao().insertSongTag(it) }
        data.playlists.forEach { database.playlistDao().insertPlaylist(it) }
        data.playlistSongs.forEach { database.playlistDao().insertPlaylistSong(it) }
        data.scanFolders.forEach { database.scanFolderDao().insertFolder(it) }
    }
}

@Serializable
data class BackupData(
    val version: Int,
    val exportTime: Long,
    val tags: List<Tag>,
    val songTags: List<SongTag>,
    val playlists: List<Playlist>,
    val playlistSongs: List<PlaylistSong>,
    val scanFolders: List<ScanFolder>
)
