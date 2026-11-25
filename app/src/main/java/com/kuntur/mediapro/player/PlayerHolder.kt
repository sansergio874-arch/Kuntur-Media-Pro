package com.kuntur.mediapro.player

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer

/**
 * Referencia compartida al ExoPlayer y control del servicio de segundo plano.
 */
object PlayerHolder {
    @Volatile
    var player: ExoPlayer? = null
        private set

    @Volatile
    var isServiceRunning: Boolean = false

    fun attach(player: ExoPlayer) {
        this.player = player
    }

    fun detach() {
        player = null
        isServiceRunning = false
    }

    /** Inicia el servicio de reproducción en segundo plano (notificación de medios). */
    fun ensureService(context: Context) {
        if (isServiceRunning) return
        try {
            val intent = Intent(context.applicationContext, PlaybackService::class.java)
            ContextCompat.startForegroundService(context.applicationContext, intent)
            isServiceRunning = true
        } catch (_: Throwable) {
            try {
                context.applicationContext.startService(
                    Intent(context.applicationContext, PlaybackService::class.java)
                )
                isServiceRunning = true
            } catch (_: Throwable) {
            }
        }
    }
}
