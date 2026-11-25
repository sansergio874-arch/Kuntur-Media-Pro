package com.kuntur.mediapro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.kuntur.mediapro.billing.PurchaseManager
import com.kuntur.mediapro.data.FavoritesManager
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.data.MediaRepository
import com.kuntur.mediapro.player.DrmHelper
import com.kuntur.mediapro.player.PlayerHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val NavyS = Color(0xFF0A1628)
private val NavyMidS = Color(0xFF132337)
private val CardBgS = Color(0xFF1A2D45)
private val GoldS = Color(0xFFE8B923)

enum class AppSection { HOME, BROWSE, PLAYER, PLANS, FAVORITES }

fun canAccess(context: android.content.Context, item: MediaItem): Boolean {
    if (!item.isPremium) return true
    val plan = PurchaseManager.getCurrentPlan(context)
    return PurchaseManager.isApproved(context) && plan != "free"
}

@Composable
fun AppRoot(player: ExoPlayer) {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var section by remember { mutableStateOf(AppSection.HOME) }
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var accessDeniedMessage by remember { mutableStateOf<String?>(null) }
    var playErrorMessage by remember { mutableStateOf<String?>(null) }
    var browseInitialPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(1200)
        showSplash = false
    }

    if (showSplash) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(NavyS, NavyMidS))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("K", color = GoldS, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Kuntur Media Pro", color = GoldS, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Radio · TV · Películas", color = Color.White.copy(0.65f), fontSize = 14.sp)
            }
        }
        return
    }

    if (accessDeniedMessage != null) {
        AlertDialog(
            onDismissRequest = { accessDeniedMessage = null },
            title = { Text("Contenido Premium") },
            text = { Text(accessDeniedMessage!!) },
            confirmButton = {
                Button(
                    onClick = { accessDeniedMessage = null; section = AppSection.PLANS },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldS)
                ) { Text("Ver planes", color = NavyS) }
            },
            dismissButton = {
                TextButton(onClick = { accessDeniedMessage = null }) { Text("Cerrar", color = GoldS) }
            }
        )
    }

    if (playErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { playErrorMessage = null },
            title = { Text("Error de reproducción") },
            text = { Text(playErrorMessage!!) },
            confirmButton = {
                Button(
                    onClick = { playErrorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldS)
                ) { Text("Aceptar", color = NavyS) }
            }
        )
    }

    fun startPlayback(item: MediaItem) {
        if (!canAccess(context, item)) {
            accessDeniedMessage = "Contenido Premium. Pague por Yape 955238172 y solicite activación."
            return
        }
        currentItem = item
        section = AppSection.PLAYER
        // Activa el servicio para notificación y reproducción en segundo plano
        PlayerHolder.ensureService(context)
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
            val isRadio = item.category.equals("Radio", ignoreCase = true)
            PlayerScreen(
                item = item,
                player = player,
                onBack = {
                    // Radio/TV: seguir en segundo plano al volver.
                    // Películas: se puede pausar si se prefiere; aquí se mantiene la sesión.
                    if (!isRadio && !item.category.equals("TV", ignoreCase = true)) {
                        // Opcional: no detener; la notificación permite controlar.
                    }
                    section = AppSection.BROWSE
                },
                onStop = {
                    try {
                        player.stop()
                        player.clearMediaItems()
                    } catch (_: Exception) {
                    }
                    section = AppSection.BROWSE
                },
                onToggleFavorite = {
                    FavoritesManager.toggle(context, item.id)
                }
            )
        } else {
            section = AppSection.HOME
        }

        AppSection.PLANS -> PlansScreen(onBack = { section = AppSection.HOME })

        AppSection.FAVORITES -> FavoritesScreen(
            onPlay = { startPlayback(it) },
            onBack = { section = AppSection.HOME }
        )

        AppSection.BROWSE -> BrowsePagerScreen(
            initialPage = browseInitialPage,
            onPlay = { startPlayback(it) },
            onBack = { section = AppSection.HOME },
            onPlans = { section = AppSection.PLANS }
        )

        AppSection.HOME -> HomeHub(
            onRadios = {
                browseInitialPage = 0
                section = AppSection.BROWSE
            },
            onChannels = {
                browseInitialPage = 1
                section = AppSection.BROWSE
            },
            onMovies = {
                browseInitialPage = 2
                section = AppSection.BROWSE
            },
            onFavorites = { section = AppSection.FAVORITES },
            onPlans = { section = AppSection.PLANS }
        )
    }
}

@Composable
fun HomeHub(
    onRadios: () -> Unit,
    onChannels: () -> Unit,
    onMovies: () -> Unit,
    onFavorites: () -> Unit,
    onPlans: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyS, NavyMidS)))
            .padding(20.dp)
    ) {
        Text("Kuntur Media Pro", color = GoldS, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Elija una sección · Deslice entre categorías", color = Color.White.copy(0.6f), fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        SectionCard("Radios", "Solo emisoras · ${MediaRepository.radios.size}", Icons.Default.Radio, onRadios)
        Spacer(Modifier.height(12.dp))
        SectionCard("Canales TV", "Solo canales · ${MediaRepository.channels.size}", Icons.Default.Tv, onChannels)
        Spacer(Modifier.height(12.dp))
        SectionCard("Películas", "Solo películas · ${MediaRepository.movies.size}", Icons.Default.Movie, onMovies)
        Spacer(Modifier.height(12.dp))
        SectionCard("Favoritos", "Sus contenidos guardados", Icons.Default.Favorite, onFavorites)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onPlans,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldS),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Planes y Yape 955238172", color = NavyS, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
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
        Triple("Radios", Icons.Default.Radio, MediaRepository.radios),
        Triple("Canales TV", Icons.Default.Tv, MediaRepository.channels),
        Triple("Películas", Icons.Default.Movie, MediaRepository.movies)
    )
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, pages.lastIndex),
        pageCount = { pages.size }
    )
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyS, NavyMidS)))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

        Text(
            "Deslice horizontalmente para cambiar de sección",
            color = Color.White.copy(0.45f),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val (title, icon, items) = pages[page]
            val filtered = if (query.isBlank()) items else items.filter {
                it.title.contains(query, ignoreCase = true)
            }
            CategoryListContent(
                title = title,
                subtitle = "${filtered.size} de ${items.size}",
                icon = icon,
                items = filtered,
                onPlay = onPlay
            )
        }
    }
}

@Composable
fun CategoryListContent(
    title: String,
    subtitle: String,
    icon: ImageVector,
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
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
            item {
                Text(
                    "No hay resultados",
                    color = Color.White.copy(0.5f),
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            items(items, key = { it.id }) { MediaRow(it, onPlay) }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun FavoritesScreen(
    onPlay: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val favorites = remember(refresh) {
        FavoritesManager.favoritesFrom(context, MediaRepository.all())
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyS, NavyMidS)))
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Default.Home, null, tint = GoldS, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Inicio", color = GoldS)
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = GoldS, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Favoritos", color = GoldS, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("${favorites.size} guardados", color = Color.White.copy(0.5f), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (favorites.isEmpty()) {
                item {
                    Text(
                        "Aún no tiene favoritos.\nToque el corazón en cualquier título para guardar.",
                        color = Color.White.copy(0.55f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(favorites, key = { it.id }) { item ->
                    MediaRow(item, onPlay, onFavoriteChanged = { refresh++ })
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}
