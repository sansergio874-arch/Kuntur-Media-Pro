package com.kuntur.mediapro.data

/** Parser de listas IPTV M3U/M3U8. No incluye listas privadas ni fuentes sin autorización. */
object M3uPlaylistParser {
    fun parse(content: String): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        var title: String? = null
        var logo: String? = null
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val displayName = line.substringAfterLast(',', "").trim()
                    title = displayName.ifBlank { attribute(line, "tvg-name") ?: attribute(line, "name") }
                    logo = attribute(line, "tvg-logo")
                }
                line.isNotBlank() && !line.startsWith("#") &&
                    (line.startsWith("https://", true) || line.startsWith("http://", true)) -> {
                    val streamTitle = title
                    if (!streamTitle.isNullOrBlank()) {
                        val normalizedUrl = line.substringBefore('|').trim()
                        if (normalizedUrl.isNotBlank()) {
                            result.add(MediaItem(
                                id = "iptv_${normalizedUrl.hashCode().toUInt().toString(16)}",
                                title = streamTitle.take(100),
                                category = "TV",
                                streamUrl = normalizedUrl,
                                logoUrl = logo?.takeIf { it.startsWith("https://", true) }
                            ))
                        }
                    }
                    title = null
                    logo = null
                }
            }
        }
        return result.distinctBy { it.streamUrl }
    }

    private fun attribute(line: String, name: String): String? {
        val regex = Regex(Regex.escape(name) + "=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
        return regex.find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }
}
