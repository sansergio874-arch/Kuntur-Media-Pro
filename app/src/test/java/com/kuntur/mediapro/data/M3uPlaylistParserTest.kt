package com.kuntur.mediapro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class M3uPlaylistParserTest {
    @Test
    fun parsesChannelsAndLogoMetadata() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://example.com/logo.png" group-title="Peru",Canal de prueba
            https://example.com/live.m3u8
        """.trimIndent()

        val channels = M3uPlaylistParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("Canal de prueba", channels.single().title)
        assertEquals("TV", channels.single().category)
        assertEquals("https://example.com/logo.png", channels.single().logoUrl)
        assertEquals("https://example.com/live.m3u8", channels.single().streamUrl)
    }

    @Test
    fun parsesFromReaderAndCreatesStableUniqueIds() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Canal A
            https://example.com/a.m3u8
            #EXTINF:-1,Canal B
            https://example.com/b.m3u8
        """.trimIndent()

        val first = M3uPlaylistParser.parse(StringReader(playlist))
        val second = M3uPlaylistParser.parse(StringReader(playlist))

        assertEquals(2, first.size)
        assertEquals(first.map { it.id }, second.map { it.id })
        assertTrue(first[0].id != first[1].id)
    }

    @Test
    fun ignoresDuplicatesAndEntriesWithoutNames() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Canal Uno
            https://example.com/one.m3u8
            #EXTINF:-1,Canal Uno repetido
            https://example.com/one.m3u8
            https://example.com/orphan.m3u8
        """.trimIndent()

        val channels = M3uPlaylistParser.parse(playlist)

        assertEquals(1, channels.size)
        assertTrue(channels.single().title.startsWith("Canal Uno"))
    }
}
