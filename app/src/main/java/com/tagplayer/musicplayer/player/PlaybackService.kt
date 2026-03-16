package com.tagplayer.musicplayer.player

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var positionUpdateJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var musicPlayer: MusicPlayer

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        val player = musicPlayer.initializePlayer()

        mediaSession = MediaSession.Builder(this, player)
            .build()

        startPositionUpdates()
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
}
