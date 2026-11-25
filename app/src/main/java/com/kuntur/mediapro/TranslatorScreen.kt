package com.kuntur.mediapro

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

private val NavyT = Color(0xFF0A1628)
private val NavyMidT = Color(0xFF132337)
private val GoldT = Color(0xFFE8B923)
private val CardBgT = Color(0xFF1A2D45)

@Composable
fun TranslatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Pulse el micrófono y hable en cualquier idioma.") }
    var isListening by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
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
                status = "Traduciendo a español…"
                isTranslating = true
                translateToSpanish(text) { translated, error ->
                    isTranslating = false
                    if (error != null) {
                        status = "Error: $error"
                    } else {
                        translatedText = translated.orEmpty()
                        status = "Traducción lista. Pulse el altavoz para escuchar."
                        if (ttsReady && translatedText.isNotBlank()) {
                            tts.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, "trad")
                        }
                    }
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
            "Traduce cualquier idioma a español en audio",
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
                    fontSize = 15.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBgT),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Traducción al español", color = GoldT, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    translatedText.ifBlank { "—" },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(20.dp))

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
                    if (ttsReady && translatedText.isNotBlank()) {
                        tts.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, "trad2")
                    } else {
                        status = "No hay traducción para reproducir."
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardBgT),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Escuchar", tint = GoldT, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        if (isTranslating) {
            CircularProgressIndicator(color = GoldT, strokeWidth = 3.dp)
            Spacer(Modifier.height(8.dp))
        }
        Text(status, color = Color.White.copy(0.75f), fontSize = 13.sp)

        Spacer(Modifier.weight(1f))
        Text(
            "La primera traducción puede descargar el modelo de idioma (requiere internet una vez).",
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
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "") // auto-detect
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    launcher.launch(intent)
}

private fun translateToSpanish(text: String, onResult: (String?, String?) -> Unit) {
    try {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH) // se puede ampliar; ML Kit requiere fuente conocida
            .setTargetLanguage(TranslateLanguage.SPANISH)
            .build()
        val translator: Translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translated -> onResult(translated, null) }
                    .addOnFailureListener { e -> onResult(null, e.message ?: "Error de traducción") }
            }
            .addOnFailureListener { e -> onResult(null, e.message ?: "No se pudo descargar el modelo") }
    } catch (e: Throwable) {
        onResult(null, e.message ?: "Error")
    }
}
