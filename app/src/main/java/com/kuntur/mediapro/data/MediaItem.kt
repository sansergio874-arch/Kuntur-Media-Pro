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
 * Fuentes públicas legales de demostración.
 * Sustituya por URLs con derechos propios cuando las disponga.
 */
object MediaRepository {

    val radios = listOf(
        MediaItem("r1", "Radio Paradise AAC", "Radio", "https://stream.radioparadise.com/aac-128", false),
        MediaItem("r2", "Radio Paradise MP3", "Radio", "https://stream.radioparadise.com/mp3-128", false),
        MediaItem("r3", "Radio Paradise Mellow", "Radio", "https://stream.radioparadise.com/m128", false),
        MediaItem("r4", "Radio Paradise Rock", "Radio", "https://stream.radioparadise.com/rock-128", false),
        MediaItem("r5", "SomaFM Groove Salad", "Radio", "https://ice1.somafm.com/groovesalad-128-mp3", false),
        MediaItem("r6", "SomaFM Drone Zone", "Radio", "https://ice1.somafm.com/dronezone-128-mp3", false),
        MediaItem("r7", "SomaFM Indie Pop", "Radio", "https://ice1.somafm.com/indiepop-128-mp3", false),
        MediaItem("r8", "SomaFM Space Station", "Radio", "https://ice1.somafm.com/spacestation-128-mp3", false),
        MediaItem("r9", "SomaFM Folk Forward", "Radio", "https://ice1.somafm.com/folkfwd-128-mp3", false),
        MediaItem("r10", "SomaFM Beat Blender", "Radio", "https://ice1.somafm.com/beatblender-128-mp3", true),
        MediaItem("r11", "SomaFM Metal Detector", "Radio", "https://ice1.somafm.com/metal-128-mp3", true),
        MediaItem("r12", "SomaFM Left Coast", "Radio", "https://ice1.somafm.com/leftcoast-128-mp3", false),
        MediaItem("r13", "SomaFM Fluid", "Radio", "https://ice1.somafm.com/fluid-128-mp3", false),
        MediaItem("r14", "SomaFM DEF CON", "Radio", "https://ice1.somafm.com/defcon-128-mp3", true),
        MediaItem("r15", "SomaFM Deep Space One", "Radio", "https://ice1.somafm.com/deepspaceone-128-mp3", false),
        MediaItem("r16", "SomaFM Lush", "Radio", "https://ice1.somafm.com/lush-128-mp3", false),
        MediaItem("r17", "SomaFM PopTron", "Radio", "https://ice1.somafm.com/poptron-128-mp3", true),
        MediaItem("r18", "SomaFM ThistleRadio", "Radio", "https://ice1.somafm.com/thistle-128-mp3", false)
    )

    val channels = listOf(
        MediaItem("c1", "Mux Demo HLS", "TV", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", false),
        MediaItem("c2", "Mux Test 001", "TV", "https://test-streams.mux.dev/test_001/stream.m3u8", false),
        MediaItem("c3", "Apple BipBop Basic", "TV", "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8", false),
        MediaItem("c4", "Apple BipBop 16x9", "TV", "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8", false),
        MediaItem("c5", "Apple Advanced Stream", "TV", "https://devstreaming-cdn.apple.com/videos/streaming/examples/adv_dv_atmos/main.m3u8", true),
        MediaItem("c6", "Mux DAI Demo", "TV", "https://test-streams.mux.dev/dai-discontinuity-deltatre/manifest.m3u8", true),
        MediaItem("c7", "Akamai HLS Test", "TV", "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8", false),
        MediaItem("c8", "Cloudflare Sample", "TV", "https://customer-m033z5x00ks6nunl.cloudflarestream.com/b236bde30eb07b9cba1e031208885754/manifest/video.m3u8", true)
    )

    val movies = listOf(
        MediaItem("m1", "Big Buck Bunny", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", false),
        MediaItem("m2", "Elephants Dream", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", false),
        MediaItem("m3", "Sintel", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", false),
        MediaItem("m4", "Tears of Steel", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", false),
        MediaItem("m5", "For Bigger Blazes", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", false),
        MediaItem("m6", "For Bigger Escape", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", false),
        MediaItem("m7", "For Bigger Fun", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", false),
        MediaItem("m8", "For Bigger Joyrides", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4", true),
        MediaItem("m9", "For Bigger Meltdowns", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", false),
        MediaItem("m10", "Subaru Outback", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4", true),
        MediaItem("m11", "Volkswagen GTI", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4", true),
        MediaItem("m12", "What car?", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4", false),
        MediaItem("m13", "We Are Going On Bullrun", "Película", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4", true)
    )

    fun all() = radios + channels + movies
}
