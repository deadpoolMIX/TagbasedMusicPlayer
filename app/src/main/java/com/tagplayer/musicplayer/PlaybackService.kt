package com.tagplayer.musicplayer

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 自定义 ForwardingPlayer，拦截上一首/下一首命令
 * 确保直接切换到上一首/下一首歌曲，而不是跳到歌曲开头
 */
@UnstableApi
class CustomForwardingPlayer(
    private val exoPlayer: ExoPlayer
) : ForwardingPlayer(exoPlayer) {

    override fun seekToPreviousMediaItem() {
        // 直接跳到上一首歌曲，不管播放进度
        val currentIndex = exoPlayer.currentMediaItemIndex
        if (currentIndex > 0) {
            exoPlayer.seekToDefaultPosition(currentIndex - 1)
            exoPlayer.play()
        } else if (exoPlayer.repeatMode == Player.REPEAT_MODE_ALL) {
            // 列表循环：跳到最后一首
            val lastIndex = exoPlayer.mediaItemCount - 1
            if (lastIndex >= 0) {
                exoPlayer.seekToDefaultPosition(lastIndex)
                exoPlayer.play()
            }
        }
    }

    override fun seekToNextMediaItem() {
        // 直接跳到下一首歌曲
        val currentIndex = exoPlayer.currentMediaItemIndex
        val nextIndex = currentIndex + 1
        if (nextIndex < exoPlayer.mediaItemCount) {
            exoPlayer.seekToDefaultPosition(nextIndex)
            exoPlayer.play()
        } else if (exoPlayer.repeatMode == Player.REPEAT_MODE_ALL) {
            // 列表循环：跳到第一首
            exoPlayer.seekToDefaultPosition(0)
            exoPlayer.play()
        }
    }
}

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

    private var exoPlayer: ExoPlayer? = null
    private var forwardingPlayer: CustomForwardingPlayer? = null
    private var mediaSession: MediaSession? = null

    companion object {
        // 单例引用，供 MusicPlayer 获取 Player 实例
        @Volatile
        private var instance: PlaybackService? = null

        fun getPlayer(): Player? = instance?.exoPlayer
    }

    override fun onCreate() {
        super.onCreate()

        // 1. 初始化 ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build()

        // 2. 使用自定义 ForwardingPlayer 包装 ExoPlayer
        forwardingPlayer = CustomForwardingPlayer(exoPlayer!!)

        // 3. 创建 MediaSession 并绑定 ForwardingPlayer
        // 系统拿到 Session 就会自动接管通知栏和蓝牙按键
        mediaSession = MediaSession.Builder(this, forwardingPlayer!!)
            .build()

        // 4. 设置单例引用
        instance = this
    }

    // 5. 系统底层（包括 MediaController）连接服务时，必须返回此 Session
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        instance = null
        mediaSession?.release()
        mediaSession = null
        forwardingPlayer = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
}