package com.kuntur.mediapro.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Fábrica estable de ExoPlayer para radio, TV (HLS) y películas (MP4).
 */
object StreamPlayerFactory {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36 Kuntur/1.7"

    fun create(context: Context): ExoPlayer {
        return try {
            createConfigured(context)
        } catch (_: Throwable) {
            ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                volume = 1f
            }
        }
    }

    private fun createConfigured(context: Context): ExoPlayer {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setConnectTimeoutMs(25_000)
            .setReadTimeoutMs(35_000)
            .setAllowCrossProtocolRedirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(8_000, 40_000, 1_500, 3_000)
            .build()

        val builder = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)

        // WAKE_LOCK ya está en el manifest; si fallara, se omite
        try {
            builder.setWakeMode(C.WAKE_MODE_NETWORK)
        } catch (_: Throwable) {
            // sin wake mode
        }

        return builder.build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1f
        }
    }

    fun humanError(error: PlaybackException?): String {
        if (error == null) return "Error de reproducción"
        val msg = error.message.orEmpty()
        if (msg.contains("WAKE_LOCK", ignoreCase = true)) {
            return "Falta permiso WAKE_LOCK. Reinstale la app actualizada."
        }
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Sin conexión o red inestable. Reintente."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "El servidor rechazó el stream."
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "Stream no encontrado."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                "Formato de stream no válido."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                "No se puede decodificar en este dispositivo."
            else -> error.message?.take(100) ?: "Error (${error.errorCode})"
        }
    }
}
