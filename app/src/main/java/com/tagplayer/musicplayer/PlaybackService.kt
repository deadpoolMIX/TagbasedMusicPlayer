package com.tagplayer.musicplayer

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures

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
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    // 拦截上一首/下一首命令，直接跳转而不跳到歌曲开头
                    when (playerCommand) {
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                            // 直接跳到上一首
                            val currentIndex = player?.currentMediaItemIndex ?: 0
                            if (currentIndex > 0) {
                                player?.seekToDefaultPosition(currentIndex - 1)
                                player?.play()
                            } else {
                                // 列表循环：跳到最后一首
                                val lastIndex = (player?.mediaItemCount ?: 1) - 1
                                if (lastIndex >= 0 && player?.repeatMode == Player.REPEAT_MODE_ALL) {
                                    player?.seekToDefaultPosition(lastIndex)
                                    player?.play()
                                }
                            }
                            return SessionResult.RESULT_SUCCESS
                        }
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                            // 直接跳到下一首
                            val currentIndex = player?.currentMediaItemIndex ?: 0
                            val nextIndex = currentIndex + 1
                            if (nextIndex < (player?.mediaItemCount ?: 0)) {
                                player?.seekToDefaultPosition(nextIndex)
                                player?.play()
                            } else if (player?.repeatMode == Player.REPEAT_MODE_ALL) {
                                // 列表循环：跳到第一首
                                player?.seekToDefaultPosition(0)
                                player?.play()
                            }
                            return SessionResult.RESULT_SUCCESS
                        }
                    }
                    return super.onPlayerCommandRequest(session, controller, playerCommand)
                }
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