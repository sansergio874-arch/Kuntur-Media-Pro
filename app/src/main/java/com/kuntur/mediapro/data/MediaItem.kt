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
 * Radios publicas, senales IRTP y peliculas legales (dominio publico / CC / muestras).
 */
object MediaRepository {

    val radios = listOf(
        MediaItem("r1", "Radio Nacional del Perú", "Radio",
            "https://cdnhd.iblups.com/hls/0773874174fd4eba8bb9eff741d190dc.m3u8", false, null),
        MediaItem("r2", "PBO Radio 91.9", "Radio",
            "https://stream.radiojar.com/2fse67zuv8hvv", false, null),
        MediaItem("r3", "Radio Disney Perú", "Radio",
            "https://24403.live.streamtheworld.com/DISNEY_PER_LM.mp3", false, null),
        MediaItem("r4", "La Inolvidable (CRP)", "Radio",
            "https://27163.live.streamtheworld.com/CRP_INC.mp3", false, null),
        MediaItem("r5", "Perú Folk Radio", "Radio",
            "https://raymistream.net/listen/perufolkradio/live", false, null),
        MediaItem("r6", "Radio Cielo 101.7", "Radio",
            "https://stream.zeno.fm/kfjyautiyevvv", false, null),
        MediaItem("r7", "Radio Amo la Música (Cusco)", "Radio",
            "https://stream.zeno.fm/aqmct15qmchvv", false, null),
        MediaItem("r8", "Radio Enamorados", "Radio",
            "https://stream.zeno.fm/gnybbqc1fnruv", false, null),
        MediaItem("r9", "Onda Popular Perú", "Radio",
            "https://dattavolt.com/8278/stream", false, null),
        MediaItem("r10", "Radio Paradise AAC", "Radio",
            "https://stream.radioparadise.com/aac-128", false,
            "https://www.radioparadise.com/graphics/logo_flat_256.png"),
        MediaItem("r11", "SomaFM Groove Salad", "Radio",
            "https://ice1.somafm.com/groovesalad-128-mp3", false,
            "https://somafm.com/img3/groovesalad-400.jpg"),
        MediaItem("r12", "SomaFM Space Station", "Radio",
            "https://ice1.somafm.com/spacestation-128-mp3", false,
            "https://somafm.com/img3/spacestation-400.jpg")
    )

    val channels = listOf(
        MediaItem("c1", "TV Perú", "TV",
            "https://cdnhd.iblups.com/hls/777b4d4cc0984575a7d14f6ee57dbcaf7.m3u8", false, null),
        MediaItem("c2", "TV Perú Noticias", "TV",
            "https://cdnhd.iblups.com/hls/902c1a0395264f269f1160efa00660e4.m3u8", false, null),
        MediaItem("c3", "TV Perú Internacional", "TV",
            "https://cdnhd.iblups.com/hls/ee2450c81e554f4cae0e6292106993c2.m3u8", false, null),
        MediaItem("c4", "Canal IPe", "TV",
            "https://cdnhd.iblups.com/hls/3f2cb1658d114f2693eff18d83199e67.m3u8", false, null),
        MediaItem("c5", "Mux Demo HLS", "TV",
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", false,
            "https://www.mux.com/images/mux-logo-black.png"),
        MediaItem("c6", "Apple BipBop", "TV",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            false, null),
        MediaItem("c7", "Akamai HLS Live", "TV",
            "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8", false, null),
        MediaItem("c8", "Mux Test 001", "TV",
            "https://test-streams.mux.dev/test_001/stream.m3u8", true,
            "https://www.mux.com/images/mux-logo-black.png")
    )

    /**
     * Peliculas legales:
     * - Blender Open Movies (CC)
     * - Clasicos de dominio publico (Internet Archive)
     * - Muestras cortas de prueba
     */
    val movies = listOf(
        MediaItem(
            "m1", "Big Buck Bunny", "Película",
            "https://uploads.video-commander.com/sample/BigBuckBunny.mp4",
            false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/220px-Big_buck_bunny_poster_big.jpg"
        ),
        MediaItem(
            "m2", "Sintel", "Película",
            "https://download.blender.org/durian/trailer/sintel_trailer-1080p.mp4",
            false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b0/Sintel_poster.jpg/220px-Sintel_poster.jpg"
        ),
        MediaItem(
            "m3", "Elephants Dream", "Película",
            "https://archive.org/download/ElephantsDream/ed_hd.mp4",
            false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e8/Elephants_Dream_s5_both.jpg/220px-Elephants_Dream_s5_both.jpg"
        ),
        MediaItem(
            "m4", "Tears of Steel (trailer)", "Película",
            "https://download.blender.org/mango/trailer_tears_of_steel_1080p.mov",
            false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Tears_of_Steel_poster.jpg/220px-Tears_of_Steel_poster.jpg"
        ),
        MediaItem(
            "m5", "Night of the Living Dead", "Película",
            "https://archive.org/download/night_of_the_living_dead/night_of_the_living_dead_512kb.mp4",
            false, null
        ),
        MediaItem(
            "m6", "The Phantom of the Opera (1925)", "Película",
            "https://archive.org/download/ThePhantomOfTheOpera/Phantom_of_the_Opera_512kb.mp4",
            false, null
        ),
        MediaItem(
            "m7", "Nosferatu (1922)", "Película",
            "https://archive.org/download/nosferatucomplete/Nosferatu.mp4",
            false, null
        ),
        MediaItem(
            "m8", "The Most Dangerous Game", "Película",
            "https://archive.org/download/TheMostDangerousGame/TheMostDangerousGame_512kb.mp4",
            false, null
        ),
        MediaItem(
            "m9", "House on Haunted Hill", "Película",
            "https://archive.org/download/house_on_haunted_hill/house_on_haunted_hill_512kb.mp4",
            false, null
        ),
        MediaItem(
            "m10", "Plan 9 from Outer Space", "Película",
            "https://archive.org/download/Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959.mp4",
            true, null
        ),
        MediaItem(
            "m11", "Charade (1963)", "Película",
            "https://archive.org/download/Charade1963/Charade1963.mp4",
            true, null
        ),
        MediaItem(
            "m12", "Flower (demo corto)", "Película",
            "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
            false, null
        ),
        MediaItem(
            "m13", "Big Buck Bunny 360p (corto)", "Película",
            "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4",
            false, null
        )
    )

    fun all() = radios + channels + movies
}
