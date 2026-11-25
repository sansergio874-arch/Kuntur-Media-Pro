package com.kuntur.mediapro.player

import androidx.media3.exoplayer.ExoPlayer

/**
 * Referencia compartida al ExoPlayer gestionado por [PlaybackService].
 * Permite que la UI de Compose controle el mismo player que corre en segundo plano.
 */
object PlayerHolder {
    @Volatile
    var player: ExoPlayer? = null
        private set

    @Volatile
    var isServiceRunning: Boolean = false
        private set

    fun attach(player: ExoPlayer) {
        this.player = player
        isServiceRunning = true
    }

    fun detach() {
        player = null
        isServiceRunning = false
    }
}
