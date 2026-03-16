package com.tagplayer.musicplayer

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 媒体会话服务
 *
 * 遵循 Media3 官方规范：
 * - 系统自动识别此 Service 并生成通知栏播放器
 * - 蓝牙耳机按键自动路由到 MediaSession
 * - 无需手动编写任何 Notification 代码
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    companion object {
        // 单例引用，供 MusicPlayer 获取 Player 实例
        @Volatile
        private var instance: PlaybackService? = null

        fun getPlayer(): Player? = instance?.player
    }

    override fun onCreate() {
        super.onCreate()

        // 1. 初始化 ExoPlayer
        player = ExoPlayer.Builder(this).build()

        // 2. 创建 MediaSession 并绑定 Player
        // 系统拿到 Session 就会自动接管通知栏和蓝牙按键
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(object : MediaSession.Callback {
                // 使用默认回调，允许所有媒体控制命令
            })
            .build()

        // 3. 设置单例引用
        instance = this
    }

    // 4. 系统底层（包括 MediaController）连接服务时，必须返回此 Session
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        instance = null
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
}