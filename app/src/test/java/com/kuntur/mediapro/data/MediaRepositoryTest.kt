package com.kuntur.mediapro.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRepositoryTest {

    @Test
    fun allCatalogIdsAreUnique() {
        val items = MediaRepository.all()
        assertEquals(items.size, items.map { it.id }.toSet().size)
    }

    @Test
    fun allCatalogStreamsUseHttps() {
        assertTrue(MediaRepository.all().all { it.streamUrl.startsWith("https://") })
    }

    @Test
    fun categoryListsContainOnlyTheirOwnMediaTypes() {
        assertTrue(MediaRepository.radios.all { it.category.equals("Radio", ignoreCase = true) })
        assertTrue(MediaRepository.channels.all { it.category.equals("TV", ignoreCase = true) })
        assertTrue(MediaRepository.movies.all { it.category.equals("Película", ignoreCase = true) })
    }
}
