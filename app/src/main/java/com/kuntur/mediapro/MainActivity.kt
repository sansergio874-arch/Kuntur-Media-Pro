package com.kuntur.mediapro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer
import com.kuntur.mediapro.player.PlaybackService
import com.kuntur.mediapro.player.PlayerHolder
import com.kuntur.mediapro.player.StreamPlayerFactory
import com.kuntur.mediapro.ui.theme.KunturMediaProTheme

class MainActivity : ComponentActivity() {

    private var playerState by mutableStateOf<ExoPlayer?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* opcional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        startPlaybackService()

        // Reutilizar el player del servicio si ya existe; si no, crear uno local temporal.
        val existing = PlayerHolder.player
        playerState = existing ?: try {
            StreamPlayerFactory.create(this).also { PlayerHolder.attach(it) }
        } catch (_: Throwable) {
            try {
                ExoPlayer.Builder(this).build().also { PlayerHolder.attach(it) }
            } catch (_: Throwable) {
                null
            }
        }

        setContent {
            KunturMediaProTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0A1628)) {
                    val p = playerState ?: PlayerHolder.player
                    if (p != null) {
                        AppRoot(p)
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error al iniciar el reproductor", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Sincronizar referencia si el servicio ya creó el player.
        PlayerHolder.player?.let { playerState = it }
    }

    override fun onDestroy() {
        // No liberar el player aquí: el servicio lo mantiene para reproducción en segundo plano.
        // Solo se libera cuando el servicio se destruye (usuario detiene o no hay media activa).
        if (isFinishing && !PlayerHolder.isServiceRunning) {
            try {
                playerState?.release()
            } catch (_: Throwable) {
            }
            PlayerHolder.detach()
        }
        super.onDestroy()
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(this, PlaybackService::class.java)
            ContextCompat.startForegroundService(this, intent)
        } catch (_: Throwable) {
            try {
                startService(Intent(this, PlaybackService::class.java))
            } catch (_: Throwable) {
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
