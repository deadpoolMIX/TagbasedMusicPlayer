package com.tagplayer.musicplayer.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var musicPlayer: MusicPlayer

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHelper.ACTION_PLAY_PAUSE -> {
                musicPlayer.playPauseToggle()
            }
            NotificationHelper.ACTION_PREVIOUS -> {
                musicPlayer.playPrevious()
            }
            NotificationHelper.ACTION_NEXT -> {
                musicPlayer.playNext()
            }
            NotificationHelper.ACTION_STOP -> {
                musicPlayer.pause()
            }
        }
    }
}