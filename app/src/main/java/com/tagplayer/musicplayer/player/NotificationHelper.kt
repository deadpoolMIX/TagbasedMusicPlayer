package com.tagplayer.musicplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.tagplayer.musicplayer.MainActivity
import com.tagplayer.musicplayer.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY_PAUSE = "com.tagplayer.musicplayer.ACTION_PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.tagplayer.musicplayer.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.tagplayer.musicplayer.ACTION_NEXT"
        const val ACTION_STOP = "com.tagplayer.musicplayer.ACTION_STOP"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放控制"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        title: String,
        artist: String,
        albumId: Long,
        isPlaying: Boolean
    ): Notification {
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_player)

        // 设置歌曲信息
        remoteViews.setTextViewText(R.id.tv_title, title)
        remoteViews.setTextViewText(R.id.tv_artist, artist)

        // 设置播放/暂停按钮图标
        remoteViews.setImageViewResource(
            R.id.btn_play_pause,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )

        // 设置歌曲封面
        val coverBitmap = loadAlbumArt(albumId)
        if (coverBitmap != null) {
            remoteViews.setImageViewBitmap(R.id.iv_cover, coverBitmap)
        } else {
            remoteViews.setImageViewResource(R.id.iv_cover, android.R.drawable.ic_menu_gallery)
        }

        // 设置按钮点击事件
        remoteViews.setOnClickPendingIntent(R.id.btn_previous, createPendingIntent(ACTION_PREVIOUS))
        remoteViews.setOnClickPendingIntent(R.id.btn_play_pause, createPendingIntent(ACTION_PLAY_PAUSE))
        remoteViews.setOnClickPendingIntent(R.id.btn_next, createPendingIntent(ACTION_NEXT))

        // 点击通知打开应用
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadAlbumArt(albumId: Long): Bitmap? {
        if (albumId <= 0) return null
        return try {
            val uri = Uri.parse("content://media/external/audio/albumart/$albumId")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun showNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}