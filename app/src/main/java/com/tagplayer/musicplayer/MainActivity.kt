package com.tagplayer.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tagplayer.musicplayer.ui.components.MiniPlayer
import com.tagplayer.musicplayer.ui.navigation.BottomNavBar
import com.tagplayer.musicplayer.ui.navigation.NavGraph
import com.tagplayer.musicplayer.ui.navigation.Routes
import com.tagplayer.musicplayer.ui.player.viewmodel.PlayerViewModel
import com.tagplayer.musicplayer.ui.settings.viewmodel.SettingsViewModel
import com.tagplayer.musicplayer.ui.settings.viewmodel.ThemeMode
import com.tagplayer.musicplayer.ui.theme.TagPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 通知权限状态（用于 UI 显示）
    private var onNotificationPermissionResult: ((Boolean) -> Unit)? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onNotificationPermissionResult?.invoke(isGranted)
        if (!isGranted) {
            // 权限被拒，可选择引导用户到设置页面
            // 这里不自动跳转，让用户在设置页面手动开启
        }
    }

    /**
     * 检查通知权限是否已授予
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 以下不需要动态申请
        }
    }

    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(callback: ((Boolean) -> Unit)? = null) {
        onNotificationPermissionResult = callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                callback?.invoke(true)
            }
        } else {
            callback?.invoke(true)
        }
    }

    /**
     * 跳转到系统通知设置页面
     */
    fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 请求通知权限（启动时静默请求）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 延迟请求，避免启动时立即弹窗
                android.os.Handler(mainLooper).postDelayed({
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }, 1500)
            }
        }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            TagPlayerTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                val currentRoute by remember(navBackStackEntry) {
                    derivedStateOf {
                        navBackStackEntry?.destination?.route
                    }
                }

                val showPlayer by remember(currentRoute) {
                    derivedStateOf {
                        currentRoute?.startsWith(Routes.PLAYER) == true
                    }
                }

                val hideBottomBar by remember(currentRoute) {
                    derivedStateOf {
                        currentRoute?.let { route ->
                            route.startsWith(Routes.ADD_SONGS_TO_PLAYLIST) ||
                            route.startsWith(Routes.ADD_SONGS_TO_TAG) ||
                            route.startsWith(Routes.SETTINGS) ||
                            route.startsWith(Routes.LYRICS) ||
                            route.startsWith(Routes.QUEUE)
                        } == true
                    }
                }

                // 滚动到当前歌曲的请求计数器
                var scrollToCurrentSongRequest by remember { mutableIntStateOf(0) }

                // 获取播放状态以判断是否有正在播放的歌曲
                val playerViewModel: PlayerViewModel = hiltViewModel()
                val playbackState by playerViewModel.playbackState.collectAsState()
                val hasCurrentSong = playbackState.currentSong != null

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!showPlayer && !hideBottomBar) {
                            Column {
                                // MiniPlayer 容器，右上角带跳转按钮
                                Box {
                                    MiniPlayer(
                                        onPlayerClick = {
                                            navController.navigate(Routes.PLAYER)
                                        }
                                    )
                                    // 右上角跳转到当前歌曲按钮
                                    if (hasCurrentSong) {
                                        FloatingActionButton(
                                            onClick = { scrollToCurrentSongRequest++ },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 4.dp, end = 24.dp),
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MyLocation,
                                                contentDescription = "跳转到当前歌曲"
                                            )
                                        }
                                    }
                                }
                                BottomNavBar(navController = navController)
                            }
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        scrollToCurrentSongRequest = scrollToCurrentSongRequest,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}