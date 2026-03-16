package com.tagplayer.musicplayer.ui.home.viewmodel

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagplayer.musicplayer.data.local.entity.ScanFolder
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.repository.DeleteFileResult
import com.tagplayer.musicplayer.data.repository.ScanFolderRepository
import com.tagplayer.musicplayer.data.repository.SettingsRepository
import com.tagplayer.musicplayer.data.repository.SongRepository
import com.tagplayer.musicplayer.util.AlphabetIndexUtils
import com.tagplayer.musicplayer.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
    private val scanFolderRepository: ScanFolderRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 筛选类型
    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    // 排序类型（从设置中读取默认值）
    private val _sortType = MutableStateFlow(SortType.DATE_ADDED_DESC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    init {
        // 读取默认排序设置
        viewModelScope.launch {
            settingsRepository.defaultSort.collect { sortOrdinal ->
                if (sortOrdinal in SortType.entries.indices) {
                    _sortType.value = SortType.entries[sortOrdinal]
                }
            }
        }
    }

    // 是否显示排序对话框
    private val _showSortDialog = MutableStateFlow(false)
    val showSortDialog: StateFlow<Boolean> = _showSortDialog.asStateFlow()

    // 扫描状态
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // 选中的歌曲（用于操作菜单）
    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()

    // 是否显示操作菜单
    private val _showActionSheet = MutableStateFlow(false)
    val showActionSheet: StateFlow<Boolean> = _showActionSheet.asStateFlow()

    // 多选模式状态
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    // 多选模式下选中的歌曲集合
    private val _selectedSongs = MutableStateFlow<Set<Song>>(emptySet())
    val selectedSongs: StateFlow<Set<Song>> = _selectedSongs.asStateFlow()

    // 权限状态
    private val _hasPermission = MutableStateFlow(PermissionUtils.hasAudioPermission(context))
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    // 是否显示权限请求对话框
    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()

    // 是否显示文件夹管理对话框
    private val _showFolderManager = MutableStateFlow(false)
    val showFolderManager: StateFlow<Boolean> = _showFolderManager.asStateFlow()

    // 删除文件权限请求相关
    private val _deletePermissionIntentSender = MutableStateFlow<IntentSender?>(null)
    val deletePermissionIntentSender: StateFlow<IntentSender?> = _deletePermissionIntentSender.asStateFlow()

    private val _songPendingDelete = MutableStateFlow<Song?>(null)
    val songPendingDelete: StateFlow<Song?> = _songPendingDelete.asStateFlow()

    // 扫描文件夹列表
    val scanFolders: StateFlow<List<ScanFolder>> = scanFolderRepository.getAllScanFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 歌曲总数
    val songCount: StateFlow<Int> = songRepository.getSongCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 歌曲列表（根据搜索和筛选条件）
    @OptIn(ExperimentalCoroutinesApi::class)
    val songs: StateFlow<List<Song>> = combine(
        searchQuery,
        filterType,
        sortType
    ) { query, filter, sort ->
        Triple(query, filter, sort)
    }.flatMapLatest { (query, filter, sort) ->
        getSongsFlow(query, filter, sort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 分组后的歌曲数据（用于标题排序时的字母分组显示）
    @OptIn(ExperimentalCoroutinesApi::class)
    val groupedSongs: StateFlow<Map<Char, List<Song>>> = combine(
        songs,
        sortType
    ) { songList, sort ->
        Pair(songList, sort)
    }.map { (songList, sort) ->
        // 仅在标题排序时进行分组
        if (sort == SortType.TITLE_ASC || sort == SortType.TITLE_DESC) {
            AlphabetIndexUtils.groupByFirstLetter(songList) { it.title }
        } else {
            emptyMap()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // 字母到列表索引的映射（用于滚动定位）
    val letterToIndexMap: StateFlow<Map<Char, Int>> = groupedSongs.map { grouped ->
        if (grouped.isNotEmpty()) {
            AlphabetIndexUtils.calculateLetterToIndexMap(grouped)
        } else {
            emptyMap()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    private fun getSongsFlow(query: String, filter: FilterType, sort: SortType): Flow<List<Song>> {
        return when {
            query.isNotBlank() -> {
                songRepository.searchSongs(query)
            }
            filter == FilterType.RECENT -> {
                songRepository.getRecentlyPlayedSongs()
            }
            else -> {
                songRepository.getAllSongs()
            }
        }.map { songs ->
            // 应用排序
            when (sort) {
                SortType.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAdded }
                SortType.DATE_ADDED_ASC -> songs.sortedBy { it.dateAdded }
                SortType.TITLE_ASC -> songs.sortedWith(compareBy({ AlphabetIndexUtils.getFirstLetter(it.title) }, { it.title.lowercase() }))
                SortType.TITLE_DESC -> songs.sortedWith(compareByDescending<Song> { AlphabetIndexUtils.getFirstLetter(it.title) }.thenByDescending { it.title.lowercase() })
                SortType.ARTIST_ASC -> songs.sortedWith(compareBy({ AlphabetIndexUtils.getFirstLetter(it.artist) }, { it.artist.lowercase() }))
                SortType.ARTIST_DESC -> songs.sortedWith(compareByDescending<Song> { AlphabetIndexUtils.getFirstLetter(it.artist) }.thenByDescending { it.artist.lowercase() })
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterTypeChange(type: FilterType) {
        _filterType.value = type
    }

    fun onSortTypeChange(type: SortType) {
        _sortType.value = type
    }

    fun showSortDialog() {
        _showSortDialog.value = true
    }

    fun dismissSortDialog() {
        _showSortDialog.value = false
    }

    fun onSongMoreClick(song: Song) {
        _selectedSong.value = song
        _showActionSheet.value = true
    }

    fun dismissActionSheet() {
        _showActionSheet.value = false
        // 注意：不清除_selectedSong，因为对话框可能需要使用它
    }

    fun clearSelectedSong() {
        _selectedSong.value = null
    }

    /**
     * 检查权限并扫描
     * 只扫描已添加的文件夹，扫描结果覆盖之前的歌曲
     */
    fun checkPermissionAndScan() {
        if (PermissionUtils.hasAudioPermission(context)) {
            _hasPermission.value = true
            scanAddedFoldersAndReplace()
        } else {
            _hasPermission.value = false
            _showPermissionDialog.value = true
        }
    }

    /**
     * 更新权限状态（不自动申请）
     */
    fun updatePermissionState() {
        _hasPermission.value = PermissionUtils.hasAudioPermission(context)
    }

    /**
     * 权限已授予 - 仅更新状态，不自动扫描
     */
    fun onPermissionGranted() {
        _hasPermission.value = true
        _showPermissionDialog.value = false
    }

    /**
     * 权限被拒绝
     */
    fun onPermissionDenied() {
        _hasPermission.value = false
        _showPermissionDialog.value = false
    }

    /**
     * 显示权限对话框
     */
    fun showPermissionDialog() {
        _showPermissionDialog.value = true
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
    }

    /**
     * 扫描所有歌曲（全量扫描）
     */
    fun scanSongs() {
        if (!PermissionUtils.hasAudioPermission(context)) {
            _showPermissionDialog.value = true
            return
        }

        viewModelScope.launch {
            _isScanning.value = true
            try {
                songRepository.scanAndSaveSongs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * 扫描已添加的文件夹并覆盖之前的歌曲
     * 1. 先清空数据库中的所有歌曲
     * 2. 只扫描已添加的文件夹
     * 3. 扫描结果存入数据库
     */
    private fun scanAddedFoldersAndReplace() {
        if (!PermissionUtils.hasAudioPermission(context)) {
            _showPermissionDialog.value = true
            return
        }

        viewModelScope.launch {
            _isScanning.value = true
            try {
                // 获取所有已添加的文件夹
                val folders = scanFolderRepository.getAllScanFoldersOnce()

                if (folders.isEmpty()) {
                    // 没有添加任何文件夹，不做任何操作
                    return@launch
                }

                // 先清空所有歌曲
                songRepository.deleteAllSongs()

                // 扫描每个文件夹
                folders.forEach { folder ->
                    val realPath = getRealPathFromUri(Uri.parse(folder.path))
                    if (realPath != null) {
                        songRepository.scanFolder(realPath)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * 通过 URI 添加扫描文件夹
     */
    fun addScanFolderByUri(uri: Uri) {
        viewModelScope.launch {
            try {
                // 持久化 URI 读写权限（用于后续删除文件）
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    // 忽略权限持久化错误
                    e.printStackTrace()
                }

                // 获取文件夹名称作为显示名
                val folderName = getFolderNameFromUri(uri) ?: uri.toString()

                // 获取真实路径用于扫描
                val realPath = getRealPathFromUri(uri)

                // 保存到数据库（同时存储 URI 和真实路径）
                scanFolderRepository.addScanFolder(
                    path = uri.toString(),
                    name = folderName,
                    realPath = realPath
                )

                // 扫描该文件夹
                scanFolder(uri.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFolderNameFromUri(uri: Uri): String? {
        return try {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            if (split.size >= 2) {
                val path = split[1]
                path.substringAfterLast('/')
            } else {
                uri.lastPathSegment
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    /**
     * 扫描指定文件夹
     */
    private suspend fun scanFolder(uriString: String) {
        if (!PermissionUtils.hasAudioPermission(context)) {
            _showPermissionDialog.value = true
            return
        }

        _isScanning.value = true
        try {
            // 尝试从 URI 解析真实路径
            val realPath = getRealPathFromUri(Uri.parse(uriString))
            if (realPath != null) {
                // 如果有真实路径，使用路径扫描
                val songs = songRepository.scanFolder(realPath)
                if (songs.isEmpty()) {
                    // 如果没有找到歌曲，执行全量扫描（可能路径格式不匹配）
                    songRepository.scanAndSaveSongs()
                }
            } else {
                // 无法解析路径，执行全量扫描
                songRepository.scanAndSaveSongs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isScanning.value = false
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    // 尝试解析 content URI
                    val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                    val split = docId.split(":")
                    if (split.size >= 2) {
                        val type = split[0]
                        val path = split[1]
                        when (type) {
                            "primary" -> "/storage/emulated/0/$path"
                            else -> "/storage/$type/$path"
                        }
                    } else {
                        null
                    }
                }
                "file" -> uri.path
                else -> uri.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteSong(song: Song, deleteFile: Boolean = false) {
        viewModelScope.launch {
            if (deleteFile) {
                // 尝试删除本地文件
                when (val result = songRepository.deleteSongFile(song)) {
                    is DeleteFileResult.Success -> {
                        // 文件删除成功，继续删除数据库记录
                        songRepository.deleteSong(song)
                    }
                    is DeleteFileResult.NeedPermission -> {
                        // 需要用户授权，保存状态等待 UI 处理
                        _deletePermissionIntentSender.value = result.intentSender
                        _songPendingDelete.value = song
                        return@launch
                    }
                    is DeleteFileResult.Error -> {
                        // 删除失败，只删除数据库记录
                        songRepository.deleteSong(song)
                    }
                }
            } else {
                // 只删除数据库记录
                songRepository.deleteSong(song)
            }
            dismissActionSheet()
        }
    }

    /**
     * 在用户授权删除权限后调用
     */
    fun onDeletePermissionGranted() {
        viewModelScope.launch {
            val song = _songPendingDelete.value
            if (song != null) {
                // 再次尝试删除文件
                songRepository.deleteSongFileAfterPermission(song)
                songRepository.deleteSong(song)
                _songPendingDelete.value = null
                _deletePermissionIntentSender.value = null
            }
            dismissActionSheet()
        }
    }

    /**
     * 取消删除权限请求
     */
    fun cancelDeletePermissionRequest() {
        _songPendingDelete.value = null
        _deletePermissionIntentSender.value = null
    }

    /**
     * 清除删除权限意图（在 IntentSender 启动后调用）
     */
    fun clearDeletePermissionIntentSender() {
        _deletePermissionIntentSender.value = null
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * 显示文件夹管理器
     */
    fun showFolderManager() {
        _showFolderManager.value = true
    }

    fun dismissFolderManager() {
        _showFolderManager.value = false
    }

    /**
     * 添加扫描文件夹
     */
    fun addScanFolder(path: String, name: String = path) {
        viewModelScope.launch {
            scanFolderRepository.addScanFolder(path, name)
        }
    }

    /**
     * 移除扫描文件夹
     */
    fun removeScanFolder(scanFolder: ScanFolder) {
        viewModelScope.launch {
            scanFolderRepository.removeScanFolder(scanFolder)
        }
    }

    /**
     * 重新扫描所有已添加的文件夹
     */
    fun rescanAllFolders() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                scanFolderRepository.getAllScanFolders().collect { folderList ->
                    folderList.forEach { folder ->
                        scanFolder(folder.path)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    // ==================== 多选模式相关方法 ====================

    /**
     * 进入多选模式
     */
    fun enterMultiSelectMode(initialSong: Song? = null) {
        _isMultiSelectMode.value = true
        if (initialSong != null) {
            _selectedSongs.value = setOf(initialSong)
        } else {
            _selectedSongs.value = emptySet()
        }
    }

    /**
     * 退出多选模式
     */
    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedSongs.value = emptySet()
    }

    /**
     * 切换歌曲选中状态
     */
    fun toggleSongSelection(song: Song) {
        val currentSet = _selectedSongs.value.toMutableSet()
        if (song in currentSet) {
            currentSet.remove(song)
        } else {
            currentSet.add(song)
        }
        _selectedSongs.value = currentSet

        // 如果没有任何选中的歌曲，退出多选模式
        if (currentSet.isEmpty()) {
            _isMultiSelectMode.value = false
        }
    }

    /**
     * 全选当前列表中的所有歌曲
     */
    fun selectAllSongs(allSongs: List<Song>) {
        _selectedSongs.value = allSongs.toSet()
    }

    /**
     * 取消所有选择
     */
    fun clearSelection() {
        _selectedSongs.value = emptySet()
    }

    /**
     * 批量删除选中的歌曲
     */
    fun deleteSelectedSongs(deleteFiles: Boolean = false) {
        viewModelScope.launch {
            val selectedList = _selectedSongs.value.toList()
            selectedList.forEach { song ->
                if (deleteFiles) {
                    songRepository.deleteSongFile(song)
                }
                songRepository.deleteSong(song)
            }
            exitMultiSelectMode()
        }
    }

    /**
     * 检查歌曲是否被选中
     */
    fun isSongSelected(song: Song): Boolean {
        return song in _selectedSongs.value
    }
}

enum class FilterType(val title: String) {
    ALL("全部"),
    RECENT("最近播放"),
    ARTIST("艺术家"),
    ALBUM("专辑")
}

enum class SortType(val title: String) {
    DATE_ADDED_DESC("添加时间（新→旧）"),
    DATE_ADDED_ASC("添加时间（旧→新）"),
    TITLE_ASC("歌曲名称（A→Z）"),
    TITLE_DESC("歌曲名称（Z→A）"),
    ARTIST_ASC("歌手名称（A→Z）"),
    ARTIST_DESC("歌手名称（Z→A）")
}
