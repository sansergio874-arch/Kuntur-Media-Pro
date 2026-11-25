package com.kuntur.mediapro.data

import java.io.BufferedReader
import java.io.Reader
import java.security.MessageDigest

/** Parser incremental de listas IPTV M3U/M3U8 con clasificación por tipo. */
object M3uPlaylistParser {
    const val MAX_CHANNELS = 100_000

    fun parse(content: String): List<MediaItem> = parse(content.reader())

    fun parse(reader: Reader): List<MediaItem> {
        val result = ArrayList<MediaItem>()
        val seenUrls = HashSet<String>()
        val input = if (reader is BufferedReader) reader else reader.buffered()
        var title: String? = null
        var logo: String? = null
        var group: String? = null

        while (true) {
            val line = input.readLine()?.trim() ?: break
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val displayName = displayName(line)
                    title = displayName.ifBlank {
                        attribute(line, "tvg-name") ?: attribute(line, "name").orEmpty()
                    }
                    logo = attribute(line, "tvg-logo")
                    group = attribute(line, "group-title")
                }
                line.isNotBlank() && !line.startsWith("#") &&
                    (line.startsWith("https://", true) || line.startsWith("http://", true)) -> {
                    val streamTitle = title
                    val normalizedUrl = line.substringBefore('|').trim()
                    if (!streamTitle.isNullOrBlank() && normalizedUrl.isNotBlank() && seenUrls.add(normalizedUrl)) {
                        require(result.size < MAX_CHANNELS) {
                            "La lista supera el máximo de $MAX_CHANNELS entradas admitidas."
                        }
                        val category = classify(streamTitle, group, normalizedUrl)
                        result.add(
                            MediaItem(
                                id = "iptv_${stableId(normalizedUrl)}",
                                title = streamTitle.take(100),
                                category = category,
                                streamUrl = normalizedUrl,
                                isPremium = false,
                                logoUrl = logo?.takeIf { it.startsWith("https://", true) }
                            )
                        )
                    }
                    title = null
                    logo = null
                    group = null
                }
            }
        }
        return result
    }

    /**
     * Ordena cada entrada: Radio, Película o TV.
     * Usa group-title, título y extensión de URL.
     */
    fun classify(title: String, groupTitle: String?, url: String): String {
        val g = (groupTitle ?: "").lowercase()
        val t = title.lowercase()
        val u = url.lowercase()

        val radioHints = listOf(
            "radio", "fm ", " am", "emisora", "music", "audio", "radio station",
            "radios", "rádio", "estacion"
        )
        val movieHints = listOf(
            "movie", "movies", "film", "films", "vod", "cinema", "película", "pelicula",
            "películas", "peliculas", "series", "serie", "on demand", "cine"
        )

        if (radioHints.any { g.contains(it) || t.contains(it) }) return "Radio"
        if (movieHints.any { g.contains(it) || t.contains(it) }) return "Película"

        if (u.contains(".mp3") || u.contains(".aac") || u.contains("/listen/") ||
            u.contains("radio") || u.contains("streamtheworld") || u.contains("zeno.fm") ||
            u.contains("icecast") || u.contains("shoutcast")
        ) return "Radio"

        if (u.endsWith(".mp4") || u.endsWith(".mkv") || u.endsWith(".avi") ||
            u.endsWith(".mov") || u.contains("/vod/") || u.contains("movie")
        ) return "Película"

        return "TV"
    }

    private fun stableId(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun displayName(line: String): String {
        var quote: Char? = null
        for (index in line.indices) {
            val char = line[index]
            if (quote == null && (char == '"' || char == '\'')) {
                quote = char
            } else if (quote == char) {
                quote = null
            } else if (char == ',' && quote == null) {
                return line.substring(index + 1).trim()
            }
        }
        return ""
    }

    private fun attribute(line: String, name: String): String? {
        val regex = Regex(Regex.escape(name) + "=(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE)
        val match = regex.find(line) ?: return null
        return (match.groupValues.getOrNull(1).orEmpty().ifBlank { match.groupValues.getOrNull(2).orEmpty() })
            .takeIf { it.isNotBlank() }
    }
}
