package com.zero.health.service

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.zero.health.helper.NotifyHelper

class HealthService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        NotifyHelper.showMediaNotification(this, notifyId = 0x10080)
        player = ExoPlayer.Builder(this).build()

        val callback = object : MediaLibrarySession.Callback {
            override fun onGetItem(session: MediaLibrarySession,
                                   controller: MediaSession.ControllerInfo,
                                   mediaId: String): ListenableFuture<LibraryResult<MediaItem>> {
                val item = MediaItem.Builder().setMediaId(mediaId).build()
                return Futures.immediateFuture(LibraryResult.ofItem(item, null))
            }
        }
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }
}