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
 * Catálogo reducido a fuentes peruanas reconocidas.
 * Sin demos internacionales, streams de relleno ni películas de prueba.
 */
object MediaRepository {

    val radios = listOf(
        MediaItem(
            id = "r1",
            title = "Radio Nacional del Perú",
            category = "Radio",
            streamUrl = "https://cdnhd.iblups.com/hls/0773874174fd4eba8bb9eff741d190dc.m3u8",
            isPremium = false
        ),
        MediaItem(
            id = "r2",
            title = "PBO Radio 91.9",
            category = "Radio",
            streamUrl = "https://stream.radiojar.com/2fse67zuv8hvv",
            isPremium = false
        ),
        MediaItem(
            id = "r3",
            title = "Radio Disney Perú",
            category = "Radio",
            streamUrl = "https://24403.live.streamtheworld.com/DISNEY_PER_LM.mp3",
            isPremium = false
        ),
        MediaItem(
            id = "r4",
            title = "La Inolvidable (CRP)",
            category = "Radio",
            streamUrl = "https://27163.live.streamtheworld.com/CRP_INC.mp3",
            isPremium = false
        )
    )

    val channels = listOf(
        MediaItem(
            id = "c1",
            title = "TV Perú",
            category = "TV",
            streamUrl = "https://cdnhd.iblups.com/hls/777b4d4cc0984575a7d14f6ee57dbcaf7.m3u8",
            isPremium = false
        ),
        MediaItem(
            id = "c2",
            title = "TV Perú Noticias",
            category = "TV",
            streamUrl = "https://cdnhd.iblups.com/hls/902c1a0395264f269f1160efa00660e4.m3u8",
            isPremium = false
        ),
        MediaItem(
            id = "c3",
            title = "TV Perú Internacional",
            category = "TV",
            streamUrl = "https://cdnhd.iblups.com/hls/ee2450c81e554f4cae0e6292106993c2.m3u8",
            isPremium = false
        ),
        MediaItem(
            id = "c4",
            title = "Canal IPe",
            category = "TV",
            streamUrl = "https://cdnhd.iblups.com/hls/3f2cb1658d114f2693eff18d83199e67.m3u8",
            isPremium = false
        )
    )

    /** Sin películas de demo ni relleno. Agregue títulos solo con derechos verificados. */
    val movies = emptyList<MediaItem>()

    fun all() = radios + channels + movies
}
