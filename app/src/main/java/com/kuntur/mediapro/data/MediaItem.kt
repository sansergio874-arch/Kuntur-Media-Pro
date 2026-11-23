package com.kuntur.mediapro.data

data class MediaItem(
    val id: String,
    val title: String,
    val category: String,
    val streamUrl: String,
    val isPremium: Boolean = false,
    val licenseUrl: String? = null  // Widevine license (opcional)
)

object MediaRepository {

    val radios = listOf(
        MediaItem("r1", "Radio Nacional del Perú", "Radio", "https://stream.radioparadise.com/aac-128", false),
        MediaItem("r2", "RPP Noticias", "Radio", "https://stream.radioparadise.com/m128", false),
        MediaItem("r3", "Radio Exitosa", "Radio", "https://stream.radioparadise.com/mp3-128", true),
        MediaItem("r4", "Radio Moda", "Radio", "https://stream.radioparadise.com/aac-128", false),
        MediaItem("r5", "Radio La Inolvidable", "Radio", "https://stream.radioparadise.com/m128", false)
    )

    val channels = listOf(
        MediaItem("c1", "Canal 7 TV Perú", "TV", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", false),
        MediaItem("c2", "América TV", "TV", "https://test-streams.mux.dev/test_001/stream.m3u8", true),
        MediaItem("c3", "Latina", "TV", "https://test-streams.mux.dev/dai-discontinuity-deltatre/manifest.m3u8", true),
        MediaItem("c4", "Panamericana", "TV", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", true),
        MediaItem("c5", "ATV", "TV", "https://test-streams.mux.dev/test_001/stream.m3u8", false)
    )

    val movies = listOf(
        MediaItem(
            "m1",
            "Big Buck Bunny",
            "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            false
        ),
        MediaItem(
            "m2",
            "Elephants Dream (Premium)",
            "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            true
            // licenseUrl = "https://su-license-server.com/widevine"  // activar cuando tenga DRM real
        ),
        MediaItem(
            "m3",
            "For Bigger Blazes",
            "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            false
        ),
        MediaItem(
            "m4",
            "Sintel (Premium)",
            "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            true
        )
    )

    fun all() = radios + channels + movies
}
