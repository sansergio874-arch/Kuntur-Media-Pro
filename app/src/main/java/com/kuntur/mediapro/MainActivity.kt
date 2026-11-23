package com.kuntur.mediapro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kuntur.mediapro.billing.PurchaseManager
import com.kuntur.mediapro.billing.TransactionManager
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.data.MediaRepository
import com.kuntur.mediapro.player.DrmHelper
import com.kuntur.mediapro.ui.theme.KunturMediaProTheme
import com.kuntur.mediapro.update.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        setContent {
            KunturMediaProTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(player!!)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}

@Composable
fun AppRoot(player: ExoPlayer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }
    var screen by remember { mutableStateOf("home") }
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var accessDeniedMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        delay(2200)
        showSplash = false
        updateInfo = UpdateManager.checkForUpdate()
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Actualización disponible") },
            text = { Text("Versión ${updateInfo?.version} lista. Se instala sin desinstalar la actual.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        UpdateManager.downloadAndInstall(context, updateInfo!!.downloadUrl)
                    }
                    updateInfo = null
                }) { Text("Actualizar ahora") }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) { Text("Más tarde") }
            }
        )
    }

    if (accessDeniedMessage != null) {
        AlertDialog(
            onDismissRequest = { accessDeniedMessage = null },
            title = { Text("Contenido Premium") },
            text = { Text(accessDeniedMessage!!) },
            confirmButton = {
                Button(onClick = {
                    accessDeniedMessage = null
                    screen = "plans"
                }) { Text("Ver planes") }
            },
            dismissButton = {
                TextButton(onClick = { accessDeniedMessage = null }) { Text("Cerrar") }
            }
        )
    }

    when {
        screen == "player" && currentItem != null -> PlayerScreen(
            item = currentItem!!,
            player = player,
            onBack = {
                player.stop()
                screen = "home"
            }
        )
        screen == "plans" -> PlansScreen(onBack = { screen = "home" })
        else -> HomeScreen(
            onPlay = { item ->
                val plan = PurchaseManager.getCurrentPlan(context)
                val approved = PurchaseManager.isApproved(context)
                val canPlay = !item.isPremium || (approved && plan != "free")

                if (!canPlay) {
                    accessDeniedMessage =
                        "Este contenido es Premium. Adquiera un plan y espere la aprobación privada tras pagar por Yape 955238172."
                    return@HomeScreen
                }

                currentItem = item
                val mediaItem = if (item.isPremium && !item.licenseUrl.isNullOrBlank()) {
                    DrmHelper.createProtectedMediaItem(item.streamUrl, item.licenseUrl!!)
                } else {
                    DrmHelper.createClearMediaItem(item.streamUrl)
                }
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                screen = "player"
            },
            onPlans = { screen = "plans" }
        )
    }
}

@Composable
fun SplashScreen() {
    val infinite = rememberInfiniteTransition(label = "splash")
    val scale by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B1C2C), Color(0xFF152536), Color(0xFF0B1C2C)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0xFFD4A017)),
                contentAlignment = Alignment.Center
            ) {
                Text("K", color = Color(0xFF0B1C2C), fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Kuntur Media Pro",
                color = Color(0xFFD4A017),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Radio · TV · Películas",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(
                color = Color(0xFFD4A017),
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun HomeScreen(onPlay: (MediaItem) -> Unit, onPlans: () -> Unit) {
    val context = LocalContext.current
    val plan = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1C2C), Color(0xFF152536))))
            .padding(16.dp)
    ) {
        Text(
            "Kuntur Media Pro",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFD4A017),
            fontWeight = FontWeight.Bold
        )
        Text("Radio · TV · Películas", color = Color.White.copy(alpha = 0.8f))
        Text(
            "Plan: $plan ${if (approved) "✓" else "(pendiente)"}",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onPlans,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4A017)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Planes y Yape 955238172", color = Color(0xFF0B1C2C), fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Radios")
        MediaList(MediaRepository.radios, onPlay)
        Spacer(Modifier.height(12.dp))
        SectionTitle("Canales TV")
        MediaList(MediaRepository.channels, onPlay)
        Spacer(Modifier.height(12.dp))
        SectionTitle("Películas")
        MediaList(MediaRepository.movies, onPlay)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun MediaList(items: List<MediaItem>, onPlay: (MediaItem) -> Unit) {
    LazyColumn(modifier = Modifier.heightIn(max = 210.dp)) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlay(item) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F42))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B1C2C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (item.isPremium) Icons.Default.Lock else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFD4A017)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Text(item.category, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall)
                    }
                    if (item.isPremium) {
                        Icon(Icons.Default.Star, contentDescription = "Premium", tint = Color(0xFFD4A017))
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(item: MediaItem, player: ExoPlayer, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1C2C))
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Volver", color = Color(0xFFD4A017))
        }
        Text(item.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        if (item.isPremium) {
            Text("Contenido Premium", color = Color(0xFFD4A017), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(16.dp))
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(16.dp))
        Text("Reproduciendo: ${item.category}", color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun PlansScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val current = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)
    val history = TransactionManager.getHistory(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1C2C))
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Volver", color = Color(0xFFD4A017))
        }
        Text("Planes de compra", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Pago único vía Yape: 955238172", color = Color(0xFFD4A017), fontWeight = FontWeight.SemiBold)
        Text(
            "Tras pagar, el administrador aprueba su plan de forma privada. No hay panel visible para otros usuarios.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        PurchaseManager.plans.forEach { plan ->
            val selected = current == plan.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFF2A3F55) else Color(0xFF1E2F42)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.name, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("S/ ${plan.price}", color = Color(0xFFD4A017), fontWeight = FontWeight.Bold)
                    }
                    Text(plan.description, color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            PurchaseManager.requestPlan(context, plan.id)
                            TransactionManager.createTransaction(context, plan.id, plan.name, plan.price)
                        },
                        enabled = !selected,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4A017)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (selected) "Solicitado" else "Seleccionar y pagar por Yape", color = Color(0xFF0B1C2C))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Estado: $current ${if (approved) "· Aprobado ✓" else "· Pendiente de aprobación"}",
            color = Color.White,
            textAlign = TextAlign.Start
        )

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Historial de solicitudes", color = Color.White, style = MaterialTheme.typography.titleSmall)
            history.take(6).forEach { tx ->
                Text(
                    "${tx.createdAt} · ${tx.planName} · S/ ${tx.price} · ${tx.status}",
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
