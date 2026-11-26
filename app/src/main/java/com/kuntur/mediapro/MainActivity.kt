package com.kuntur.mediapro

import android.Manifest
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
import com.kuntur.mediapro.player.PlayerHolder
import com.kuntur.mediapro.player.StreamPlayerFactory
import com.kuntur.mediapro.ui.theme.KunturMediaProTheme

class MainActivity : ComponentActivity() {

    private var playerState by mutableStateOf<ExoPlayer?>(null)
    private var bootError by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Thread.setDefaultUncaughtExceptionHandler { _, e ->
                try {
                    android.util.Log.e("KunturCrash", e.message ?: "crash", e)
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}

        try {
            requestNotificationPermissionIfNeeded()
        } catch (_: Throwable) {}

        try {
            val existing = try { PlayerHolder.player } catch (_: Throwable) { null }
            playerState = existing ?: try {
                StreamPlayerFactory.create(applicationContext).also {
                    try { PlayerHolder.attach(it) } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {
                try {
                    ExoPlayer.Builder(applicationContext).build().also {
                        try { PlayerHolder.attach(it) } catch (_: Throwable) {}
                    }
                } catch (e: Throwable) {
                    bootError = e.message?.take(80)
                    null
                }
            }
        } catch (e: Throwable) {
            bootError = e.message?.take(80)
            playerState = null
        }

        setContent {
            KunturMediaProTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A1628)) {
                    val p = playerState
                    when {
                        p != null -> AppRoot(p)
                        bootError != null -> ErrorScreen("Error al iniciar: $bootError")
                        else -> ErrorScreen("Error al iniciar el reproductor")
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            PlayerHolder.player?.let { playerState = it }
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
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

@androidx.compose.runtime.Composable
private fun ErrorScreen(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.White)
    }
}
