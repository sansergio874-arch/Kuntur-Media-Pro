package com.kuntur.mediapro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.exoplayer.ExoPlayer
import com.kuntur.mediapro.player.StreamPlayerFactory
import com.kuntur.mediapro.ui.theme.KunturMediaProTheme

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
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0A1628)) {
                    val p = player
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            player?.release()
        } catch (_: Throwable) {
        }
        player = null
    }
}
