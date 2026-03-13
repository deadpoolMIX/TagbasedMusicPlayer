package com.tagplayer.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tagplayer.musicplayer.ui.components.MiniPlayer
import com.tagplayer.musicplayer.ui.navigation.BottomNavBar
import com.tagplayer.musicplayer.ui.navigation.NavGraph
import com.tagplayer.musicplayer.ui.navigation.Routes
import com.tagplayer.musicplayer.ui.theme.TagPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 移除 enableEdgeToEdge 以避免顶部空白问题
        // enableEdgeToEdge()
        setContent {
            TagPlayerTheme {
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
                            route.startsWith(Routes.ALBUM_LIST) ||
                            route.startsWith(Routes.ARTIST_LIST) ||
                            route.startsWith(Routes.ARTIST_DETAIL) ||
                            route.startsWith(Routes.ALBUM_DETAIL)
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
