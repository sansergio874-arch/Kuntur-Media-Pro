package com.kuntur.mediapro.data

data class MediaItem(
    val id: String,
    val title: String,
    val category: String,
    val streamUrl: String,
    val isPremium: Boolean = false,
    val licenseUrl: String? = null
)

/**
 * Fuentes públicas de demostración / abiertas.
 * Reemplace por URLs propias con derechos cuando las tenga.
 */
object MediaRepository {

    val radios = listOf(
        MediaItem("r1", "Radio Paradise (AAC)", "Radio", "https://stream.radioparadise.com/aac-128", false),
        MediaItem("r2", "Radio Paradise (MP3)", "Radio", "https://stream.radioparadise.com/mp3-128", false),
        MediaItem("r3", "Radio Paradise Mellow", "Radio", "https://stream.radioparadise.com/m128", false),
        MediaItem("r4", "SomaFM Groove Salad", "Radio", "https://ice1.somafm.com/groovesalad-128-mp3", false),
        MediaItem("r5", "SomaFM Drone Zone", "Radio", "https://ice1.somafm.com/dronezone-128-mp3", false),
        MediaItem("r6", "SomaFM Indie Pop", "Radio", "https://ice1.somafm.com/indiepop-128-mp3", true),
        MediaItem("r7", "SomaFM Metal Detector", "Radio", "https://ice1.somafm.com/metal-128-mp3", true),
        MediaItem("r8", "SomaFM Space Station", "Radio", "https://ice1.somafm.com/spacestation-128-mp3", false),
        MediaItem("r9", "SomaFM Beat Blender", "Radio", "https://ice1.somafm.com/beatblender-128-mp3", true),
        MediaItem("r10", "SomaFM Folk Forward", "Radio", "https://ice1.somafm.com/folkfwd-128-mp3", false)
    )

    val channels = listOf(
        MediaItem("c1", "Mux Demo HLS", "TV", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", false),
        MediaItem("c2", "Mux Test Stream", "TV", "https://test-streams.mux.dev/test_001/stream.m3u8", false),
        MediaItem("c3", "Mux DAI Demo", "TV", "https://test-streams.mux.dev/dai-discontinuity-deltatre/manifest.m3u8", true),
        MediaItem("c4", "Apple HLS Basic", "TV", "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8", false),
        MediaItem("c5", "Apple HLS Advanced", "TV", "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8", true),
        MediaItem("c6", "Cloudflare Stream Sample", "TV", "https://customer-m033z5x00ks6nunl.cloudflarestream.com/b236bde30eb07b9cba1e031208885754/manifest/video.m3u8", true)
    )

    val movies = listOf(
        MediaItem("m1", "Big Buck Bunny", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", false),
        MediaItem("m2", "Elephants Dream", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", false),
        MediaItem("m3", "For Bigger Blazes", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", false),
        MediaItem("m4", "For Bigger Escape", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", true),
        MediaItem("m5", "For Bigger Fun", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", false),
        MediaItem("m6", "For Bigger Joyrides", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4", true),
        MediaItem("m7", "Sintel", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", false),
        MediaItem("m8", "Subaru Outback", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4", true),
        MediaItem("m9", "Tears of Steel", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", false),
        MediaItem("m10", "Volkswagen GTI", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4", true)
    )

    fun all() = radios + channels + movies
}
