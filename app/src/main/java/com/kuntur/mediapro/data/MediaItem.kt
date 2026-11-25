package com.kuntur.mediapro.data

data class MediaItem(
    val id: String,
    val title: String,
    val category: String,
    val streamUrl: String,
    val isPremium: Boolean = false,
    val logoUrl: String? = null
)

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
    )

    val movies = listOf(
        // Open movies / CC
        MediaItem("m1", "Big Buck Bunny", "Película",
            "https://uploads.video-commander.com/sample/BigBuckBunny.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/220px-Big_buck_bunny_poster_big.jpg"),
        MediaItem("m3", "Elephants Dream", "Película",
            "https://archive.org/download/ElephantsDream/ed_hd.mp4", false,
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e8/Elephants_Dream_s5_both.jpg/220px-Elephants_Dream_s5_both.jpg"),

        // Clasicos dominio publico
        MediaItem("m5", "Night of the Living Dead", "Película",
            "https://archive.org/download/night_of_the_living_dead/night_of_the_living_dead_512kb.mp4", false, null),
        MediaItem("m6", "The Phantom of the Opera (1925)", "Película",
            "https://archive.org/download/ThePhantomOfTheOpera/Phantom_of_the_Opera_512kb.mp4", false, null),
        MediaItem("m7", "Nosferatu (1922)", "Película",
            "https://archive.org/download/nosferatucomplete/Nosferatu.mp4", false, null),
        MediaItem("m8", "The Most Dangerous Game", "Película",
            "https://archive.org/download/TheMostDangerousGame/TheMostDangerousGame_512kb.mp4", false, null),
        MediaItem("m9", "House on Haunted Hill", "Película",
            "https://archive.org/download/house_on_haunted_hill/house_on_haunted_hill_512kb.mp4", false, null),
        MediaItem("m10", "Plan 9 from Outer Space", "Película",
            "https://archive.org/download/Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959.mp4", false, null),

        // Nuevas
        MediaItem("m11", "His Girl Friday", "Película",
            "https://archive.org/download/HisGirlFriday1940HQ/His_Girl_Friday_1940.mp4", false, null),
        MediaItem("m12", "Detour (1945)", "Película",
            "https://archive.org/download/Detour/Detour.mp4", false, null),
        MediaItem("m13", "D.O.A. (1950)", "Película",
            "https://archive.org/download/doa_1950/Doa.mp4", false, null),
        MediaItem("m14", "The Little Shop of Horrors (1960)", "Película",
            "https://archive.org/download/TheLittleShopOfHorrors_655/TheLittleShopOfHorrors.mp4", false, null),
        MediaItem("m15", "Carnival of Souls", "Película",
            "https://archive.org/download/CarnivalOfSouls1962/CarnivalOfSouls.mp4", false, null),
        MediaItem("m16", "The General (Buster Keaton)", "Película",
            "https://archive.org/download/TheGeneral1926/TheGeneral.mp4", false, null),
        MediaItem("m17", "Steamboat Bill Jr.", "Película",
            "https://archive.org/download/SteamboatBillJr/SteamboatBillJr.mp4", false, null),
        MediaItem("m18", "Metropolis (1927)", "Película",
            "https://archive.org/download/Metropolis1927RestoredVersion/Metropolis.mp4", false, null),
        MediaItem("m19", "Charade (1963)", "Película",
            "https://archive.org/download/Charade1963/Charade1963.mp4", false, null),
    )

    fun all() = radios + channels + movies
}
