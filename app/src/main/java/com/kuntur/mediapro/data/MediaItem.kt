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
 * Emisoras peruanas con streams publicos conocidos,
 * canales de prueba HLS, y peliculas de dominio abierto / muestras oficiales.
 * Los canales comerciales de TV (America, Latina, etc.) requieren URLs
 * con derechos; no se incluyen streams no autorizados.
 */
object MediaRepository {

    val radios = listOf(
        MediaItem(
            "r1", "Radio Nacional del Perú", "Radio",
            "https://cdnhd.iblups.com/hls/0773874174fd4eba8bb9eff741d190dc.m3u8",
            false, null
        ),
        MediaItem(
            "r2", "PBO Radio 91.9", "Radio",
            "https://stream.radiojar.com/2fse67zuv8hvv",
            false, null
        ),
        MediaItem(
            "r3", "Radio Disney Perú", "Radio",
            "https://24403.live.streamtheworld.com/DISNEY_PER_LM.mp3",
            false, null
        ),
        MediaItem(
            "r4", "La Inolvidable (CRP)", "Radio",
            "https://27163.live.streamtheworld.com/CRP_INC.mp3",
            false, null
        ),
        MediaItem(
            "r5", "Perú Folk Radio", "Radio",
            "https://raymistream.net/listen/perufolkradio/live",
            false, null
        ),
        MediaItem(
            "r6", "Radio Cielo 101.7", "Radio",
            "https://stream.zeno.fm/kfjyautiyevvv",
            false, null
        ),
        MediaItem(
            "r7", "Radio Amo la Música (Cusco)", "Radio",
            "https://stream.zeno.fm/aqmct15qmchvv",
            false, null
        ),
        MediaItem(
            "r8", "Radio Enamorados", "Radio",
            "https://stream.zeno.fm/gnybbqc1fnruv",
            false, null
        ),
        MediaItem(
            "r9", "Onda Popular Perú", "Radio",
            "https://dattavolt.com/8278/stream",
            false, null
        ),
        MediaItem(
            "r10", "Radio Paradise AAC", "Radio",
            "https://stream.radioparadise.com/aac-128",
            false,
            "https://www.radioparadise.com/graphics/logo_flat_256.png"
        ),
        MediaItem(
            "r11", "SomaFM Groove Salad", "Radio",
            "https://ice1.somafm.com/groovesalad-128-mp3",
            false,
            "https://somafm.com/img3/groovesalad-400.jpg"
        ),
        MediaItem(
            "r12", "SomaFM Space Station", "Radio",
            "https://ice1.somafm.com/spacestation-128-mp3",
            false,
            "https://somafm.com/img3/spacestation-400.jpg"
        ),
        MediaItem(
            "r13", "Radio Paradise MP3", "Radio",
            "https://stream.radioparadise.com/mp3-128",
            true,
            "https://www.radioparadise.com/graphics/logo_flat_256.png"
        ),
        MediaItem(
            "r14", "SomaFM Drone Zone", "Radio",
            "https://ice1.somafm.com/dronezone-128-mp3",
            true,
            "https://somafm.com/img3/dronezone-400.jpg"
        )
    )

    /**
 * Señales HLS publicas de prueba y demos.
 * Cuando tenga URLs oficiales de TV Perú u otros con derechos, se pueden reemplazar aqui.
 */
    val channels = listOf(
        MediaItem(
            "c1", "Mux Demo HLS", "TV",
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            false,
            "https://www.mux.com/images/mux-logo-black.png"
        ),
        MediaItem(
            "c2", "Apple BipBop", "TV",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            false, null
        ),
        MediaItem(
            "c3", "Apple BipBop 16x9", "TV",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            false, null
        ),
        MediaItem(
            "c4", "Akamai HLS Live", "TV",
            "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
            false, null
        ),
        MediaItem(
            "c5", "Mux Test 001", "TV",
            "https://test-streams.mux.dev/test_001/stream.m3u8",
            false,
            "https://www.mux.com/images/mux-logo-black.png"
        ),
        MediaItem(
            "c6", "Mux DAI Demo", "TV",
            "https://test-streams.mux.dev/dai-discontinuity-deltatre/manifest.m3u8",
            true,
            "https://www.mux.com/images/mux-logo-black.png"
        )
    )

    private const val GTV = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/"
    private const val ARCHIVE = "https://archive.org/download/"

    val movies = listOf(
        MediaItem(
            "m1", "Big Buck Bunny", "Película",
            "${GTV}BigBuckBunny.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/220px-Big_buck_bunny_poster_big.jpg"
        ),
        MediaItem(
            "m2", "Sintel", "Película",
            "${GTV}Sintel.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/Sintel_poster.jpg/220px-Sintel_poster.jpg"
        ),
        MediaItem(
            "m3", "Tears of Steel", "Película",
            "${GTV}TearsOfSteel.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Tears_of_Steel_poster.jpg/220px-Tears_of_Steel_poster.jpg"
        ),
        MediaItem(
            "m4", "Elephants Dream", "Película",
            "${GTV}ElephantsDream.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e8/Elephants_Dream_s5_both.jpg/220px-Elephants_Dream_s5_both.jpg"
        ),
        MediaItem(
            "m5", "Cosmos Laundromat", "Película",
            "https://download.blender.org/demo/movies/BBB/bbb_sunflower_1080p_30fps_normal.mp4",
            false, null
        ),
        MediaItem(
            "m6", "For Bigger Blazes", "Película",
            "${GTV}ForBiggerBlazes.mp4", false, null
        ),
        MediaItem(
            "m7", "For Bigger Escape", "Película",
            "${GTV}ForBiggerEscapes.mp4", false, null
        ),
        MediaItem(
            "m8", "For Bigger Fun", "Película",
            "${GTV}ForBiggerFun.mp4", false, null
        ),
        MediaItem(
            "m9", "For Bigger Joyrides", "Película",
            "${GTV}ForBiggerJoyrides.mp4", true, null
        ),
        MediaItem(
            "m10", "For Bigger Meltdowns", "Película",
            "${GTV}ForBiggerMeltdowns.mp4", false, null
        ),
        MediaItem(
            "m11", "Subaru Outback", "Película",
            "${GTV}SubaruOutbackOnStreetAndDirt.mp4", true, null
        ),
        MediaItem(
            "m12", "Volkswagen GTI", "Película",
            "${GTV}VolkswagenGTIReview.mp4", true, null
        )
    )

    fun all() = radios + channels + movies
}
