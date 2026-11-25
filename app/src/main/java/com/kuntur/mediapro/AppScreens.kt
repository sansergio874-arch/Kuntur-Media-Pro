package com.kuntur.mediapro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.kuntur.mediapro.data.M3uPlaylistParser
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.data.MediaRepository
import com.kuntur.mediapro.player.DrmHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private val NavyS = Color(0xFF0A1628)
private val NavyMidS = Color(0xFF132337)
private val CardBgS = Color(0xFF1A2D45)
private val GoldS = Color(0xFFE8B923)

enum class AppSection { HOME, RADIOS, CHANNELS, MOVIES, PLAYER }

@Composable
fun AppRoot(player: ExoPlayer) {
    var showSplash by remember { mutableStateOf(true) }
    var section by remember { mutableStateOf(AppSection.HOME) }
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var playErrorMessage by remember { mutableStateOf<String?>(null) }
    val channels = remember { mutableStateListOf<MediaItem>().apply { addAll(MediaRepository.channels) } }
    val scope = rememberCoroutineScope()
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var playlistUrl by remember { mutableStateOf("") }
    var playlistMessage by remember { mutableStateOf<String?>(null) }
    var importingPlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(900)
        showSplash = false
    }

    if (showSplash) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS))), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("K", color = GoldS, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Kuntur Media Pro", color = GoldS, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("TV en vivo · IPTV · Radios · Películas", color = Color.White.copy(0.7f), fontSize = 13.sp)
            }
        }
        return
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { if (!importingPlaylist) showPlaylistDialog = false },
            title = { Text("Agregar lista IPTV") },
            text = {
                Column {
                    Text("Pega la dirección HTTPS de tu lista M3U/M3U8 autorizada.", fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = { playlistUrl = it },
                        singleLine = true,
                        label = { Text("URL de lista M3U") },
                        placeholder = { Text("https://servidor/lista.m3u") },
                        enabled = !importingPlaylist
                    )
                    playlistMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = if (it.startsWith("Lista")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !importingPlaylist && playlistUrl.startsWith("https://", true),
                    onClick = {
                        importingPlaylist = true
                        playlistMessage = null
                        scope.launch {
                            try {
                                val imported = withContext(Dispatchers.IO) {
                                    val connection = (URL(playlistUrl.trim()).openConnection() as HttpURLConnection).apply {
                                        connectTimeout = 20000
                                        readTimeout = 30000
                                        instanceFollowRedirects = true
                                        requestMethod = "GET"
                                        setRequestProperty("User-Agent", "KunturMediaPro/2.9")
                                        setRequestProperty("Accept", "audio/x-mpegurl, application/vnd.apple.mpegurl, text/plain, */*")
                                    }
                                    try {
                                        require(connection.responseCode in 200..299) {
                                            "El servidor respondió HTTP ${connection.responseCode}."
                                        }
                                        val maxBytes = 256L * 1024L * 1024L
                                        require(connection.contentLengthLong <= 0L || connection.contentLengthLong <= maxBytes) {
                                            "La lista supera el límite de 256 MB."
                                        }
                                        connection.inputStream.use { input ->
                                            val limited = LimitedPlaylistInputStream(input, maxBytes)
                                            InputStreamReader(limited, Charsets.UTF_8).buffered().use { reader ->
                                                M3uPlaylistParser.parse(reader)
                                            }
                                        }
                                    } finally {
                                        connection.disconnect()
                                    }
                                }
                                val existing = channels.mapTo(HashSet()) { it.streamUrl }
                                val fresh = imported.filter { existing.add(it.streamUrl) }
                                // Insertar en grupos pequeños para que Compose pueda dibujar y responder entre lotes.
                                fresh.chunked(200).forEach { batch ->
                                    channels.addAll(batch)
                                    yield()
                                }
                                playlistMessage = if (fresh.isEmpty()) "No se encontraron canales nuevos en la lista." else "Lista importada: ${fresh.size} canales nuevos."
                            } catch (error: Exception) {
                                playlistMessage = "No se pudo importar: ${error.message?.take(110) ?: "revisa la URL y el formato M3U"}"
                            } finally {
                                importingPlaylist = false
                            }
                        }
                    }
                ) {
                    if (importingPlaylist) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!importingPlaylist) showPlaylistDialog = false }) { Text("Cerrar") }
            }
        )
    }

    if (playErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { playErrorMessage = null },
            title = { Text("Error de reproducción") },
            text = { Text(playErrorMessage!!) },
            confirmButton = { TextButton(onClick = { playErrorMessage = null }) { Text("Aceptar") } }
        )
    }

    fun startPlayback(item: MediaItem) {
        currentItem = item
        section = AppSection.PLAYER
        try {
            val live = DrmHelper.isLikelyLiveOrRadio(item.streamUrl, item.category)
            val mediaItem = DrmHelper.createClearMediaItem(item.streamUrl, item.title, isLive = live)
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            player.play()
        } catch (error: Exception) {
            playErrorMessage = error.message?.take(120) ?: "No se pudo iniciar la reproducción."
        }
    }

    when (section) {
        AppSection.PLAYER -> {
            val item = currentItem
            if (item != null) {
                PlayerScreen(item = item, player = player, onBack = {
                    try { player.stop(); player.clearMediaItems() } catch (_: Exception) { }
                    section = when (item.category.lowercase()) {
                        "radio" -> AppSection.RADIOS
                        "tv" -> AppSection.CHANNELS
                        else -> AppSection.MOVIES
                    }
                })
            } else section = AppSection.HOME
        }
        AppSection.RADIOS -> CategoryScreen(
            title = "Radios en vivo",
            subtitle = "${MediaRepository.radios.size} emisoras",
            icon = Icons.Default.Radio,
            items = MediaRepository.radios,
            onPlay = { startPlayback(it) },
            onBack = { section = AppSection.HOME }
        )
        AppSection.CHANNELS -> CategoryScreen(
            title = "TV en vivo e IPTV",
            subtitle = "${channels.size} canales · listas M3U/M3U8",
            icon = Icons.Default.Tv,
            items = channels,
            onPlay = { startPlayback(it) },
            onBack = { section = AppSection.HOME },
            onImportPlaylist = { showPlaylistDialog = true }
        )
        AppSection.MOVIES -> CategoryScreen(
            title = "Películas",
            subtitle = "${MediaRepository.movies.size} títulos disponibles",
            icon = Icons.Default.Movie,
            items = MediaRepository.movies,
            onPlay = { startPlayback(it) },
            onBack = { section = AppSection.HOME }
        )
        AppSection.HOME -> HomeHub(
            onRadios = { section = AppSection.RADIOS },
            onChannels = { section = AppSection.CHANNELS },
            onMovies = { section = AppSection.MOVIES }
        )
    }
}

@Composable
fun HomeHub(onRadios: () -> Unit, onChannels: () -> Unit, onMovies: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS))).padding(20.dp)
    ) {
        Text("Kuntur Media Pro", color = GoldS, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Tu entretenimiento en un solo lugar", color = Color.White.copy(0.65f), fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))
        SectionCard("TV en vivo e IPTV", "${MediaRepository.channels.size} canales + tus listas M3U", Icons.Default.Tv, onChannels)
        Spacer(Modifier.height(12.dp))
        SectionCard("Radios", "${MediaRepository.radios.size} emisoras en directo", Icons.Default.Radio, onRadios)
        Spacer(Modifier.height(12.dp))
        SectionCard("Películas", "${MediaRepository.movies.size} títulos", Icons.Default.Movie, onMovies)
        Spacer(Modifier.weight(1f))
        Text("Utiliza fuentes propias o autorizadas. La disponibilidad depende del proveedor.", color = Color.White.copy(0.42f), fontSize = 11.sp)
    }
}

@Composable
fun SectionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBgS),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GoldS, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = Color.White.copy(0.55f), fontSize = 13.sp)
            }
            Icon(Icons.Default.PlayArrow, null, tint = GoldS)
        }
    }
}

@Composable
fun CategoryScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit,
    onBack: () -> Unit,
    onImportPlaylist: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS)))) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 6.dp)) {
            Icon(Icons.Default.Home, null, tint = GoldS, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Inicio", color = GoldS)
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = GoldS, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(title, color = GoldS, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = Color.White.copy(0.55f), fontSize = 13.sp)
                    }
                }
                if (onImportPlaylist != null) {
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onImportPlaylist,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldS),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("＋ Agregar lista IPTV (M3U)", color = NavyS, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(14.dp))
            }
            items(items, key = { it.id }) { MediaRow(it, onPlay) }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}


/** Cuenta bytes reales durante la descarga y corta respuestas sin Content-Length fiable. */
private class LimitedPlaylistInputStream(
    input: InputStream,
    private val maxBytes: Long
) : FilterInputStream(input) {
    private var bytesRead = 0L

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) countBytes(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count > 0) countBytes(count)
        return count
    }

    private fun countBytes(count: Int) {
        bytesRead += count
        require(bytesRead <= maxBytes) { "La lista supera el límite de 256 MB." }
    }
}
