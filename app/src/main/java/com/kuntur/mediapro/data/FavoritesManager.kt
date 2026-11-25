package com.kuntur.mediapro.data

import android.content.Context

/**
 * Almacena favoritos de forma local (SharedPreferences).
 */
object FavoritesManager {

    private const val PREFS = "kuntur_favorites"
    private const val KEY = "ids"

    fun getIds(context: Context): Set<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun isFavorite(context: Context, id: String): Boolean =
        getIds(context).contains(id)

    fun toggle(context: Context, id: String): Boolean {
        val current = getIds(context).toMutableSet()
        val nowFavorite = if (current.contains(id)) {
            current.remove(id)
            false
        } else {
            current.add(id)
            true
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, current.joinToString(","))
            .apply()
        return nowFavorite
    }

    fun favoritesFrom(context: Context, all: List<MediaItem>): List<MediaItem> {
        val ids = getIds(context)
        return all.filter { it.id in ids }
    }
}
