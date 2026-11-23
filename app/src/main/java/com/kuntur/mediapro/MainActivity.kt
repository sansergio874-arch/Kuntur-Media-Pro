package com.kuntur.mediapro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var screen by remember { mutableStateOf("home") }
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        updateInfo = UpdateManager.checkForUpdate()
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Actualización disponible") },
            text = { Text("Versión ${updateInfo?.version} lista para instalar sin desinstalar.") },
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
                currentItem = item
                val mediaItem = if (item.isPremium && item.licenseUrl != null) {
                    DrmHelper.createProtectedMediaItem(
                        videoUrl = item.streamUrl,
                        licenseUrl = item.licenseUrl!!
                    )
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
fun HomeScreen(
    onPlay: (MediaItem) -> Unit,
    onPlans: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1C2C), Color(0xFF152536))
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Kuntur Media Pro",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFD4A017),
            fontWeight = FontWeight.Bold
        )
        Text(
            "Radio · TV · Películas",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onPlans,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4A017))
        ) {
            Text("Planes y Yape 955238172", color = Color(0xFF0B1C2C))
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
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun MediaList(items: List<MediaItem>, onPlay: (MediaItem) -> Unit) {
    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
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
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFD4A017)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Text(item.category, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
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
        Text(
            item.title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
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
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier.height(16.dp))
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
        Text(
            "Planes de compra",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text("Pago único vía Yape: 955238172", color = Color(0xFFD4A017))
        Text(
            "Después de pagar, el administrador aprobará su plan de forma privada.",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier.height(16.dp))

        PurchaseManager.plans.forEach { plan ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F42))
            ) {
                Column(modifier.padding(16.dp)) {
                    Text(plan.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("S/ ${plan.price}", color = Color(0xFFD4A017), fontWeight = FontWeight.Bold)
                    Text(plan.description, color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            PurchaseManager.requestPlan(context, plan.id)
                            TransactionManager.createTransaction(context, plan.id, plan.name, plan.price)
                        },
                        enabled = current != plan.id,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4A017))
                    ) {
                        Text(
                            if (current == plan.id) "Solicitado" else "Seleccionar",
                            color = Color(0xFF0B1C2C)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Plan actual: $current ${if (approved) "· Aprobado" else "· Pendiente"}",
            color = Color.White
        )

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Historial de solicitudes", color = Color.White, style = MaterialTheme.typography.titleSmall)
            history.take(5).forEach { tx ->
                Text(
                    "${tx.createdAt} · ${tx.planName} · S/ ${tx.price} · ${tx.status}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
