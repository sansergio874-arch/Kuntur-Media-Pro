package com.kuntur.mediapro.data

data class MediaItem(
    val id: String,
    val title: String,
    val category: String,
    val streamUrl: String,
    val isPremium: Boolean = false,
    val logoUrl: String? = null
)

/**
 * Catalogo publico funcional (streams abiertos / de prueba oficiales).
 */
object MediaRepository {

    private const val RP = "https://www.radioparadise.com/graphics/logo_flat_256.png"
    private const val SOMA = "https://somafm.com/img3/"

    val radios = listOf(
        MediaItem("r1", "Radio Paradise AAC", "Radio", "https://stream.radioparadise.com/aac-128", false, RP),
        MediaItem("r2", "Radio Paradise MP3", "Radio", "https://stream.radioparadise.com/mp3-128", false, RP),
        MediaItem("r3", "Radio Paradise Mellow", "Radio", "https://stream.radioparadise.com/m128", false, RP),
        MediaItem("r4", "Radio Paradise Rock", "Radio", "https://stream.radioparadise.com/rock-128", false, RP),
        MediaItem("r5", "SomaFM Groove Salad", "Radio", "https://ice1.somafm.com/groovesalad-128-mp3", false, "${SOMA}groovesalad-400.jpg"),
        MediaItem("r6", "SomaFM Drone Zone", "Radio", "https://ice1.somafm.com/dronezone-128-mp3", false, "${SOMA}dronezone-400.jpg"),
        MediaItem("r7", "SomaFM Space Station", "Radio", "https://ice1.somafm.com/spacestation-128-mp3", false, "${SOMA}spacestation-400.jpg"),
        MediaItem("r8", "SomaFM Indie Pop", "Radio", "https://ice1.somafm.com/indiepop-128-mp3", false, "${SOMA}indiepop400.jpg"),
        MediaItem("r9", "SomaFM Folk Forward", "Radio", "https://ice1.somafm.com/folkfwd-128-mp3", false, "${SOMA}folkfwd400.jpg"),
        MediaItem("r10", "SomaFM Left Coast", "Radio", "https://ice1.somafm.com/leftcoast-128-mp3", false, "${SOMA}leftcoast400.jpg"),
        MediaItem("r11", "SomaFM Fluid", "Radio", "https://ice1.somafm.com/fluid-128-mp3", false, "${SOMA}fluid400.jpg"),
        MediaItem("r12", "SomaFM Lush", "Radio", "https://ice1.somafm.com/lush-128-mp3", false, "${SOMA}lush-400.jpg"),
        MediaItem("r13", "SomaFM Beat Blender", "Radio", "https://ice1.somafm.com/beatblender-128-mp3", true, "${SOMA}beatblender400.jpg"),
        MediaItem("r14", "SomaFM Metal Detector", "Radio", "https://ice1.somafm.com/metal-128-mp3", true, "${SOMA}metal-400.jpg")
    )

    val channels = listOf(
        MediaItem(
            "c1", "Mux Demo HLS", "TV",
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", false,
            "https://www.mux.com/images/mux-logo-black.png"
        ),
        MediaItem(
            "c2", "Mux Test 001", "TV",
            "https://test-streams.mux.dev/test_001/stream.m3u8", false,
            "https://www.mux.com/images/mux-logo-black.png"
        ),
        MediaItem(
            "c3", "Apple BipBop", "TV",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            false, null
        ),
        MediaItem(
            "c4", "Apple BipBop 16x9", "TV",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            false, null
        ),
        MediaItem(
            "c5", "Akamai HLS Live", "TV",
            "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
            false, null
        ),
        MediaItem(
            "c6", "Mux DAI Demo", "TV",
            "https://test-streams.mux.dev/dai-discontinuity-deltatre/manifest.m3u8",
            true, "https://www.mux.com/images/mux-logo-black.png"
        )
    )

    private const val GTV = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/"

    val movies = listOf(
        MediaItem("m1", "Big Buck Bunny", "Película", "${GTV}BigBuckBunny.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/220px-Big_buck_bunny_poster_big.jpg"),
        MediaItem("m2", "Elephants Dream", "Película", "${GTV}ElephantsDream.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e8/Elephants_Dream_s5_both.jpg/220px-Elephants_Dream_s5_both.jpg"),
        MediaItem("m3", "Sintel", "Película", "${GTV}Sintel.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/Sintel_poster.jpg/220px-Sintel_poster.jpg"),
        MediaItem("m4", "Tears of Steel", "Película", "${GTV}TearsOfSteel.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Tears_of_Steel_poster.jpg/220px-Tears_of_Steel_poster.jpg"),
        MediaItem("m5", "For Bigger Blazes", "Película", "${GTV}ForBiggerBlazes.mp4", false, null),
        MediaItem("m6", "For Bigger Escape", "Película", "${GTV}ForBiggerEscapes.mp4", false, null),
        MediaItem("m7", "For Bigger Fun", "Película", "${GTV}ForBiggerFun.mp4", false, null),
        MediaItem("m8", "For Bigger Joyrides", "Película", "${GTV}ForBiggerJoyrides.mp4", true, null),
        MediaItem("m9", "For Bigger Meltdowns", "Película", "${GTV}ForBiggerMeltdowns.mp4", false, null),
        MediaItem("m10", "Subaru Outback", "Película", "${GTV}SubaruOutbackOnStreetAndDirt.mp4", true, null)
    )

    fun all() = radios + channels + movies
}
