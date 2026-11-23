package com.kuntur.mediapro.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

/**
 * Construcción de MediaItem para radio, HLS, DASH, MP4 y DRM Widevine.
 */
@UnstableApi
object DrmHelper {

    fun createProtectedMediaItem(
        videoUrl: String,
        licenseUrl: String,
        title: String? = null,
        authToken: String? = null
    ): MediaItem {
        val drm = DrmConfiguration.Builder(C.WIDEVINE_UUID)
            .setLicenseUri(licenseUrl)
            .setForceDefaultLicenseUri(true)
            .apply {
                if (!authToken.isNullOrBlank()) {
                    setLicenseRequestHeaders(mapOf("Authorization" to "Bearer $authToken"))
                }
            }
            .build()

        return MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(guessMime(videoUrl))
            .setDrmConfiguration(drm)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title ?: "Kuntur Media Pro")
                    .build()
            )
            .build()
    }

    fun createClearMediaItem(
        videoUrl: String,
        title: String? = null,
        isLive: Boolean = false
    ): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(guessMime(videoUrl))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title ?: "Kuntur Media Pro")
                    .setIsBrowsable(false)
                    .build()
            )

        // Live / radio: sin clipping; VOD normal
        if (isLive) {
            builder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setMaxPlaybackSpeed(1.02f)
                    .setMinPlaybackSpeed(0.98f)
                    .build()
            )
        }

        return builder.build()
    }

    /** Detecta MIME a partir de la URL para que Media3 elija el extractor correcto. */
    fun guessMime(url: String): String? {
        val path = Uri.parse(url).path?.lowercase() ?: url.lowercase()
        return when {
            path.contains(".m3u8") || path.endsWith("m3u8") -> MimeTypes.APPLICATION_M3U8
            path.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            path.contains(".ism") || path.contains("manifest") && path.contains("ism") ->
                MimeTypes.APPLICATION_SS
            path.endsWith(".mp3") || path.contains("mp3") && !path.contains("mp4") ->
                MimeTypes.AUDIO_MPEG
            path.endsWith(".aac") || path.contains("/aac") -> MimeTypes.AUDIO_AAC
            path.endsWith(".mp4") || path.endsWith(".m4a") -> MimeTypes.VIDEO_MP4
            path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
            path.endsWith(".ts") -> MimeTypes.VIDEO_MP2T
            else -> null // Media3 infiere solo
        }
    }

    fun isLikelyLiveOrRadio(url: String, category: String): Boolean {
        val c = category.lowercase()
        if (c.contains("radio") || c.contains("tv")) return true
        val u = url.lowercase()
        return u.contains("stream") || u.contains("live") || u.contains("icy") ||
            u.contains("icecast") || u.contains("shoutcast") ||
            (!u.contains(".mp4") && !u.contains(".webm") && !u.contains(".mkv"))
    }
}
