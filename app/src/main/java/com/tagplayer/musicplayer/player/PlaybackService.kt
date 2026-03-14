package com.tagplayer.musicplayer.player

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.tagplayer.musicplayer.player.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var positionUpdateJob: Job? = null
    private var notificationJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var musicPlayer: MusicPlayer

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        val player = musicPlayer.initializePlayer()

        mediaSession = MediaSession.Builder(this, player)
            .build()

        startPositionUpdates()
        startNotificationUpdates()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady == false || player?.playbackState == androidx.media3.common.Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        positionUpdateJob?.cancel()
        notificationJob?.cancel()
        notificationHelper.hideNotification()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun startPositionUpdates() {
        positionUpdateJob = serviceScope.launch {
            while (isActive) {
                musicPlayer.updatePosition()
                delay(1000)
            }
        }
    }

    @UnstableApi
    private fun startNotificationUpdates() {
        notificationJob = serviceScope.launch {
            musicPlayer.playbackState.collectLatest { state ->
                val song = state.currentSong
                if (song != null) {
                    val notification = notificationHelper.createNotification(
                        title = song.title,
                        artist = song.artist,
                        albumId = song.albumId,
                        isPlaying = state.isPlaying
                    )
                    notificationHelper.showNotification(notification)
                } else {
                    notificationHelper.hideNotification()
                }
            }
        }
    }
}
