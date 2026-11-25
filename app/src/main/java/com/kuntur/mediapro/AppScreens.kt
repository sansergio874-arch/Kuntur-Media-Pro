package com.kuntur.mediapro

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.kuntur.mediapro.billing.BillingConfig
import com.kuntur.mediapro.billing.PurchaseManager
import com.kuntur.mediapro.data.FavoritesManager
import com.kuntur.mediapro.data.M3uPlaylistParser
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.data.PlaylistCatalog
import com.kuntur.mediapro.player.DrmHelper
import com.kuntur.mediapro.player.PlayerHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

enum class AppSection { HOME, BROWSE, PLAYER, PLANS, FAVORITES, SETTINGS, TRANSLATOR }

fun canAccess(context: Context, item: MediaItem): Boolean =
    PurchaseManager.canAccess(context, item)

@Composable
fun AppRoot(player: ExoPlayer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }
    var section by remember { mutableStateOf(AppSection.HOME) }
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var accessDeniedMessage by remember { mutableStateOf<String?>(null) }
    var playErrorMessage by remember { mutableStateOf<String?>(null) }
    var browseInitialPage by remember { mutableIntStateOf(0) }

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var playlistUrl by remember { mutableStateOf("") }
    var playlistMessage by remember { mutableStateOf<String?>(null) }
    var importingPlaylist by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(Unit) {
        delay(1200)
        showSplash = false
    }

    LaunchedEffect(Unit) {
        try {
            val sources = PlaylistCatalog.savedSources(context)
            sources.forEach { source ->
                try {
                    val imported = loadPlaylist(source)
                    PlaylistCatalog.importItems(imported)
                    yield()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = NavyMidS) {
                Spacer(Modifier.height(24.dp))
                Text("Kuntur Media Pro", color = GoldS, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
                Text("Menú", color = Color.White.copy(0.55f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, null, tint = GoldS) },
                    label = { Text("Inicio", color = Color.White) },
                    selected = section == AppSection.HOME,
                    onClick = { section = AppSection.HOME; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Mic, null, tint = GoldS) },
                    label = { Text("Traductor", color = Color.White) },
                    selected = section == AppSection.TRANSLATOR,
                    onClick = { section = AppSection.TRANSLATOR; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, null, tint = GoldS) },
                    label = { Text("Configuraciones", color = Color.White) },
                    selected = section == AppSection.SETTINGS,
                    onClick = { section = AppSection.SETTINGS; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Star, null, tint = GoldS) },
                    label = { Text("Planes Premium", color = Color.White) },
                    selected = section == AppSection.PLANS,
                    onClick = { section = AppSection.PLANS; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.weight(1f))
                Text("Fuentes autorizadas únicamente", color = Color.White.copy(0.4f), fontSize = 11.sp, modifier = Modifier.padding(20.dp))
            }
        }
    ) {
        if (showPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { if (!importingPlaylist) showPlaylistDialog = false },
                title = { Text("Agregar lista IPTV (M3U)") },
                text = {
                    Column {
                        Text("Pegue la URL HTTPS de su lista M3U/M3U8 autorizada. Las entradas se ordenarán automáticamente.", fontSize = 13.sp)
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
                                    val imported = loadPlaylist(playlistUrl.trim())
                                    PlaylistCatalog.saveSource(context, playlistUrl.trim())
                                    val (r, c, m) = PlaylistCatalog.importItems(imported)
                                    val total = r + c + m
                                    playlistMessage = if (total == 0) "No se encontraron entradas nuevas." else "Lista importada: $r radios · $c canales · $m películas."
                                } catch (error: Exception) {
                                    playlistMessage = "No se pudo importar: ${error.message?.take(110) ?: "revise la URL"}"
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

        if (accessDeniedMessage != null) {
            AlertDialog(
                onDismissRequest = { accessDeniedMessage = null },
                title = { Text("Contenido Premium") },
                text = { Text(accessDeniedMessage!!) },
                confirmButton = {
                    Button(onClick = { accessDeniedMessage = null; section = AppSection.PLANS }, colors = ButtonDefaults.buttonColors(containerColor = GoldS)) { Text("Ver planes", color = NavyS) }
                },
                dismissButton = { TextButton(onClick = { accessDeniedMessage = null }) { Text("Cerrar", color = GoldS) } }
            )
        }

        if (playErrorMessage != null) {
            AlertDialog(
                onDismissRequest = { playErrorMessage = null },
                title = { Text("Error de reproducción") },
                text = { Text(playErrorMessage!!) },
                confirmButton = { Button(onClick = { playErrorMessage = null }, colors = ButtonDefaults.buttonColors(containerColor = GoldS)) { Text("Aceptar", color = NavyS) } }
            )
        }

        fun startPlayback(item: MediaItem) {
            if (!canAccess(context, item)) {
                accessDeniedMessage = "Contenido Premium. Elija un plan, pague por Yape ${BillingConfig.YAPE_NUMBER} e indique su código."
                return
            }
            currentItem = item
            section = AppSection.PLAYER
            try { PlayerHolder.ensureService(context) } catch (_: Throwable) {}
            try {
                val live = DrmHelper.isLikelyLiveOrRadio(item.streamUrl, item.category)
                val mi = DrmHelper.createClearMediaItem(item.streamUrl, item.title, isLive = live)
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(mi)
                player.prepare()
                player.playWhenReady = true
                player.play()
            } catch (e: Exception) {
                playErrorMessage = e.message?.take(120) ?: "No se pudo iniciar la reproducción."
            }
        }

        when (section) {
            AppSection.PLAYER -> if (currentItem != null) {
                val item = currentItem!!
                PlayerScreen(
                    item = item,
                    player = player,
                    onBack = { section = AppSection.BROWSE },
                    onStop = {
                        try { player.stop(); player.clearMediaItems() } catch (_: Exception) {}
                        section = AppSection.BROWSE
                    },
                    onToggleFavorite = { FavoritesManager.toggle(context, item.id) }
                )
            } else {
                section = AppSection.HOME
            }

            AppSection.PLANS -> PlansScreen(onBack = { section = AppSection.HOME })

            AppSection.FAVORITES -> FavoritesScreen(onPlay = { startPlayback(it) }, onBack = { section = AppSection.HOME })

            AppSection.SETTINGS -> SettingsScreen(
                onBack = { section = AppSection.HOME },
                onClearAll = { PlaylistCatalog.clearImported(context) },
                onRemoveOne = { url -> PlaylistCatalog.removeSource(context, url) }
            )

            AppSection.TRANSLATOR -> TranslatorScreen(onBack = { section = AppSection.HOME })

            AppSection.BROWSE -> BrowsePagerScreen(
                initialPage = browseInitialPage,
                onPlay = { startPlayback(it) },
                onBack = { section = AppSection.HOME },
                onPlans = { section = AppSection.PLANS }
            )

            AppSection.HOME -> HomeHub(
                radioCount = PlaylistCatalog.radios.size,
                channelCount = PlaylistCatalog.channels.size,
                movieCount = PlaylistCatalog.movies.size,
                onMenu = { scope.launch { drawerState.open() } },
                onRadios = { browseInitialPage = 0; section = AppSection.BROWSE },
                onChannels = { browseInitialPage = 1; section = AppSection.BROWSE },
                onMovies = { browseInitialPage = 2; section = AppSection.BROWSE },
                onFavorites = { section = AppSection.FAVORITES },
                onPlans = { section = AppSection.PLANS },
                onAddPlaylist = {
                    playlistUrl = ""
                    playlistMessage = null
                    showPlaylistDialog = true
                }
            )
        }
    }
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("K", color = GoldS, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("Kuntur Media Pro", color = GoldS, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("TV · IPTV · Radios · Películas", color = Color.White.copy(0.7f), fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = GoldS, strokeWidth = 3.dp)
        }
    }
}

@Composable
fun HomeHub(
    radioCount: Int,
    channelCount: Int,
    movieCount: Int,
    onMenu: () -> Unit,
    onRadios: () -> Unit,
    onChannels: () -> Unit,
    onMovies: () -> Unit,
    onFavorites: () -> Unit,
    onPlans: () -> Unit,
    onAddPlaylist: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS))).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Menú", tint = GoldS) }
            Column(Modifier.weight(1f)) {
                Text("Kuntur Media Pro", color = GoldS, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Tu entretenimiento en un solo lugar", color = Color.White.copy(0.6f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionCard("TV en vivo e IPTV", "$channelCount canales + tus listas M3U", Icons.Default.Tv, onChannels)
        Spacer(Modifier.height(12.dp))
        SectionCard("Radios", "$radioCount emisoras en directo", Icons.Default.Radio, onRadios)
        Spacer(Modifier.height(12.dp))
        SectionCard("Películas", "$movieCount títulos", Icons.Default.Movie, onMovies)
        Spacer(Modifier.height(12.dp))
        SectionCard("Favoritos", "Sus contenidos guardados", Icons.Default.Favorite, onFavorites)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAddPlaylist,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldS),
            shape = RoundedCornerShape(14.dp)
        ) { Text("+ Agregar lista IPTV (M3U)", color = NavyS, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        Text("Las listas se guardan permanentemente. Puede eliminarlas en Configuraciones.", color = Color.White.copy(0.45f), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onPlans,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardBgS),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Planes Premium y Yape", color = GoldS, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    onRemoveOne: (String) -> Unit
) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var sources by remember { mutableStateOf(PlaylistCatalog.savedSources(context)) }
    val version = "2.9"

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Eliminar todas las listas") },
            text = { Text("Se eliminarán todas las listas M3U guardadas y se restaurará el catálogo base. ¿Continuar?") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        sources = emptyList()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Eliminar todo", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancelar", color = GoldS) } }
        )
    }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS))).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.Home, null, tint = GoldS, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Inicio", color = GoldS)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Settings, null, tint = GoldS)
        }
        Spacer(Modifier.height(8.dp))
        Text("Configuraciones", color = GoldS, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Listas M3U guardadas permanentemente", color = Color.White.copy(0.55f), fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBgS), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = GoldS)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Versión", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(version, color = Color.White.copy(0.6f), fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Listas guardadas (${sources.size})", color = GoldS, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        if (sources.isEmpty()) {
            Text("No hay listas importadas.", color = Color.White.copy(0.5f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 280.dp)) {
                items(sources, key = { it }) { url ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBgS),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                url.take(48) + if (url.length > 48) "…" else "",
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                onRemoveOne(url)
                                sources = PlaylistCatalog.savedSources(context)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFE53935))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (sources.isNotEmpty()) {
            Button(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Eliminar todas las listas", color = Color.White, fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(20.dp))
        Text("Yape: ${BillingConfig.YAPE_NUMBER}", color = GoldS, fontWeight = FontWeight.SemiBold)
        Text("La activación de planes se realiza tras verificar el pago.", color = Color.White.copy(0.55f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.weight(1f))
        Text("Utilice únicamente fuentes de contenido autorizadas.", color = Color.White.copy(0.4f), fontSize = 12.sp)
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
                Text(subtitle, color = Color.White.copy(0.5f), fontSize = 13.sp)
            }
            Icon(Icons.Default.PlayArrow, null, tint = GoldS)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BrowsePagerScreen(
    initialPage: Int,
    onPlay: (MediaItem) -> Unit,
    onBack: () -> Unit,
    onPlans: () -> Unit
) {
    val pages = listOf(
        Triple("Radios", Icons.Default.Radio, PlaylistCatalog.radios.toList()),
        Triple("Canales TV", Icons.Default.Tv, PlaylistCatalog.channels.toList()),
        Triple("Películas", Icons.Default.Movie, PlaylistCatalog.movies.toList())
    )
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, pages.lastIndex), pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS)))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.Home, null, tint = GoldS, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Inicio", color = GoldS)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onPlans) {
                Icon(Icons.Default.Star, null, tint = GoldS, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Planes", color = GoldS)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.forEachIndexed { index, (title, icon, _) ->
                val selected = pagerState.currentPage == index
                FilterChip(
                    selected = selected,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    label = { Text(title, fontSize = 13.sp) },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldS,
                        selectedLabelColor = NavyS,
                        selectedLeadingIconColor = NavyS,
                        containerColor = CardBgS,
                        labelColor = Color.White,
                        iconColor = GoldS
                    )
                )
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Buscar…", color = Color.White.copy(0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = GoldS) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldS,
                unfocusedBorderColor = Color.White.copy(0.25f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = GoldS,
                focusedContainerColor = CardBgS,
                unfocusedContainerColor = CardBgS
            ),
            shape = RoundedCornerShape(12.dp)
        )
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val (title, icon, items) = pages[page]
            val filtered = if (query.isBlank()) items else items.filter { it.title.contains(query, ignoreCase = true) }
            CategoryListContent(title = title, subtitle = "${filtered.size} de ${items.size}", icon = icon, items = filtered, onPlay = onPlay)
        }
    }
}

@Composable
fun CategoryListContent(title: String, subtitle: String, icon: ImageVector, items: List<MediaItem>, onPlay: (MediaItem) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = GoldS, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = GoldS, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.White.copy(0.5f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (items.isEmpty()) {
            item { Text("No hay resultados", color = Color.White.copy(0.5f), modifier = Modifier.padding(24.dp)) }
        } else {
            items(items, key = { it.id }) { MediaRow(it, onPlay) }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun FavoritesScreen(onPlay: (MediaItem) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val all = remember(refresh, PlaylistCatalog.radios.size, PlaylistCatalog.channels.size, PlaylistCatalog.movies.size) {
        PlaylistCatalog.radios + PlaylistCatalog.channels + PlaylistCatalog.movies
    }
    val favorites = remember(refresh, all) { FavoritesManager.favoritesFrom(context, all) }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS)))) {
        TextButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Default.Home, null, tint = GoldS, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Inicio", color = GoldS)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                Text("Favoritos", color = GoldS, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${favorites.size} guardados", color = Color.White.copy(0.5f), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
            }
            if (favorites.isEmpty()) {
                item { Text("Aún no tiene favoritos.", color = Color.White.copy(0.55f), modifier = Modifier.padding(16.dp)) }
            } else {
                items(favorites, key = { it.id }) { item -> MediaRow(item, onPlay, onFavoriteChanged = { refresh++ }) }
            }
        }
    }
}

private class LimitedPlaylistInputStream(input: InputStream, private val maxBytes: Long) : FilterInputStream(input) {
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

private suspend fun loadPlaylist(sourceUrl: String): List<MediaItem> = withContext(Dispatchers.IO) {
    require(sourceUrl.startsWith("https://", ignoreCase = true)) { "La URL debe usar HTTPS." }
    val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000
        readTimeout = 20000
        instanceFollowRedirects = true
        requestMethod = "GET"
        setRequestProperty("User-Agent", "KunturMediaPro/2.9")
        setRequestProperty("Accept", "audio/x-mpegurl, application/vnd.apple.mpegurl, text/plain, */*")
    }
    try {
        require(connection.responseCode in 200..299) { "El servidor respondió HTTP ${connection.responseCode}." }
        val maxBytes = 256L * 1024L * 1024L
        require(connection.contentLengthLong <= 0L || connection.contentLengthLong <= maxBytes) { "La lista supera el límite de 256 MB." }
        connection.inputStream.use { input ->
            val limited = LimitedPlaylistInputStream(input, maxBytes)
            InputStreamReader(limited, Charsets.UTF_8).buffered().use { reader -> M3uPlaylistParser.parse(reader) }
        }
    } finally {
        connection.disconnect()
    }
}
