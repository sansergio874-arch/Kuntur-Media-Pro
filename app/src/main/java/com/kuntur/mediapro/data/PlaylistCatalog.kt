package com.kuntur.mediapro.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Catálogo dinámico: bases oficiales + listas M3U importadas,
 * separadas en radios, canales y películas.
 * Las listas se guardan de forma permanente en SharedPreferences.
 */
object PlaylistCatalog {

    private const val PREFS = "kuntur_iptv_sources"
    private const val KEY_SOURCES = "sources"

    val radios: SnapshotStateList<MediaItem> = mutableStateListOf<MediaItem>().apply {
        try { addAll(MediaRepository.radios) } catch (_: Throwable) {}
    }
    val channels: SnapshotStateList<MediaItem> = mutableStateListOf<MediaItem>().apply {
        try { addAll(MediaRepository.channels) } catch (_: Throwable) {}
    }
    val movies: SnapshotStateList<MediaItem> = mutableStateListOf<MediaItem>().apply {
        try { addAll(MediaRepository.movies) } catch (_: Throwable) {}
    }

    fun knownUrls(): HashSet<String> {
        val set = HashSet<String>()
        try {
            radios.forEach { set.add(it.streamUrl) }
            channels.forEach { set.add(it.streamUrl) }
            movies.forEach { set.add(it.streamUrl) }
        } catch (_: Throwable) {}
        return set
    }

    fun importItems(items: List<MediaItem>): Triple<Int, Int, Int> {
        val existing = knownUrls()
        var r = 0
        var c = 0
        var m = 0
        items.forEach { item ->
            try {
                if (!existing.add(item.streamUrl)) return@forEach
                when (item.category.lowercase()) {
                    "radio" -> {
                        radios.add(item)
                        r++
                    }
                    "película", "pelicula" -> {
                        movies.add(item)
                        m++
                    }
                    else -> {
                        channels.add(item.copy(category = "TV"))
                        c++
                    }
                }
            } catch (_: Throwable) {}
        }
        return Triple(r, c, m)
    }

    /** Guarda la URL de forma permanente. */
    fun saveSource(context: Context, url: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val set = prefs.getStringSet(KEY_SOURCES, emptySet()).orEmpty().toMutableSet()
            set.add(url.trim())
            prefs.edit().putStringSet(KEY_SOURCES, set).apply()
        } catch (_: Throwable) {}
    }

    /** Elimina una URL específica y recarga el catálogo base. */
    fun removeSource(context: Context, url: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val set = prefs.getStringSet(KEY_SOURCES, emptySet()).orEmpty().toMutableSet()
            set.remove(url.trim())
            prefs.edit().putStringSet(KEY_SOURCES, set).apply()
            // Restaurar base y dejar que el resto de fuentes se recarguen al reiniciar o manualmente
            resetToBase()
        } catch (_: Throwable) {}
    }

    fun savedSources(context: Context): List<String> {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.getStringSet(KEY_SOURCES, emptySet())?.toList().orEmpty()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /** Elimina todas las listas importadas y restaura el catálogo base. */
    fun clearImported(context: Context) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
            resetToBase()
        } catch (_: Throwable) {}
    }

    private fun resetToBase() {
        radios.clear()
        channels.clear()
        movies.clear()
        try { radios.addAll(MediaRepository.radios) } catch (_: Throwable) {}
        try { channels.addAll(MediaRepository.channels) } catch (_: Throwable) {}
        try { movies.addAll(MediaRepository.movies) } catch (_: Throwable) {}
    }
}
