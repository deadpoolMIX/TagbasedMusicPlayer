package com.tagplayer.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tagplayer.musicplayer.ui.components.MiniPlayer
import com.tagplayer.musicplayer.ui.navigation.BottomNavBar
import com.tagplayer.musicplayer.ui.navigation.NavGraph
import com.tagplayer.musicplayer.ui.navigation.Routes
import com.tagplayer.musicplayer.ui.settings.viewmodel.SettingsViewModel
import com.tagplayer.musicplayer.ui.settings.viewmodel.ThemeMode
import com.tagplayer.musicplayer.ui.theme.TagPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 权限结果处理，Media3 会自动处理通知 */ }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ 请求通知权限
        requestNotificationPermission()

        // 移除 enableEdgeToEdge 以避免顶部空白问题
        // enableEdgeToEdge()
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

                // 使用 derivedStateOf 确保导航状态变化时正确重组
                val currentRoute by remember(navBackStackEntry) {
                    derivedStateOf {
                        navBackStackEntry?.destination?.route
                    }
                }

                // 判断是否显示播放器（用于控制 MiniPlayer 和底部导航栏）
                val showPlayer by remember(currentRoute) {
                    derivedStateOf {
                        currentRoute?.startsWith(Routes.PLAYER) == true
                    }
                }

                // 判断是否隐藏底部导航栏的页面
                val hideBottomBar by remember(currentRoute) {
                    derivedStateOf {
                        currentRoute?.let { route ->
                            route.startsWith(Routes.ADD_SONGS_TO_PLAYLIST) ||
                            route.startsWith(Routes.ADD_SONGS_TO_TAG) ||
                            route.startsWith(Routes.ARTIST_DETAIL) ||
                            route.startsWith(Routes.SETTINGS) ||
                            route.startsWith(Routes.LYRICS) ||
                            route.startsWith(Routes.QUEUE)
                        } == true
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // 只在非播放器页面且非隐藏底部栏页面显示底部栏和 MiniPlayer
                        if (!showPlayer && !hideBottomBar) {
                            Column {
                                MiniPlayer(
                                    onPlayerClick = {
                                        navController.navigate(Routes.PLAYER)
                                    }
                                )
                                BottomNavBar(navController = navController)
                            }
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
