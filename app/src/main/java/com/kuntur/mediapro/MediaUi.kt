package com.kuntur.mediapro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kuntur.mediapro.billing.PurchaseManager
import com.kuntur.mediapro.billing.TransactionManager
import com.kuntur.mediapro.data.FavoritesManager
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.player.StreamPlayerFactory
import com.kuntur.mediapro.ui.LogoImage

private val NavyUi = Color(0xFF0A1628)
private val CardBgUi = Color(0xFF1A2D45)
private val GoldUi = Color(0xFFE8B923)

@Composable
fun MediaRow(
    item: MediaItem,
    onPlay: (MediaItem) -> Unit,
    onFavoriteChanged: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val unlocked = canAccess(context, item)
    var isFav by remember(item.id) { mutableStateOf(FavoritesManager.isFavorite(context, item.id)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBgUi),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Área principal: reproduce
            Row(
                Modifier
                    .weight(1f)
                    .clickable { onPlay(item) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                LogoImage(logoUrl = item.logoUrl, category = item.category, size = 48.dp, corner = 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text(
                        if (unlocked) item.category else "${item.category} · Bloqueado",
                        color = Color.White.copy(0.45f),
                        fontSize = 12.sp
                    )
                }
            }

            // Botón favorito
            IconButton(
                onClick = {
                    isFav = FavoritesManager.toggle(context, item.id)
                    onFavoriteChanged?.invoke()
                }
            ) {
                Icon(
                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFav) Color(0xFFE53935) else GoldUi
                )
            }

            // Botón reproducir / estado
            IconButton(onClick = { onPlay(item) }) {
                when {
                    item.isPremium && unlocked -> Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF4CAF50))
                    item.isPremium -> Icon(Icons.Default.Star, null, tint = GoldUi)
                    unlocked -> Icon(Icons.Default.PlayArrow, null, tint = GoldUi)
                    else -> Icon(Icons.Default.Lock, null, tint = GoldUi)
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(
    item: MediaItem,
    player: ExoPlayer,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit = {}
) {
    val context = LocalContext.current
    var videoScale by remember(item.id) { mutableFloatStateOf(1f) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isFav by remember(item.id) { mutableStateOf(FavoritesManager.isFavorite(context, item.id)) }

    val isRadio = item.category.equals("Radio", ignoreCase = true)
    val isVideo = !isRadio

    DisposableEffect(player) {
        isPlaying = player.isPlaying
        isBuffering = player.playbackState == Player.STATE_BUFFERING ||
            (player.playWhenReady && !player.isPlaying && player.playbackState != Player.STATE_ENDED)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> isBuffering = player.playWhenReady
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        errorMsg = null
                        isBuffering = player.playWhenReady && !player.isPlaying
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        isBuffering = false
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                isBuffering = !playing && player.playWhenReady && player.playbackState != Player.STATE_ENDED
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMsg = StreamPlayerFactory.humanError(error)
                isBuffering = false
            }
        }
        player.addListener(listener)
        onDispose {
            try {
                player.removeListener(listener)
            } catch (_: Exception) {
            }
        }
    }

    val sectionLabel = when (item.category.lowercase()) {
        "radio" -> "Reproductor de radio"
        "tv" -> "Reproductor de canales"
        else -> "Reproductor de películas"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Barra superior
        Row(
            Modifier
                .fillMaxWidth()
                .background(NavyUi)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(8.dp)) {
                Text("← Volver", color = GoldUi, fontSize = 14.sp)
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    sectionLabel,
                    color = GoldUi.copy(0.75f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = {
                    isFav = FavoritesManager.toggle(context, item.id)
                    onToggleFavorite()
                }
            ) {
                Icon(
                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFav) Color(0xFFE53935) else GoldUi
                )
            }
        }

        // Área de video / audio
        Box(
            modifier = if (isVideo) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        controllerShowTimeoutMs = 4000
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(item.id) {
                        detectTransformGestures { _, _, zoom, _ ->
                            videoScale = (videoScale * zoom).coerceIn(1f, 3f)
                        }
                    }
                    .graphicsLayer {
                        scaleX = videoScale
                        scaleY = videoScale
                    }
            )
            if (isBuffering && errorMsg == null) {
                CircularProgressIndicator(color = GoldUi, strokeWidth = 3.dp)
            }
            if (errorMsg != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color.Black.copy(0.8f), RoundedCornerShape(12.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        errorMsg!!,
                        color = Color(0xFFFF8A80),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Compruebe su conexión o elija otra fuente.",
                        color = Color.White.copy(0.72f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            errorMsg = null
                            try {
                                player.prepare()
                                player.playWhenReady = true
                                player.play()
                            } catch (_: Exception) {
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldUi)
                    ) {
                        Text("Reintentar", color = NavyUi)
                    }
                }
            }
        }

        // Controles extra visibles
        Row(
            Modifier
                .fillMaxWidth()
                .background(NavyUi)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Play / Pause grande
            FilledIconButton(
                onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = GoldUi)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = NavyUi
                )
            }

            val status = when {
                errorMsg != null -> "Error"
                isBuffering -> "Cargando…"
                isPlaying -> "Reproduciendo"
                else -> "Pausado"
            }
            Text(
                status,
                color = Color.White.copy(0.75f),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )

            if (!isRadio) {
                LogoImage(
                    logoUrl = item.logoUrl,
                    category = item.category,
                    size = 36.dp,
                    corner = 8.dp
                )
            }
        }

        if (isRadio) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun PlansScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val current = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)
    val history = remember { TransactionManager.getHistory(context) }

    LazyColumn(
        Modifier.fillMaxSize().background(NavyUi),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Volver", color = GoldUi) }
            Text("Planes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Yape: 955238172", color = GoldUi, fontWeight = FontWeight.SemiBold)
            Text(
                "Pague el plan y envíe el comprobante. La activación es manual.",
                color = Color.White.copy(0.55f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
        }
        items(PurchaseManager.plans, key = { it.id }) { plan ->
            val selected = current == plan.id
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFF243B55) else CardBgUi
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.name, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("S/ ${plan.price}", color = GoldUi, fontWeight = FontWeight.Bold)
                    }
                    Text(plan.description, color = Color.White.copy(0.6f), fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            PurchaseManager.requestPlan(context, plan.id)
                            TransactionManager.createTransaction(context, plan.id, plan.name, plan.price)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldUi),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (selected && approved) "Activo" else "Seleccionar", color = NavyUi)
                    }
                }
            }
        }
        if (history.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("Historial de solicitudes", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            items(history.take(10), key = { it.id }) { tx ->
                Text(
                    "${tx.createdAt} · ${tx.planName} · S/ ${tx.price} · ${tx.status}",
                    color = Color.White.copy(0.55f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}
