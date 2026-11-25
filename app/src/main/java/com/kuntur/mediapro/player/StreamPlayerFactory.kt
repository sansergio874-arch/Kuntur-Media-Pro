package com.kuntur.mediapro.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLivePlaybackSpeedControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * ExoPlayer con buffer amplio, reproducción estable y sin reintentos automáticos.
 */
object StreamPlayerFactory {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36 Kuntur/2.6"

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
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(45_000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

        // No repetir solicitudes fallidas: informar el error al usuario.
        val errorPolicy: LoadErrorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy(0) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                return C.TIME_UNSET
            }

            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return 0
            }
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(errorPolicy)

        // Buffer amplio para reducir rebuffering (cortes)
        // minBuffer, maxBuffer, bufferForPlayback, bufferForPlaybackAfterRebuffer
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs */ 25_000,
                /* maxBufferMs */ 90_000,
                /* bufferForPlaybackMs */ 3_500,
                /* bufferForPlaybackAfterRebufferMs */ 8_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val liveSpeed = DefaultLivePlaybackSpeedControl.Builder()
            .setFallbackMaxPlaybackSpeed(1.04f)
            .setFallbackMinPlaybackSpeed(0.96f)
            .build()

        val builder = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setLivePlaybackSpeedControl(liveSpeed)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)

        try {
            builder.setWakeMode(C.WAKE_MODE_NETWORK)
        } catch (_: Throwable) {
        }

        return builder.build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1f
            try {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    /* handleAudioFocus = */ true
                )
            } catch (_: Throwable) {
            }
            // Reduce saltos de calidad bruscos en adaptativo
            try {
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setForceHighestSupportedBitrate(false)
                    .build()
            } catch (_: Throwable) {
            }
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
                "No se pudo conectar con la fuente. Compruebe la conexión o el enlace."
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
