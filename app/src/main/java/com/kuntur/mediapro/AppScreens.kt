package com.kuntur.mediapro

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kuntur.mediapro.auth.AuthManager
import com.kuntur.mediapro.billing.PurchaseManager
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.data.MediaRepository
import com.kuntur.mediapro.player.DrmHelper
import kotlinx.coroutines.delay

private val NavyS = Color(0xFF0A1628)
private val NavyMidS = Color(0xFF132337)
private val CardBgS = Color(0xFF1A2D45)
private val GoldS = Color(0xFFE8B923)

enum class AppSection { HOME, RADIOS, CHANNELS, MOVIES, PLAYER, PLANS }

fun canAccess(context: android.content.Context, item: MediaItem): Boolean {
    if (!item.isPremium) return true
    if (AuthManager.isOwner(context)) return true
    val plan = PurchaseManager.getCurrentPlan(context)
    return PurchaseManager.isApproved(context) && plan != "free"
}

@Composable
fun AppRoot(player: ExoPlayer) {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var loggedIn by remember { mutableStateOf(AuthManager.isLoggedIn(context)) }
    var section by remember { mutableStateOf(AppSection.HOME) }
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var accessDeniedMessage by remember { mutableStateOf<String?>(null) }
    var playErrorMessage by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestProfile().build()
    }
    val googleClient = remember { try { GoogleSignIn.getClient(context, gso) } catch (_: Exception) { null } }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val email = account?.email ?: ""
            val name = account?.displayName ?: email.ifBlank { "Usuario" }
            if (email.isBlank()) loginError = "No se obtuvo el correo"
            else {
                AuthManager.login(context, email, name)
                if (AuthManager.isOwner(context)) {
                    PurchaseManager.requestPlan(context, "premium")
                    PurchaseManager.approve(context)
                }
                loggedIn = true; loginError = null
            }
        } catch (e: ApiException) {
            loginError = "Error Google (${e.statusCode})"
        } catch (e: Exception) {
            loginError = e.message ?: "Error"
        }
    }
    LaunchedEffect(Unit) {
        delay(1200); showSplash = false
        try {
            val existing = GoogleSignIn.getLastSignedInAccount(context)
            if (existing?.email != null && !AuthManager.isLoggedIn(context)) {
                AuthManager.login(context, existing.email!!, existing.displayName ?: existing.email!!)
                loggedIn = true
            }
        } catch (_: Exception) {}
        if (AuthManager.isLoggedIn(context) && AuthManager.isOwner(context)) {
            PurchaseManager.requestPlan(context, "premium"); PurchaseManager.approve(context)
        }
        loggedIn = AuthManager.isLoggedIn(context)
    }
    if (showSplash) {
        Box(Modifier.fillMaxSize().background(NavyS), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("K", color = GoldS, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text("Kuntur Media Pro", color = GoldS, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        return
    }
    if (!loggedIn) {
        Column(Modifier.fillMaxSize().background(NavyS).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Kuntur Media Pro", color = GoldS, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Button(onClick = {
                loginError = null
                val c = googleClient
                if (c == null) loginError = "Google no disponible"
                else c.signOut().addOnCompleteListener { googleLauncher.launch(c.signInIntent) }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Continuar con Google", color = Color.Black)
            }
            if (!loginError.isNullOrBlank()) Text(loginError!!, color = Color(0xFFFF8A80), fontSize = 12.sp)
        }
        return
    }
    if (accessDeniedMessage != null) {
        AlertDialog(onDismissRequest = { accessDeniedMessage = null }, title = { Text("Contenido Premium") }, text = { Text(accessDeniedMessage!!) },
            confirmButton = { Button(onClick = { accessDeniedMessage = null; section = AppSection.PLANS }, colors = ButtonDefaults.buttonColors(containerColor = GoldS)) { Text("Ver planes", color = NavyS) } },
            dismissButton = { TextButton(onClick = { accessDeniedMessage = null }) { Text("Cerrar", color = GoldS) } })
    }
    if (playErrorMessage != null) {
        AlertDialog(onDismissRequest = { playErrorMessage = null }, title = { Text("Error de reproducción") }, text = { Text(playErrorMessage!!) },
            confirmButton = { Button(onClick = { playErrorMessage = null }, colors = ButtonDefaults.buttonColors(containerColor = GoldS)) { Text("Aceptar", color = NavyS) } })
    }
    fun startPlayback(item: MediaItem) {
        if (!canAccess(context, item)) {
            accessDeniedMessage = "Contenido Premium. Yape 955238172."
            return
        }
        currentItem = item
        section = AppSection.PLAYER
        try {
            val live = DrmHelper.isLikelyLiveOrRadio(item.streamUrl, item.category)
            val mi = DrmHelper.createClearMediaItem(item.streamUrl, item.title, isLive = live)
            player.stop(); player.clearMediaItems(); player.setMediaItem(mi); player.prepare(); player.playWhenReady = true; player.play()
        } catch (e: Exception) {
            playErrorMessage = e.message?.take(120) ?: "No se pudo reproducir"
        }
    }
    when (section) {
        AppSection.PLAYER -> if (currentItem != null) {
            PlayerScreen(currentItem!!, player,
                onBack = {
                    try { player.stop(); player.clearMediaItems() } catch (_: Exception) {}
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
                        player.setMediaItem(DrmHelper.createClearMediaItem(item.streamUrl, item.title, isLive = live))
                        player.prepare(); player.playWhenReady = true; player.play()
                    } catch (_: Exception) {}
                })
        } else section = AppSection.HOME
        AppSection.PLANS -> PlansScreen(onBack = { section = AppSection.HOME }, onLogout = {
            try { googleClient?.signOut() } catch (_: Exception) {}
            AuthManager.logout(context); loggedIn = false
        })
        AppSection.RADIOS -> CategoryScreen("Emisoras de radio", "Solo emisoras · ${MediaRepository.radios.size}", Icons.Default.Radio, MediaRepository.radios, { startPlayback(it) }, { section = AppSection.HOME })
        AppSection.CHANNELS -> CategoryScreen("Canales de TV", "Solo canales · ${MediaRepository.channels.size}", Icons.Default.Tv, MediaRepository.channels, { startPlayback(it) }, { section = AppSection.HOME })
        AppSection.MOVIES -> CategoryScreen("Películas", "Solo películas · ${MediaRepository.movies.size}", Icons.Default.Movie, MediaRepository.movies, { startPlayback(it) }, { section = AppSection.HOME })
        AppSection.HOME -> HomeHub(
            onRadios = { section = AppSection.RADIOS },
            onChannels = { section = AppSection.CHANNELS },
            onMovies = { section = AppSection.MOVIES },
            onPlans = { section = AppSection.PLANS }
        )
    }
}

@Composable
fun HomeHub(onRadios: () -> Unit, onChannels: () -> Unit, onMovies: () -> Unit, onPlans: () -> Unit) {
    val context = LocalContext.current
    val name = AuthManager.getDisplayName(context)
    val owner = AuthManager.isOwner(context)
    LaunchedEffect(owner) {
        if (owner) { PurchaseManager.requestPlan(context, "premium"); PurchaseManager.approve(context) }
    }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS))).padding(20.dp)) {
        Text("Hola, $name", color = Color.White.copy(0.6f), fontSize = 13.sp)
        Text("Kuntur Media Pro", color = GoldS, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(if (owner) "Acceso dueño · Todo desbloqueado" else "Usuario", color = if (owner) Color(0xFF4CAF50) else Color.White.copy(0.5f), fontSize = 12.sp)
        Spacer(Modifier.height(28.dp))
        SectionCard("Radios", "Solo emisoras · ${MediaRepository.radios.size}", Icons.Default.Radio, onRadios)
        Spacer(Modifier.height(12.dp))
        SectionCard("Canales TV", "Solo canales · ${MediaRepository.channels.size}", Icons.Default.Tv, onChannels)
        SectionCard("Películas", "Solo películas · ${MediaRepository.movies.size}", Icons.Default.Movie, onMovies)
        Spacer(Modifier.weight(1f))
        Button(onClick = onPlans, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = GoldS), shape = RoundedCornerShape(14.dp)) {
            Text("Planes y Yape 955238172", color = NavyS, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = CardBgS), shape = RoundedCornerShape(16.dp)) {
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

@Composable
fun CategoryScreen(title: String, subtitle: String, icon: ImageVector, items: List<MediaItem>, onPlay: (MediaItem) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NavyS, NavyMidS)))) {
        TextButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Default.Home, null, tint = GoldS, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text("Inicio", color = GoldS)
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = GoldS, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(title, color = GoldS, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = Color.White.copy(0.5f), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            items(items, key = { it.id }) { MediaRow(it, onPlay) }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}
