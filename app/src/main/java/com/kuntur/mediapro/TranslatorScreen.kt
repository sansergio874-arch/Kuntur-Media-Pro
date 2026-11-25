package com.kuntur.mediapro

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

private val NavyT = Color(0xFF0A1628)
private val NavyMidT = Color(0xFF132337)
private val GoldT = Color(0xFFE8B923)
private val CardBgT = Color(0xFF1A2D45)

@Composable
fun TranslatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Pulse el micrófono y hable.") }
    var isListening by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        TextToSpeech(context) { statusCode ->
            ttsReady = statusCode == TextToSpeech.SUCCESS
        }.apply {
            language = Locale("es", "ES")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { tts.stop(); tts.shutdown() } catch (_: Throwable) {}
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull().orEmpty()
            recognizedText = text
            if (text.isNotBlank()) {
                status = "Texto reconocido. Pulse el altavoz para escucharlo."
                if (ttsReady) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak")
                }
            } else {
                status = "No se reconoció voz. Intente de nuevo."
            }
        } else {
            status = "Reconocimiento cancelado."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening(speechLauncher) { isListening = true; status = "Escuchando…" }
        else status = "Se requiere permiso de micrófono."
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyT, NavyMidT)))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Volver", color = GoldT) }
            Spacer(Modifier.weight(1f))
            Text("Traductor", color = GoldT, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Reconocimiento de voz y lectura en español",
            color = Color.White.copy(0.7f),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBgT),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Texto reconocido", color = GoldT, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    recognizedText.ifBlank { "—" },
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledIconButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        startListening(speechLauncher) { isListening = true; status = "Escuchando…" }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isListening) Color(0xFFC62828) else GoldT),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Hablar", tint = NavyT, modifier = Modifier.size(36.dp))
            }

            FilledIconButton(
                onClick = {
                    if (ttsReady && recognizedText.isNotBlank()) {
                        tts.speak(recognizedText, TextToSpeech.QUEUE_FLUSH, null, "speak2")
                    } else {
                        status = "No hay texto para reproducir."
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardBgT),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Escuchar", tint = GoldT, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(status, color = Color.White.copy(0.75f), fontSize = 13.sp)

        Spacer(Modifier.weight(1f))
        Text(
            "La traducción avanzada se añadirá en una próxima actualización.",
            color = Color.White.copy(0.4f),
            fontSize = 11.sp
        )
    }
}

private fun startListening(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onStart: () -> Unit
) {
    onStart()
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    launcher.launch(intent)
}
