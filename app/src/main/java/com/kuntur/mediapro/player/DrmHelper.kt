package com.kuntur.mediapro.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration

/**
 * Helper para contenido protegido con Widevine DRM.
 * Requiere un License Server real (Cloudflare Stream, Mux, Axinom, etc.).
 */
object DrmHelper {

    /**
     * Crea un MediaItem con protección Widevine.
     *
     * @param videoUrl URL del stream cifrado (DASH .mpd o HLS fMP4)
     * @param licenseUrl URL del servidor de licencias Widevine
     * @param authToken Token opcional de autorización (Bearer)
     */
    fun createProtectedMediaItem(
        videoUrl: String,
        licenseUrl: String,
        authToken: String? = null
    ): MediaItem {
        val builder = DrmConfiguration.Builder(C.WIDEVINE_UUID)
            .setLicenseUri(licenseUrl)
            .setForceDefaultLicenseUri(true)

        if (!authToken.isNullOrBlank()) {
            builder.setLicenseRequestHeaders(
                mapOf("Authorization" to "Bearer $authToken")
            )
        }

        return MediaItem.Builder()
            .setUri(videoUrl)
            .setDrmConfiguration(builder.build())
            .build()
    }

    /**
     * MediaItem normal (sin DRM) para radios y contenido libre.
     */
    fun createClearMediaItem(videoUrl: String): MediaItem {
        return MediaItem.fromUri(videoUrl)
    }
}
