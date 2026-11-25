package com.kuntur.mediapro.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.kuntur.mediapro.MainActivity

/**
 * Servicio en primer plano de tipo mediaPlayback.
 * Mantiene el ExoPlayer vivo cuando la app pasa a segundo plano
 * y expone controles en la notificación del sistema.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val exo = try {
            StreamPlayerFactory.create(this)
        } catch (_: Throwable) {
            ExoPlayer.Builder(this).build().apply {
                playWhenReady = true
                volume = 1f
            }
        }

        try {
            exo.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
        } catch (_: Throwable) {
        }

        try {
            exo.setWakeMode(C.WAKE_MODE_NETWORK)
        } catch (_: Throwable) {
        }

        player = exo
        PlayerHolder.attach(exo)

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, exo)
            .setSessionActivity(sessionActivity)
            .setId("kuntur_media_session")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0 ||
            p.playbackState == Player.STATE_ENDED || p.playbackState == Player.STATE_IDLE
        ) {
            stopSelf()
        }
        // Si está reproduciendo, el servicio permanece activo en segundo plano.
    }

    override fun onDestroy() {
        mediaSession?.run {
            try {
                player.release()
            } catch (_: Throwable) {
            }
            release()
            mediaSession = null
        }
        player = null
        PlayerHolder.detach()
        super.onDestroy()
    }
}
