package com.kuntur.mediapro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kuntur.mediapro.auth.AuthManager
import com.kuntur.mediapro.billing.PurchaseManager
import com.kuntur.mediapro.billing.TransactionManager
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.data.MediaRepository
import com.kuntur.mediapro.player.DrmHelper
import com.kuntur.mediapro.player.StreamPlayerFactory
import com.kuntur.mediapro.ui.theme.KunturMediaProTheme
import com.kuntur.mediapro.update.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Navy = Color(0xFF0A1628)
private val NavyMid = Color(0xFF132337)
private val CardBg = Color(0xFF1A2D45)
private val Gold = Color(0xFFE8B923)

enum class AppSection { HOME, RADIOS, CHANNELS, MOVIES, PLAYER, PLANS }

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = try {
            StreamPlayerFactory.create(this)
        } catch (_: Throwable) {
            try {
                ExoPlayer.Builder(this).build()
            } catch (_: Throwable) {
                null
            }
        }
        setContent {
            KunturMediaProTheme {
                Surface(Modifier.fillMaxSize(), color = Navy) {
                    val p = player
                    if (p != null) AppRoot(p)
                    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error al iniciar el reproductor", color = Color.White)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { player?.release() } catch (_: Throwable) {}
        player = null
    }
}

fun canAccess(context: android.content.Context, item: MediaItem): Boolean {
    if (!item.isPremium) return true
    if (AuthManager.isOwner(context)) return true
    val plan = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)
    return approved && plan != "free"
}

@Composable
fun AppRoot(player: ExoPlayer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }
    var loggedIn by remember { mutableStateOf(AuthManager.isLoggedIn(context)) }
    var section by remember { mutableStateOf(AppSection.HOME) }
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var accessDeniedMessage by remember { mutableStateOf<String?>(null) }
    var playErrorMessage by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleClient = remember {
        try { GoogleSignIn.getClient(context, gso) } catch (_: Exception) { null }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            val name = account?.displayName ?: email.ifBlank { "Usuario Google" }
            if (email.isBlank()) {
                loginError = "No se obtuvo el correo."
            } else {
                AuthManager.login(context, email, name)
                if (AuthManager.isOwner(context)) {
                    PurchaseManager.requestPlan(context, "premium")
                    PurchaseManager.approve(context)
                }
                loggedIn = true
                loginError = null
            }
        } catch (e: ApiException) {
            loginError = when (e.statusCode) {
                10 -> "Error Google (SHA-1). Contacte al administrador."
                12501 -> "Inicio de sesión cancelado."
                7 -> "Sin red."
                else -> "No se pudo iniciar sesión (${e.statusCode})."
            }
        } catch (e: Exception) {
            loginError = e.message ?: "Error al iniciar sesión"
        }
    }

    LaunchedEffect(Unit) {
        delay(1400)
        showSplash = false
        try {
            val existing = GoogleSignIn.getLastSignedInAccount(context)
            if (existing?.email != null && !AuthManager.isLoggedIn(context)) {
                AuthManager.login(context, existing.email!!, existing.displayName ?: existing.email!!)
                loggedIn = true
            }
        } catch (_: Exception) {}
        if (AuthManager.isLoggedIn(context) && AuthManager.isOwner(context)) {
            PurchaseManager.requestPlan(context, "premium")
            PurchaseManager.approve(context)
        }
        loggedIn = AuthManager.isLoggedIn(context)
        try { updateInfo = UpdateManager.checkForUpdate(context) } catch (_: Exception) {}
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    if (!loggedIn) {
        LoginScreen(
            error = loginError,
            onGoogle = {
                loginError = null
                val client = googleClient
                if (client == null) loginError = "Google Play Services no disponible."
                else client.signOut().addOnCompleteListener {
                    googleLauncher.launch(client.signInIntent)
                }
            }
        )
        return
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = {
                UpdateManager.dismiss(context, updateInfo!!.version)
                updateInfo = null
            },
            title = { Text("Actualización disponible") },
            text = { Text("Versión ${updateInfo?.version} lista.") },
            confirmButton = {
                Button(
                    onClick = {
                        val url = updateInfo!!.downloadUrl
                        UpdateManager.dismiss(context, updateInfo!!.version)
                        updateInfo = null
                        scope.launch {
                            try { UpdateManager.downloadAndInstall(context, url) } catch (_: Exception) {}
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) { Text("Actualizar", color = Navy) }
            },
            dismissButton = {
                TextButton(onClick = {
                    UpdateManager.dismiss(context, updateInfo!!.version)
                    updateInfo = null
                }) { Text("Más tarde", color = Gold) }
            }
        )
    }

    if (accessDeniedMessage != null) {
        AlertDialog(
            onDismissRequest = { accessDeniedMessage = null },
            title = { Text("Contenido Premium") },
            text = { Text(accessDeniedMessage!!) },
            confirmButton = {
                Button(
                    onClick = { accessDeniedMessage = null; section = AppSection.PLANS },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) { Text("Ver planes", color = Navy) }
            },
            dismissButton = {
                TextButton(onClick = { accessDeniedMessage = null }) { Text("Cerrar", color = Gold) }
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
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) { Text("Aceptar", color = Navy) }
            }
        )
    }

    fun startPlayback(item: MediaItem) {
        if (!canAccess(context, item)) {
            accessDeniedMessage =
                "Contenido Premium. Pague por Yape 955238172 y espere aprobación."
            return
        }
        currentItem = item
        section = AppSection.PLAYER
        try {
            val live = DrmHelper.isLikelyLiveOrRadio(item.streamUrl, item.category)
            val mediaItem = if (item.isPremium && !item.licenseUrl.isNullOrBlank()) {
                DrmHelper.createProtectedMediaItem(item.streamUrl, item.licenseUrl!!, item.title)
            } else {
                DrmHelper.createClearMediaItem(item.streamUrl, item.title, isLive = live)
            }
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            player.play()
        } catch (e: Exception) {
            playErrorMessage = e.message?.take(120) ?: "No se pudo iniciar la reproducción."
        }
    }

    when (section) {
        AppSection.PLAYER -> if (currentItem != null) {
            PlayerScreen(
                item = currentItem!!,
                player = player,
                onBack = {
                    try {
                        player.stop()
                        player.clearMediaItems()
                    } catch (_: Exception) {}
                    section = when (currentItem?.category?.lowercase()) {
                        "radio" -> AppSection.RADIOS
                        "tv" -> AppSection.CHANNELS
                        else -> AppSection.MOVIES
                    }
                },
                onRetry = {
                    val item = currentItem ?: return@PlayerScreen
                    try {
                        val live = DrmHelper.isLikelyLiveOrRadio(item.streamUrl, item.category)
                        val mediaItem = DrmHelper.createClearMediaItem(item.streamUrl, item.title, isLive = live)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.playWhenReady = true
                        player.play()
                    } catch (_: Exception) {}
                }
            )
        } else {
            section = AppSection.HOME
        }

        AppSection.PLANS -> PlansScreen(
            onBack = { section = AppSection.HOME },
            onLogout = {
                try { googleClient?.signOut() } catch (_: Exception) {}
                AuthManager.logout(context)
                loggedIn = false
            }
        )

        AppSection.RADIOS -> CategoryScreen(
            title = "Emisoras de radio",
            subtitle = "Solo emisoras · ${MediaRepository.radios.size} estaciones",
            icon = Icons.Default.Radio,
            items = MediaRepository.radios,
            onPlay = { startPlayback(it) },
            onBack = { section = AppSection.HOME }
        )

        AppSection.CHANNELS -> CategoryScreen(
            title = "Canales de TV",
            subtitle = "Solo canales · ${MediaRepository.channels.size} señales",
            icon = Icons.Default.Tv,
            items = MediaRepository.channels,
            onPlay = { startPlayback(it) },
            onBack = { section = AppSection.HOME }
        )

        AppSection.MOVIES -> CategoryScreen(
            title = "Películas",
            subtitle = "Solo películas · ${MediaRepository.movies.size} títulos",
            icon = Icons.Default.Movie,
            items = MediaRepository.movies,
            onPlay = { startPlayback(it) },
            onBack = { section = AppSection.HOME }
        )

        AppSection.HOME -> HomeHub(
            onRadios = { section = AppSection.RADIOS },
            onChannels = { section = AppSection.CHANNELS },
            onMovies = { section = AppSection.MOVIES },
            onPlans = { section = AppSection.PLANS }
        )
    }
}

@Composable
fun SplashScreen() {
    val infinite = rememberInfiniteTransition(label = "s")
    val scale by infinite.animateFloat(
        0.94f, 1.06f,
        infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sc"
    )
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, NavyMid, Navy))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(110.dp).scale(scale).shadow(16.dp, CircleShape).clip(CircleShape).background(Gold),
                contentAlignment = Alignment.Center
            ) { Text("K", color = Navy, fontSize = 52.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp))
            Text("Kuntur Media Pro", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Radio · TV · Películas", color = Color.White.copy(0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun LoginScreen(error: String?, onGoogle: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, NavyMid, Color(0xFF0D1B2A))))
    ) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(96.dp).shadow(12.dp, CircleShape).clip(CircleShape).background(Gold),
                contentAlignment = Alignment.Center
            ) { Text("K", color = Navy, fontSize = 44.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(20.dp))
            Text("Kuntur Media Pro", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Inicie sesión con su cuenta Google",
                color = Color.White.copy(0.65f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
            )
            Surface(
                onClick = onGoogle,
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF4285F4)),
                        contentAlignment = Alignment.Center
                    ) { Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    Spacer(Modifier.width(14.dp))
                    Text("Continuar con Google", color = Color(0xFF3C4043), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = Color(0xFFFF8A80), fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Hub principal: tres accesos separados */
@Composable
fun HomeHub(
    onRadios: () -> Unit,
    onChannels: () -> Unit,
    onMovies: () -> Unit,
    onPlans: () -> Unit
) {
    val context = LocalContext.current
    val name = AuthManager.getDisplayName(context)
    val owner = AuthManager.isOwner(context)
    val plan = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)

    LaunchedEffect(owner) {
        if (owner) {
            PurchaseManager.requestPlan(context, "premium")
            PurchaseManager.approve(context)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, NavyMid)))
            .padding(20.dp)
    ) {
        Text("Hola, $name", color = Color.White.copy(0.6f), fontSize = 13.sp)
        Text("Kuntur Media Pro", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            if (owner) "Acceso dueño · Todo desbloqueado"
            else "Plan: $plan ${if (approved) "✓" else "· pendiente"}",
            color = if (owner) Color(0xFF4CAF50) else Color.White.copy(0.5f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(28.dp))
        Text("Elija una sección", color = Color.White.copy(0.7f), fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))

        SectionCard(
            title = "Radios",
            subtitle = "Solo emisoras · ${MediaRepository.radios.size} estaciones",
            icon = Icons.Default.Radio,
            onClick = onRadios
        )
        Spacer(Modifier.height(12.dp))
        SectionCard(
            title = "Canales TV",
            subtitle = "Solo canales · ${MediaRepository.channels.size} señales",
            icon = Icons.Default.Tv,
            onClick = onChannels
        )
        Spacer(Modifier.height(12.dp))
        SectionCard(
            title = "Películas",
            subtitle = "Solo películas · ${MediaRepository.movies.size} títulos",
            icon = Icons.Default.Movie,
            onClick = onMovies
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onPlans,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Planes y Yape 955238172", color = Navy, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun SectionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Navy)
                    .border(1.dp, Gold.copy(0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Gold, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = Color.White.copy(0.5f), fontSize = 13.sp)
            }
            Icon(Icons.Default.PlayArrow, null, tint = Gold)
        }
    }
}

/** Pantalla dedicada: solo radios, solo TV o solo películas */
@Composable
fun CategoryScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Navy, NavyMid)))
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.Home, null, tint = Gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Inicio", color = Gold)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = Gold, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(title, color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = Color.White.copy(0.5f), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            items(items, key = { it.id }) { item ->
                MediaRow(item, onPlay)
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
fun MediaRow(item: MediaItem, onPlay: (MediaItem) -> Unit) {
    val context = LocalContext.current
    val unlocked = canAccess(context, item)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable { onPlay(item) },
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Navy)
                    .border(1.dp, Gold.copy(0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (unlocked) Icons.Default.PlayArrow else Icons.Default.Lock,
                    null,
                    tint = Gold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(
                    if (unlocked) item.category else "${item.category} · Bloqueado",
                    color = Color.White.copy(0.45f),
                    fontSize = 12.sp
                )
            }
            if (item.isPremium && unlocked) {
                Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            } else if (item.isPremium && !unlocked) {
                Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun PlayerScreen(
    item: MediaItem,
    player: ExoPlayer,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING ||
                    playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    errorMsg = null
                    isBuffering = false
                }
                if (playbackState == Player.STATE_ENDED) isPlaying = false
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMsg = StreamPlayerFactory.humanError(error)
                isBuffering = false
            }
        }
        player.addListener(listener)
        isBuffering = player.playbackState == Player.STATE_BUFFERING ||
            player.playbackState == Player.STATE_IDLE
        isPlaying = player.isPlaying
        onDispose {
            try { player.removeListener(listener) } catch (_: Exception) {}
        }
    }

    val sectionLabel = when (item.category.lowercase()) {
        "radio" -> "Reproductor de radio"
        "tv" -> "Reproductor de canales"
        else -> "Reproductor de películas"
    }

    Column(
        Modifier.fillMaxSize().background(Navy).padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Volver", color = Gold) }
        Text(sectionLabel, color = Gold.copy(0.8f), fontSize = 12.sp)
        Text(item.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(item.category, color = Color.White.copy(0.55f), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(if (item.category.equals("Radio", true)) 180.dp else 260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        controllerShowTimeoutMs = 4000
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize()
            )

            if (isBuffering && errorMsg == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Gold)
                    Spacer(Modifier.height(8.dp))
                    Text("Cargando…", color = Color.White.copy(0.8f), fontSize = 13.sp)
                }
            }

            if (errorMsg != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(errorMsg!!, color = Color(0xFFFF8A80), fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            errorMsg = null
                            isBuffering = true
                            onRetry()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Navy)
                        Spacer(Modifier.width(8.dp))
                        Text("Reintentar", color = Navy)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        val status = when {
            errorMsg != null -> "Error"
            isBuffering -> "Buffering…"
            isPlaying -> "Reproduciendo"
            else -> "Pausado"
        }
        Text("$status · $sectionLabel", color = Color.White.copy(0.65f), fontSize = 14.sp)
    }
}

@Composable
fun PlansScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val current = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)
    val owner = AuthManager.isOwner(context)

    LazyColumn(
        Modifier.fillMaxSize().background(Navy),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Volver", color = Gold) }
            Text("Planes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Yape: 955238172", color = Gold, fontWeight = FontWeight.SemiBold)
            if (owner) {
                Text("Modo dueño: todo activo", color = Color(0xFF4CAF50), fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
        items(PurchaseManager.plans, key = { it.id }) { plan ->
            val selected = current == plan.id
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFF243B55) else CardBg
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.name, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("S/ ${plan.price}", color = Gold, fontWeight = FontWeight.Bold)
                    }
                    Text(plan.description, color = Color.White.copy(0.6f), fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            PurchaseManager.requestPlan(context, plan.id)
                            TransactionManager.createTransaction(context, plan.id, plan.name, plan.price)
                            if (owner) PurchaseManager.approve(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (selected && approved) "Activo" else "Seleccionar", color = Navy)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Cerrar sesión", color = Gold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
