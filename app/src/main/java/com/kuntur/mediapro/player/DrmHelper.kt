package com.kuntur.mediapro.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes

object DrmHelper {

    fun createProtectedMediaItem(
        videoUrl: String,
        licenseUrl: String,
        title: String? = null,
        authToken: String? = null
    ): MediaItem {
        val drmBuilder = DrmConfiguration.Builder(C.WIDEVINE_UUID)
            .setLicenseUri(licenseUrl)
            .setForceDefaultLicenseUri(true)
        if (!authToken.isNullOrBlank()) {
            drmBuilder.setLicenseRequestHeaders(
                mapOf("Authorization" to "Bearer $authToken")
            )
        }
        return MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(guessMime(videoUrl))
            .setDrmConfiguration(drmBuilder.build())
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(title ?: "Kuntur Media Pro").build()
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
                MediaMetadata.Builder().setTitle(title ?: "Kuntur Media Pro").build()
            )
        if (isLive) {
            try {
                builder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder().build()
                )
            } catch (_: Throwable) {
                // ignorar si no está disponible
            }
        }
        return builder.build()
    }

    fun guessMime(url: String): String? {
        val path = (Uri.parse(url).path ?: url).lowercase()
        return when {
            path.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            path.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            path.endsWith(".mp3") || path.contains("mp3") -> MimeTypes.AUDIO_MPEG
            path.endsWith(".aac") || path.contains("/aac") -> MimeTypes.AUDIO_AAC
            path.endsWith(".mp4") || path.endsWith(".m4a") -> MimeTypes.VIDEO_MP4
            path.endsWith(".webm") -> MimeTypes.VIDEO_WEBM
            else -> null
        }
    }

    fun isLikelyLiveOrRadio(url: String, category: String): Boolean {
        val c = category.lowercase()
        if (c.contains("radio") || c == "tv") return true
        val u = url.lowercase()
        return u.contains("stream") || u.contains("live") || u.contains("ice") ||
            (!u.contains(".mp4") && !u.contains(".webm") && !u.contains(".mkv"))
    }
}
