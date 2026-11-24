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
                // Target offset más alto = más buffer en vivo = menos cortes
                builder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(8_000)
                        .setMinOffsetMs(3_000)
                        .setMaxOffsetMs(25_000)
                        .setMinPlaybackSpeed(0.96f)
                        .setMaxPlaybackSpeed(1.04f)
                        .build()
                )
            } catch (_: Throwable) {
                try {
                    builder.setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder().build()
                    )
                } catch (_: Throwable) {
                }
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
            path.endsWith(".mov") -> MimeTypes.VIDEO_MP4
            else -> null
        }
    }

    fun isLikelyLiveOrRadio(url: String, category: String): Boolean {
        val c = category.lowercase()
        if (c.contains("radio") || c == "tv") return true
        val u = url.lowercase()
        return u.contains("stream") || u.contains("live") || u.contains("ice") ||
            u.contains(".m3u8") ||
            (!u.contains(".mp4") && !u.contains(".webm") && !u.contains(".mkv") && !u.contains(".mov"))
    }
}
