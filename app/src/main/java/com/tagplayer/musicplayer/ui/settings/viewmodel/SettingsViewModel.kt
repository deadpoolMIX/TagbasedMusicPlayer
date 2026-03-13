package com.tagplayer.musicplayer.ui.settings.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagplayer.musicplayer.data.local.database.ScanFolderDao
import com.tagplayer.musicplayer.data.local.entity.ScanFolder
import com.tagplayer.musicplayer.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scanFolderDao: ScanFolderDao
) : ViewModel() {

    // 主题设置
    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    // 扫描文件夹列表
    val scanFolders: StateFlow<List<ScanFolder>> = scanFolderDao.getAllScanFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 设置主题模式
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    // 添加扫描文件夹
    fun addScanFolder(folder: ScanFolder) {
        viewModelScope.launch {
            scanFolderDao.insertFolder(folder)
        }
    }

    // 删除扫描文件夹
    fun removeScanFolder(folder: ScanFolder) {
        viewModelScope.launch {
            scanFolderDao.deleteScanFolder(folder)
        }
    }

    // 导出备份
    fun exportBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                settingsRepository.exportBackup(context, uri)
                onResult(true, "备份导出成功")
            } catch (e: Exception) {
                onResult(false, "备份失败: ${e.message}")
            }
        }
    }

    // 导入备份
    fun importBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                settingsRepository.importBackup(context, uri)
                onResult(true, "备份导入成功")
            } catch (e: Exception) {
                onResult(false, "导入失败: ${e.message}")
            }
        }
    }

    // 清除缓存
    fun clearCache(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val cacheDir = context.cacheDir
                val size = cacheDir?.walkTopDown()?.map { it.length() }?.sum() ?: 0
                cacheDir?.deleteRecursively()
                val sizeStr = formatFileSize(size)
                onResult(true, "已清除 $sizeStr 缓存")
            } catch (e: Exception) {
                onResult(false, "清除失败: ${e.message}")
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
