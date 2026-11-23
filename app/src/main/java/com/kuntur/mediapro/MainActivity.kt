package com.kuntur.mediapro

import android.app.Activity
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kuntur.mediapro.auth.AuthManager
import com.kuntur.mediapro.auth.GoogleAuthConfig
import com.kuntur.mediapro.billing.PurchaseManager
import com.kuntur.mediapro.billing.TransactionManager
import com.kuntur.mediapro.data.MediaItem
import com.kuntur.mediapro.data.MediaRepository
import com.kuntur.mediapro.player.DrmHelper
import com.kuntur.mediapro.ui.theme.KunturMediaProTheme
import com.kuntur.mediapro.update.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Navy = Color(0xFF0A1628)
private val NavyMid = Color(0xFF132337)
private val CardBg = Color(0xFF1A2D45)
private val Gold = Color(0xFFE8B923)

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        setContent {
            KunturMediaProTheme {
                Surface(Modifier.fillMaxSize(), color = Navy) {
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
    var loggedIn by remember { mutableStateOf(AuthManager.isLoggedIn(context)) }
    var screen by remember { mutableStateOf("home") }
    var currentItem by remember { mutableStateOf<MediaItem?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var accessDeniedMessage by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Solo email/perfil: evita fallos extra por idToken si el Client ID no es Web
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            val name = account?.displayName ?: email.ifBlank { "Usuario Google" }
            if (email.isBlank()) {
                loginError = "No se obtuvo el correo. Intente de nuevo."
            } else {
                AuthManager.login(context, email, name)
                loggedIn = true
                loginError = null
            }
        } catch (e: ApiException) {
            loginError = when (e.statusCode) {
                10 -> "Error 10: SHA-1 del APK no coincide con Google Cloud. Use «Entrar como dueño» o registre el SHA-1 de ESTE APK."
                12501 -> "Inicio de sesión cancelado."
                7 -> "Sin red. Compruebe su conexión."
                else -> "Error Google (${e.statusCode}). Use «Entrar como dueño» si persiste."
            }
        } catch (e: Exception) {
            loginError = e.message ?: "Error al iniciar sesión"
        }
    }

    LaunchedEffect(Unit) {
        delay(1600)
        showSplash = false
        val existing = GoogleSignIn.getLastSignedInAccount(context)
        if (existing?.email != null && !loggedIn) {
            AuthManager.login(context, existing.email!!, existing.displayName ?: existing.email!!)
            loggedIn = true
        }
        updateInfo = UpdateManager.checkForUpdate(context)
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
                googleClient.signOut().addOnCompleteListener {
                    googleLauncher.launch(googleClient.signInIntent)
                }
            },
            onOwnerFallback = {
                AuthManager.loginAsOwner(context)
                loggedIn = true
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
                        scope.launch { UpdateManager.downloadAndInstall(context, url) }
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
                Button(onClick = { accessDeniedMessage = null; screen = "plans" },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
                    Text("Ver planes", color = Navy)
                }
            },
            dismissButton = {
                TextButton(onClick = { accessDeniedMessage = null }) { Text("Cerrar", color = Gold) }
            }
        )
    }

    when {
        screen == "player" && currentItem != null -> PlayerScreen(
            item = currentItem!!, player = player,
            onBack = { player.stop(); screen = "home" }
        )
        screen == "plans" -> PlansScreen(
            onBack = { screen = "home" },
            onLogout = {
                googleClient.signOut()
                AuthManager.logout(context)
                loggedIn = false
            }
        )
        else -> HomeScreen(
            onPlay = { item ->
                val owner = AuthManager.isOwner(context)
                val plan = PurchaseManager.getCurrentPlan(context)
                val approved = PurchaseManager.isApproved(context)
                val canPlay = owner || !item.isPremium || (approved && plan != "free")
                if (!canPlay) {
                    accessDeniedMessage =
                        "Contenido Premium. Pague por Yape 955238172 y espere aprobación."
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
fun LoginScreen(error: String?, onGoogle: () -> Unit, onOwnerFallback: () -> Unit) {
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

            Spacer(Modifier.height(18.dp))
            TextButton(onClick = onOwnerFallback) {
                Text("Entrar como dueño (sin Google)", color = Gold, fontSize = 13.sp)
            }
            Text(
                "El acceso de dueño desbloquea todo el contenido.",
                color = Color.White.copy(0.4f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun HomeScreen(onPlay: (MediaItem) -> Unit, onPlans: () -> Unit) {
    val context = LocalContext.current
    val plan = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)
    val name = AuthManager.getDisplayName(context)
    val owner = AuthManager.isOwner(context)

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, NavyMid)))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text("Hola, $name", color = Color.White.copy(0.6f), fontSize = 13.sp)
        Text("Kuntur Media Pro", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            if (owner) "Acceso dueño · Todo desbloqueado"
            else "Plan: $plan ${if (approved) "✓" else "· pendiente"}",
            color = Color.White.copy(0.5f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onPlans,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Planes y Yape 955238172", color = Navy, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(22.dp))
        SectionTitle("Radios")
        MediaList(MediaRepository.radios, onPlay)
        Spacer(Modifier.height(14.dp))
        SectionTitle("Canales TV")
        MediaList(MediaRepository.channels, onPlay)
        Spacer(Modifier.height(14.dp))
        SectionTitle("Películas")
        MediaList(MediaRepository.movies, onPlay)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
fun MediaList(items: List<MediaItem>, onPlay: (MediaItem) -> Unit) {
    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
        items(items) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPlay(item) },
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Navy)
                            .border(1.dp, Gold.copy(0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (item.isPremium) Icons.Default.Lock else Icons.Default.PlayArrow, null, tint = Gold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(item.category, color = Color.White.copy(0.45f), fontSize = 12.sp)
                    }
                    if (item.isPremium) Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(item: MediaItem, player: ExoPlayer, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Navy).padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Volver", color = Gold) }
        Text(item.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp))
        )
        Spacer(Modifier.height(16.dp))
        Text("Reproduciendo: ${item.category}", color = Color.White.copy(0.6f))
    }
}

@Composable
fun PlansScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val current = PurchaseManager.getCurrentPlan(context)
    val approved = PurchaseManager.isApproved(context)
    val owner = AuthManager.isOwner(context)

    Column(Modifier.fillMaxSize().background(Navy).padding(18.dp)) {
        TextButton(onClick = onBack) { Text("← Volver", color = Gold) }
        Text("Planes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Yape: 955238172", color = Gold, fontWeight = FontWeight.SemiBold)
        if (owner) Text("Modo dueño: todo activo", color = Color(0xFF4CAF50), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        PurchaseManager.plans.forEach { plan ->
            val selected = current == plan.id
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF243B55) else CardBg),
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
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Cerrar sesión", color = Gold)
        }
    }
}
