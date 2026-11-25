package com.kuntur.mediapro.data

import java.io.BufferedReader
import java.io.Reader
import java.security.MessageDigest

/** Parser incremental de listas IPTV M3U/M3U8. */
object M3uPlaylistParser {
    const val MAX_CHANNELS = 100_000

    fun parse(content: String): List<MediaItem> = parse(content.reader())

    /** Lee línea por línea: nunca mantiene una segunda copia gigante del archivo como String. */
    fun parse(reader: Reader): List<MediaItem> {
        val result = ArrayList<MediaItem>()
        val seenUrls = HashSet<String>()
        val input = if (reader is BufferedReader) reader else reader.buffered()
        var title: String? = null
        var logo: String? = null

        while (true) {
            val line = input.readLine()?.trim() ?: break
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val displayName = line.substringAfterLast(',', "").trim()
                    title = displayName.ifBlank { attribute(line, "tvg-name") ?: attribute(line, "name") }
                    logo = attribute(line, "tvg-logo")
                }
                line.isNotBlank() && !line.startsWith("#") &&
                    (line.startsWith("https://", true) || line.startsWith("http://", true)) -> {
                    val streamTitle = title
                    val normalizedUrl = line.substringBefore('|').trim()
                    if (!streamTitle.isNullOrBlank() && normalizedUrl.isNotBlank() && seenUrls.add(normalizedUrl)) {
                        require(result.size < MAX_CHANNELS) {
                            "La lista supera el máximo de $MAX_CHANNELS canales admitidos."
                        }
                        result.add(
                            MediaItem(
                                id = "iptv_${stableId(normalizedUrl)}",
                                title = streamTitle.take(100),
                                category = "TV",
                                streamUrl = normalizedUrl,
                                logoUrl = logo?.takeIf { it.startsWith("https://", true) }
                            )
                        )
                    }
                    title = null
                    logo = null
                }
            }
        }
        return result
    }

    private fun stableId(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun attribute(line: String, name: String): String? {
        val regex = Regex(Regex.escape(name) + "=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }
}
