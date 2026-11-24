package com.kuntur.mediapro.data

data class MediaItem(
    val id: String,
    val title: String,
    val category: String,
    val streamUrl: String,
    val isPremium: Boolean = false,
    val licenseUrl: String? = null,
    /** URL del logo / carátula (opcional) */
    val logoUrl: String? = null
)

/**
 * Fuentes públicas legales de demostración con logos.
 */
object MediaRepository {

    // Logos públicos conocidos / CDN estables
    private const val LOGO_RP = "https://www.radioparadise.com/graphics/logo_flat_256.png"
    private const val LOGO_SOMA = "https://somafm.com/img3/groovesalad-400.jpg"
    private const val LOGO_SOMA_BASE = "https://somafm.com/img3/"

    val radios = listOf(
        MediaItem("r1", "Radio Paradise AAC", "Radio",
            "https://stream.radioparadise.com/aac-128", false, null, LOGO_RP),
        MediaItem("r2", "Radio Paradise MP3", "Radio",
            "https://stream.radioparadise.com/mp3-128", false, null, LOGO_RP),
        MediaItem("r3", "Radio Paradise Mellow", "Radio",
            "https://stream.radioparadise.com/m128", false, null, LOGO_RP),
        MediaItem("r4", "Radio Paradise Rock", "Radio",
            "https://stream.radioparadise.com/rock-128", false, null, LOGO_RP),
        MediaItem("r5", "SomaFM Groove Salad", "Radio",
            "https://ice1.somafm.com/groovesalad-128-mp3", false, null,
            "${LOGO_SOMA_BASE}groovesalad-400.jpg"),
        MediaItem("r6", "SomaFM Drone Zone", "Radio",
            "https://ice1.somafm.com/dronezone-128-mp3", false, null,
            "${LOGO_SOMA_BASE}dronezone-400.jpg"),
        MediaItem("r7", "SomaFM Indie Pop", "Radio",
            "https://ice1.somafm.com/indiepop-128-mp3", false, null,
            "${LOGO_SOMA_BASE}indiepop400.jpg"),
        MediaItem("r8", "SomaFM Space Station", "Radio",
            "https://ice1.somafm.com/spacestation-128-mp3", false, null,
            "${LOGO_SOMA_BASE}spacestation-400.jpg"),
        MediaItem("r9", "SomaFM Folk Forward", "Radio",
            "https://ice1.somafm.com/folkfwd-128-mp3", false, null,
            "${LOGO_SOMA_BASE}folkfwd400.jpg"),
        MediaItem("r10", "SomaFM Beat Blender", "Radio",
            "https://ice1.somafm.com/beatblender-128-mp3", true, null,
            "${LOGO_SOMA_BASE}beatblender400.jpg"),
        MediaItem("r11", "SomaFM Metal Detector", "Radio",
            "https://ice1.somafm.com/metal-128-mp3", true, null,
            "${LOGO_SOMA_BASE}metal-400.jpg"),
        MediaItem("r12", "SomaFM Left Coast", "Radio",
            "https://ice1.somafm.com/leftcoast-128-mp3", false, null,
            "${LOGO_SOMA_BASE}leftcoast400.jpg"),
        MediaItem("r13", "SomaFM Fluid", "Radio",
            "https://ice1.somafm.com/fluid-128-mp3", false, null,
            "${LOGO_SOMA_BASE}fluid400.jpg"),
        MediaItem("r14", "SomaFM DEF CON", "Radio",
            "https://ice1.somafm.com/defcon-128-mp3", true, null,
            "${LOGO_SOMA_BASE}defcon400.jpg"),
        MediaItem("r15", "SomaFM Deep Space One", "Radio",
            "https://ice1.somafm.com/deepspaceone-128-mp3", false, null,
            "${LOGO_SOMA_BASE}deepspaceone-400.jpg"),
        MediaItem("r16", "SomaFM Lush", "Radio",
            "https://ice1.somafm.com/lush-128-mp3", false, null,
            "${LOGO_SOMA_BASE}lush-400.jpg"),
        MediaItem("r17", "SomaFM PopTron", "Radio",
            "https://ice1.somafm.com/poptron-128-mp3", true, null,
            "${LOGO_SOMA_BASE}poptron400.jpg"),
        MediaItem("r18", "SomaFM ThistleRadio", "Radio",
            "https://ice1.somafm.com/thistle-128-mp3", false, null,
            "${LOGO_SOMA_BASE}thistle-400.jpg")
    )

    // Logos genéricos / demos para TV de prueba
    private const val LOGO_TV = "https://cdn-icons-png.flaticon.com/512/3179/3179068.png"
    private const val LOGO_HLS = "https://cdn-icons-png.flaticon.com/512/3179/3179068.png"

    val channels = listOf(
        MediaItem("c1", "Mux Demo HLS", "TV",
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", false, null,
            "https://www.mux.com/images/mux-logo-black.png"),
        MediaItem("c2", "Mux Test 001", "TV",
            "https://test-streams.mux.dev/test_001/stream.m3u8", false, null,
            "https://www.mux.com/images/mux-logo-black.png"),
        MediaItem("c3", "Apple BipBop Basic", "TV",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            false, null, "https://cdn-icons-png.flaticon.com/512/0/747.png"),
        MediaItem("c4", "Apple BipBop 16x9", "TV",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            false, null, "https://cdn-icons-png.flaticon.com/512/0/747.png"),
        MediaItem("c5", "Apple Advanced Stream", "TV",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/adv_dv_atmos/main.m3u8",
            true, null, "https://cdn-icons-png.flaticon.com/512/0/747.png"),
        MediaItem("c6", "Mux DAI Demo", "TV",
            "https://test-streams.mux.dev/dai-discontinuity-deltatre/manifest.m3u8",
            true, null, "https://www.mux.com/images/mux-logo-black.png"),
        MediaItem("c7", "Akamai HLS Test", "TV",
            "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
            false, null, "https://cdn-icons-png.flaticon.com/512/3179/3179068.png"),
        MediaItem("c8", "Cloudflare Sample", "TV",
            "https://customer-m033z5x00ks6nunl.cloudflarestream.com/b236bde30eb07b9cba1e031208885754/manifest/video.m3u8",
            true, null, "https://cdn-icons-png.flaticon.com/512/5968/5968705.png")
    )

    // Carátulas de películas de muestra (Google sample + iconos)
    private const val POSTER_BBB = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/220px-Big_buck_bunny_poster_big.jpg"
    private const val POSTER_ED = "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e8/Elephants_Dream_s5_both.jpg/220px-Elephants_Dream_s5_both.jpg"
    private const val POSTER_SINTEL = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/Sintel_poster.jpg/220px-Sintel_poster.jpg"
    private const val POSTER_TOS = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Tears_of_Steel_poster.jpg/220px-Tears_of_Steel_poster.jpg"

    val movies = listOf(
        MediaItem("m1", "Big Buck Bunny", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            false, null, POSTER_BBB),
        MediaItem("m2", "Elephants Dream", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            false, null, POSTER_ED),
        MediaItem("m3", "Sintel", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            false, null, POSTER_SINTEL),
        MediaItem("m4", "Tears of Steel", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            false, null, POSTER_TOS),
        MediaItem("m5", "For Bigger Blazes", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            false, null, "https://cdn-icons-png.flaticon.com/512/3163/3163478.png"),
        MediaItem("m6", "For Bigger Escape", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            false, null, "https://cdn-icons-png.flaticon.com/512/3163/3163478.png"),
        MediaItem("m7", "For Bigger Fun", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            false, null, "https://cdn-icons-png.flaticon.com/512/3163/3163478.png"),
        MediaItem("m8", "For Bigger Joyrides", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            true, null, "https://cdn-icons-png.flaticon.com/512/3163/3163478.png"),
        MediaItem("m9", "For Bigger Meltdowns", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            false, null, "https://cdn-icons-png.flaticon.com/512/3163/3163478.png"),
        MediaItem("m10", "Subaru Outback", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            true, null, "https://cdn-icons-png.flaticon.com/512/744/744465.png"),
        MediaItem("m11", "Volkswagen GTI", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4",
            true, null, "https://cdn-icons-png.flaticon.com/512/744/744465.png"),
        MediaItem("m12", "What car?", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
            false, null, "https://cdn-icons-png.flaticon.com/512/744/744465.png"),
        MediaItem("m13", "We Are Going On Bullrun", "Película",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            true, null, "https://cdn-icons-png.flaticon.com/512/744/744465.png")
    )

    fun all() = radios + channels + movies
}
