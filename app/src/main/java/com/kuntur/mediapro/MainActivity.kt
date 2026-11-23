package com.kuntur.mediapro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kuntur.mediapro.billing.PurchaseManager
import com.kuntur.mediapro.billing.TransactionManager
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.data.MediaRepository
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
            text = { Text("Versión ${updateInfo?.version} lista para instalar.") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        UpdateManager.downloadAndInstall(context, updateInfo!!.downloadUrl)
                    }
                    updateInfo = null
                }) { Text("Actualizar") }
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
            onBack = { screen = "home" }
        )
        screen == "plans" -> PlansScreen(onBack = { screen = "home" })
        else -> HomeScreen(
            onPlay = { item ->
                currentItem = item
                player.setMediaItem(ExoMediaItem.fromUri(item.streamUrl))
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Kuntur Media Pro",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Radio · TV · Películas", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedButton(onClick = onPlans, modifier = Modifier.fillMaxWidth()) {
            Text("Planes y Yape")
        }
        Spacer(Modifier.height(16.dp))

        Text("Radios", style = MaterialTheme.typography.titleMedium)
        MediaList(MediaRepository.radios, onPlay)
        Spacer(Modifier.height(12.dp))
        Text("Canales TV", style = MaterialTheme.typography.titleMedium)
        MediaList(MediaRepository.channels, onPlay)
        Spacer(modifier.height(12.dp))
        Text("Películas", style = MaterialTheme.typography.titleMedium)
        MediaList(MediaRepository.movies, onPlay)
    }
}

@Composable
fun MediaList(items: List<MediaItem>, onPlay: (MediaItem) -> Unit) {
    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
        items(items) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onPlay(item) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        if (item.isPremium) {
                            Text(
                                "Premium",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(item: MediaItem, player: ExoPlayer, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Volver") }
        Text(item.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier.height(16.dp))
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(240.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Reproduciendo: ${item.category}")
    }
}

@Composable
fun PlansScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val current = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)
    val history = TransactionManager.getHistory(context)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Volver") }
        Text("Planes de compra", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier.height(8.dp))
        Text("Pago único vía Yape: 955238172")
        Text("Después de pagar, el administrador aprobará su plan de forma privada.")
        Spacer(modifier.height(16.dp))

        PurchaseManager.plans.forEach { plan ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                    Text("S/ ${plan.price}", color = MaterialTheme.colorScheme.primary)
                    Text(plan.description)
                    Button(
                        onClick = {
                            PurchaseManager.requestPlan(context, plan.id)
                            TransactionManager.createTransaction(context, plan.id, plan.name, plan.price)
                        },
                        enabled = current != plan.id
                    ) {
                        Text(if (current == plan.id) "Solicitado" else "Seleccionar")
                    }
                }
            }
        }

        Spacer(modifier.height(16.dp))
        Text("Plan actual: $current ${if (approved) "· Aprobado" else "· Pendiente"}")

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Historial de solicitudes", style = MaterialTheme.typography.titleSmall)
            history.take(5).forEach { tx ->
                Text("${tx.createdAt} · ${tx.planName} · S/ ${tx.price} · ${tx.status}")
            }
        }
    }
}
