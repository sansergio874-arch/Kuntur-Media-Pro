package com.kuntur.mediapro.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Catálogo dinámico: bases oficiales + listas M3U importadas,
 * separadas en radios, canales y películas.
 */
object PlaylistCatalog {

    private const val PREFS = "kuntur_iptv_sources"

    val radios: SnapshotStateList<MediaItem> = mutableStateListOf<MediaItem>().apply {
        addAll(MediaRepository.radios)
    }
    val channels: SnapshotStateList<MediaItem> = mutableStateListOf<MediaItem>().apply {
        addAll(MediaRepository.channels)
    }
    val movies: SnapshotStateList<MediaItem> = mutableStateListOf<MediaItem>().apply {
        addAll(MediaRepository.movies)
    }

    fun knownUrls(): HashSet<String> {
        val set = HashSet<String>()
        radios.forEach { set.add(it.streamUrl) }
        channels.forEach { set.add(it.streamUrl) }
        movies.forEach { set.add(it.streamUrl) }
        return set
    }

    /**
     * Clasifica e inserta entradas nuevas.
     * @return Triple(radiosNuevos, canalesNuevos, peliculasNuevas)
     */
    fun importItems(items: List<MediaItem>): Triple<Int, Int, Int> {
        val existing = knownUrls()
        var r = 0
        var c = 0
        var m = 0
        items.forEach { item ->
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
        }
        return Triple(r, c, m)
    }

    fun saveSource(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("sources", emptySet()).orEmpty().toMutableSet()
        set.add(url.trim())
        prefs.edit().putStringSet("sources", set).apply()
    }

    fun savedSources(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet("sources", emptySet())?.toList().orEmpty()
    }

    /** Elimina las listas importadas y restaura el catálogo base. */
    fun clearImported(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        radios.clear()
        channels.clear()
        movies.clear()
        radios.addAll(MediaRepository.radios)
        channels.addAll(MediaRepository.channels)
        movies.addAll(MediaRepository.movies)
    }
}
