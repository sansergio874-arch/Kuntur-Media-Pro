package com.kuntur.mediapro

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kuntur.mediapro.billing.BillingConfig
import com.kuntur.mediapro.billing.DeviceId
import com.kuntur.mediapro.billing.PurchaseManager
import com.kuntur.mediapro.billing.SubscriptionApi
import com.kuntur.mediapro.billing.TransactionManager
import com.kuntur.mediapro.data.FavoritesManager
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.player.StreamPlayerFactory
import com.kuntur.mediapro.ui.LogoImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val NavyUi = Color(0xFF0A1628)
private val CardBgUi = Color(0xFF1A2D45)
private val GoldUi = Color(0xFFE8B923)

enum class VideoQuality(val label: String, val maxHeight: Int, val maxBitrate: Int) {
    LOW("Baja", 360, 800_000),
    MEDIUM("Media", 480, 1_500_000),
    HIGH("Alta", 720, 3_000_000),
    ULTRA("Ultra", 1080, 8_000_000),
    MAX("Máxima", Int.MAX_VALUE, Int.MAX_VALUE)
}

fun applyQuality(player: ExoPlayer, quality: VideoQuality) {
    try {
        val params = player.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(quality.maxHeight * 2, quality.maxHeight)
            .setMaxVideoBitrate(quality.maxBitrate)
            .setForceHighestSupportedBitrate(quality == VideoQuality.MAX)
            .build()
        player.trackSelectionParameters = params
    } catch (_: Throwable) {}
}

@Composable
fun MediaRow(item: MediaItem, onPlay: (MediaItem) -> Unit, onFavoriteChanged: (() -> Unit)? = null) {
    val context = LocalContext.current
    val unlocked = canAccess(context, item)
    var isFav by remember(item.id) { mutableStateOf(FavoritesManager.isFavorite(context, item.id)) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).shadow(4.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)), colors = CardDefaults.cardColors(containerColor = CardBgUi), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clickable { onPlay(item) }, verticalAlignment = Alignment.CenterVertically) {
                LogoImage(logoUrl = item.logoUrl, category = item.category, size = 48.dp, corner = 10.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text(if (unlocked) item.category else "${item.category} · Bloqueado", color = Color.White.copy(0.45f), fontSize = 12.sp)
                }
            }
            IconButton(onClick = { isFav = FavoritesManager.toggle(context, item.id); onFavoriteChanged?.invoke() }) {
                Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorito", tint = if (isFav) Color(0xFFE53935) else GoldUi)
            }
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
fun PlayerScreen(item: MediaItem, player: ExoPlayer, onBack: () -> Unit, onStop: () -> Unit = onBack, onToggleFavorite: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isFav by remember(item.id) { mutableStateOf(FavoritesManager.isFavorite(context, item.id)) }
    var isFullscreen by remember { mutableStateOf(false) }
    var currentQuality by remember { mutableStateOf(VideoQuality.HIGH) }
    var showQualityMenu by remember { mutableStateOf(false) }
    val isRadio = item.category.equals("Radio", ignoreCase = true)
    val isVideo = !isRadio

    fun applyImmersive(enable: Boolean) {
        val act = activity ?: return
        try {
            val window = act.window
            WindowCompat.setDecorFitsSystemWindows(window, !enable)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (enable) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes = window.attributes.apply {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (_: Throwable) {}
    }

    LaunchedEffect(isFullscreen) {
        try {
            activity?.requestedOrientation = if (isFullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: Throwable) {}
        applyImmersive(isFullscreen)
    }

    DisposableEffect(Unit) {
        onDispose {
            try { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT } catch (_: Throwable) {}
            applyImmersive(false)
        }
    }

    LaunchedEffect(currentQuality) { try { applyQuality(player, currentQuality) } catch (_: Throwable) {} }

    DisposableEffect(player) {
        try {
            isPlaying = player.isPlaying
            isBuffering = player.playbackState == Player.STATE_BUFFERING || (player.playWhenReady && !player.isPlaying && player.playbackState != Player.STATE_ENDED)
        } catch (_: Throwable) {}
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                try {
                    when (playbackState) {
                        Player.STATE_IDLE -> isBuffering = player.playWhenReady
                        Player.STATE_BUFFERING -> isBuffering = true
                        Player.STATE_READY -> { errorMsg = null; isBuffering = player.playWhenReady && !player.isPlaying }
                        Player.STATE_ENDED -> { isPlaying = false; isBuffering = false }
                    }
                } catch (_: Throwable) {}
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                try {
                    isPlaying = playing
                    isBuffering = !playing && player.playWhenReady && player.playbackState != Player.STATE_ENDED
                } catch (_: Throwable) {}
            }
            override fun onPlayerError(error: PlaybackException) {
                try { errorMsg = StreamPlayerFactory.humanError(error); isBuffering = false } catch (_: Throwable) {}
            }
        }
        try { player.addListener(listener) } catch (_: Throwable) {}
        onDispose { try { player.removeListener(listener) } catch (_: Exception) {} }
    }

    val sectionLabel = when (item.category.lowercase()) {
        "radio" -> "Reproductor de radio · segundo plano activo"
        "tv" -> "Reproductor de canales · segundo plano activo"
        else -> "Reproductor de películas"
    }
    val videoResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        if (!isFullscreen) {
            Row(Modifier.fillMaxWidth().background(NavyUi).padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(8.dp)) { Text("← Volver", color = GoldUi, fontSize = 14.sp) }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sectionLabel, color = GoldUi.copy(0.75f), fontSize = 11.sp, maxLines = 1)
                }
                IconButton(onClick = { try { isFav = FavoritesManager.toggle(context, item.id); onToggleFavorite() } catch (_: Throwable) {} }) {
                    Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorito", tint = if (isFav) Color(0xFFE53935) else GoldUi)
                }
                if (isVideo) IconButton(onClick = { isFullscreen = true }) { Icon(Icons.Default.Fullscreen, contentDescription = "Pantalla completa", tint = GoldUi) }
            }
        }

        Box(
            modifier = if (isFullscreen || isVideo) Modifier.fillMaxSize().background(Color.Black)
            else Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp, vertical = 12.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        resizeMode = videoResizeMode
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        controllerShowTimeoutMs = 3000
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        try { setShowSubtitleButton(false) } catch (_: Throwable) {}
                        setKeepContentOnPlayerReset(true)
                    }
                },
                update = { view ->
                    try {
                        if (view.player !== player) view.player = player
                        view.resizeMode = videoResizeMode
                    } catch (_: Throwable) {}
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isFullscreen) {
                Row(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    IconButton(onClick = { isFullscreen = false }) { Icon(Icons.Default.FullscreenExit, null, tint = GoldUi) }
                }
            }
            if (isBuffering && errorMsg == null) CircularProgressIndicator(color = GoldUi, strokeWidth = 3.dp)
            if (errorMsg != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp).background(Color.Black.copy(0.8f), RoundedCornerShape(12.dp)).padding(20.dp)) {
                    Text(errorMsg!!, color = Color(0xFFFF8A80), fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { errorMsg = null; try { player.prepare(); player.playWhenReady = true; player.play() } catch (_: Exception) {} }, colors = ButtonDefaults.buttonColors(containerColor = GoldUi)) { Text("Reintentar", color = NavyUi) }
                }
            }
        }

        if (!isFullscreen) {
            Row(Modifier.fillMaxWidth().background(NavyUi).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledIconButton(onClick = { try { if (player.isPlaying) player.pause() else player.play() } catch (_: Throwable) {} }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = GoldUi)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = NavyUi)
                }
                OutlinedButton(onClick = onStop, colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldUi)) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Detener")
                }
                val status = when {
                    errorMsg != null -> "Error"
                    isBuffering -> "Cargando…"
                    isPlaying -> if (isRadio) "En segundo plano" else "Reproduciendo"
                    else -> "Pausado"
                }
                Text(status, color = Color.White.copy(0.75f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (isVideo) {
                    IconButton(onClick = { showQualityMenu = true }) { Icon(Icons.Default.Settings, contentDescription = "Calidad", tint = GoldUi) }
                    Text(currentQuality.label, color = GoldUi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (isRadio) Text("Puede minimizar la aplicación: la radio seguirá sonando.", color = Color.White.copy(0.5f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            else Text("Pantalla completa: sin franjas, sin barras del sistema.", color = Color.White.copy(0.45f), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }

    if (showQualityMenu) {
        AlertDialog(
            onDismissRequest = { showQualityMenu = false },
            title = { Text("Calidad de video") },
            text = {
                Column {
                    VideoQuality.entries.forEach { q ->
                        Row(Modifier.fillMaxWidth().clickable { currentQuality = q; try { applyQuality(player, q) } catch (_: Throwable) {}; showQualityMenu = false }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = currentQuality == q, onClick = { currentQuality = q; try { applyQuality(player, q) } catch (_: Throwable) {}; showQualityMenu = false }, colors = RadioButtonDefaults.colors(selectedColor = GoldUi))
                            Spacer(Modifier.width(8.dp))
                            Text(q.label, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQualityMenu = false }) { Text("Cerrar", color = GoldUi) } }
        )
    }
}

@Composable
fun PlansScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusLabel by remember { mutableStateOf(PurchaseManager.getStatusLabel(context)) }
    var txCode by remember { mutableStateOf(PurchaseManager.getTransactionCode(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(TransactionManager.getHistory(context)) }
    var isOwner by remember { mutableStateOf(PurchaseManager.isOwnerDevice(context)) }
    var selectedPlanForAdmin by remember { mutableStateOf(PurchaseManager.getCurrentPlan(context)) }
    val current = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)
    val backendOk = BillingConfig.isConfigured

    fun refreshStatus() {
        statusLabel = PurchaseManager.getStatusLabel(context)
        txCode = PurchaseManager.getTransactionCode(context)
        history = TransactionManager.getHistory(context)
        isOwner = PurchaseManager.isOwnerDevice(context)
    }

    fun refreshFromServer() {
        if (!backendOk) { message = "Cuando se confirme su pago, el plan se activará."; return }
        scope.launch {
            loading = true; message = null
            val deviceId = DeviceId.get(context)
            val result = withContext(Dispatchers.IO) { SubscriptionApi.latestApproved(deviceId) }
            result.onSuccess { sub ->
                if (sub != null) {
                    PurchaseManager.applyRemoteApproved(context, sub.planId, sub.transactionCode)
                    TransactionManager.updateStatus(context, sub.transactionCode, "approved")
                    refreshStatus(); message = "Plan activado: ${sub.planName}"
                } else message = "Aún no hay activación. Espere la confirmación del pago."
            }.onFailure { message = it.message ?: "Error de red" }
            loading = false
        }
    }

    fun requestPlan(planId: String, planName: String, price: Int) {
        scope.launch {
            loading = true; message = null
            if (!backendOk) {
                val localCode = "KNT-${planId.take(3).uppercase()}-${(1000..9999).random()}"
                PurchaseManager.requestPlan(context, planId, localCode)
                TransactionManager.createTransaction(context, planId, planName, price, localCode, "pending")
                selectedPlanForAdmin = planId; refreshStatus()
                message = "Solicitud creada. Yapee S/ $price al ${BillingConfig.YAPE_NUMBER} e indique el código $localCode."
                loading = false; return@launch
            }
            val deviceId = DeviceId.get(context)
            val result = withContext(Dispatchers.IO) { SubscriptionApi.createPending(deviceId, planId, planName, price) }
            result.onSuccess { sub ->
                PurchaseManager.requestPlan(context, planId, sub.transactionCode)
                TransactionManager.createTransaction(context, planId, planName, price, sub.transactionCode, "pending")
                selectedPlanForAdmin = planId; refreshStatus()
                message = "Solicitud registrada. Yapee S/ $price al ${BillingConfig.YAPE_NUMBER} e indique ${sub.transactionCode}."
            }.onFailure { message = it.message ?: "No se pudo registrar" }
            loading = false
        }
    }

    fun activatePlanAsOwner() {
        val ok = PurchaseManager.activatePendingAsOwner(context)
        if (ok) {
            TransactionManager.updateStatus(context, txCode.ifBlank { "ADMIN" }, "approved")
            refreshStatus(); message = "Plan activado."
        } else message = "Solicite un plan primero."
    }

    LazyColumn(Modifier.fillMaxSize().background(NavyUi), contentPadding = PaddingValues(18.dp)) {
        item {
            TextButton(onClick = onBack) { Text("← Volver", color = GoldUi) }
            Text("Planes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Yape: ${BillingConfig.YAPE_NUMBER}", color = GoldUi, fontWeight = FontWeight.SemiBold)
            Text(statusLabel, color = Color.White.copy(0.75f), fontSize = 14.sp)
            if (txCode.isNotBlank()) Text("Código: $txCode", color = GoldUi, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
            Text("Solicite un plan, realice el pago por Yape indicando el código y espere la activación.", color = Color.White.copy(0.55f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(12.dp))
            if (backendOk) Button(onClick = { refreshFromServer() }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = GoldUi), modifier = Modifier.fillMaxWidth()) {
                Text(if (loading) "Consultando…" else "Verificar activación", color = NavyUi, fontWeight = FontWeight.Bold)
            }
            if (message != null) Text(message!!, color = Color(0xFF81C784), fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(16.dp))
        }
        items(PurchaseManager.plans, key = { it.id }) { plan ->
            val selected = current == plan.id
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF243B55) else CardBgUi), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.name, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("S/ ${plan.price}", color = GoldUi, fontWeight = FontWeight.Bold)
                    }
                    Text(plan.description, color = Color.White.copy(0.6f), fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { selectedPlanForAdmin = plan.id; requestPlan(plan.id, plan.name, plan.price) }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = GoldUi), shape = RoundedCornerShape(10.dp)) {
                        Text(when { selected && approved -> "Activo"; selected && !approved -> "Pendiente"; else -> "Solicitar plan" }, color = NavyUi)
                    }
                }
            }
        }
        if (isOwner) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("Administración", color = GoldUi, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Active el plan pendiente tras confirmar el pago.", color = Color.White.copy(0.55f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
                Button(onClick = { activatePlanAsOwner() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Text("Activar plan pendiente", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (history.isNotEmpty()) {
            item { Spacer(Modifier.height(20.dp)); Text("Historial", color = Color.White, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)) }
            items(history.take(10), key = { it.id }) { tx ->
                Text("${tx.createdAt} · ${tx.planName} · S/ ${tx.price} · ${tx.status}", color = Color.White.copy(0.55f), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}
