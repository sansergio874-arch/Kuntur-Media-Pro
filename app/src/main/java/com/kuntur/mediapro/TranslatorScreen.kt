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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
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

data class SourceLang(val code: String, val label: String)

private val SOURCE_LANGUAGES = listOf(
    SourceLang(TranslateLanguage.ENGLISH, "Inglés"),
    SourceLang(TranslateLanguage.PORTUGUESE, "Portugués"),
    SourceLang(TranslateLanguage.FRENCH, "Francés"),
    SourceLang(TranslateLanguage.GERMAN, "Alemán"),
    SourceLang(TranslateLanguage.ITALIAN, "Italiano"),
    SourceLang(TranslateLanguage.CHINESE, "Chino"),
    SourceLang(TranslateLanguage.JAPANESE, "Japonés"),
    SourceLang(TranslateLanguage.KOREAN, "Coreano"),
    SourceLang(TranslateLanguage.RUSSIAN, "Ruso"),
    SourceLang(TranslateLanguage.ARABIC, "Árabe")
)

@Composable
fun TranslatorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Seleccione idioma y pulse el micrófono.") }
    var isListening by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    var selectedLang by remember { mutableStateOf(SOURCE_LANGUAGES[0]) }
    var modelsReady by remember { mutableStateOf(false) }

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
                status = "Traduciendo ${selectedLang.label} → Español…"
                isTranslating = true
                translateToSpanish(text, selectedLang.code) { translated, error ->
                    isTranslating = false
                    if (error != null) {
                        status = "Error: $error"
                    } else {
                        translatedText = translated.orEmpty()
                        status = "Traducción lista (offline)."
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
        if (granted) startListening(speechLauncher, selectedLang.code) { isListening = true; status = "Escuchando…" }
        else status = "Se requiere permiso de micrófono."
    }

    fun downloadModels() {
        isDownloading = true
        status = "Descargando modelos para uso offline…"
        var pending = SOURCE_LANGUAGES.size
        var failed = 0
        SOURCE_LANGUAGES.forEach { lang ->
            downloadModelFor(lang.code) { ok ->
                if (!ok) failed++
                pending--
                if (pending <= 0) {
                    isDownloading = false
                    modelsReady = failed == 0
                    status = if (failed == 0) "Modelos listos. Funciona sin internet." else "Algunos modelos no se descargaron ($failed)."
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyT, NavyMidT)))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Volver", color = GoldT) }
            Spacer(Modifier.weight(1f))
            Text("Traductor Offline", color = GoldT, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Traduce a español sin internet (tras descargar modelos)",
            color = Color.White.copy(0.7f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        Text("Idioma de origen", color = GoldT, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SOURCE_LANGUAGES) { lang ->
                FilterChip(
                    selected = selectedLang.code == lang.code,
                    onClick = { selectedLang = lang },
                    label = { Text(lang.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldT,
                        selectedLabelColor = NavyT,
                        containerColor = CardBgT,
                        labelColor = Color.White
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { downloadModels() },
            enabled = !isDownloading,
            colors = ButtonDefaults.buttonColors(containerColor = CardBgT),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CloudDownload, null, tint = GoldT, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isDownloading) "Descargando…" else "Descargar modelos offline", color = GoldT)
        }
        if (modelsReady) {
            Text("✓ Modelos descargados", color = Color(0xFF81C784), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBgT), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Texto reconocido", color = GoldT, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(recognizedText.ifBlank { "—" }, color = Color.White, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBgT), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Traducción al español", color = GoldT, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(translatedText.ifBlank { "—" }, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilledIconButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (granted) startListening(speechLauncher, selectedLang.code) { isListening = true; status = "Escuchando…" }
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
                    } else status = "No hay traducción para reproducir."
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CardBgT),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Escuchar", tint = GoldT, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        if (isTranslating || isDownloading) {
            CircularProgressIndicator(color = GoldT, strokeWidth = 3.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
        }
        Text(status, color = Color.White.copy(0.75f), fontSize = 13.sp)

        Spacer(Modifier.weight(1f))
        Text(
            "Una vez descargados los modelos, la traducción funciona sin conexión.",
            color = Color.White.copy(0.4f),
            fontSize = 11.sp
        )
    }
}

private fun startListening(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    sourceLangCode: String,
    onStart: () -> Unit
) {
    onStart()
    val locale = when (sourceLangCode) {
        TranslateLanguage.ENGLISH -> "en-US"
        TranslateLanguage.PORTUGUESE -> "pt-BR"
        TranslateLanguage.FRENCH -> "fr-FR"
        TranslateLanguage.GERMAN -> "de-DE"
        TranslateLanguage.ITALIAN -> "it-IT"
        TranslateLanguage.CHINESE -> "zh-CN"
        TranslateLanguage.JAPANESE -> "ja-JP"
        TranslateLanguage.KOREAN -> "ko-KR"
        TranslateLanguage.RUSSIAN -> "ru-RU"
        TranslateLanguage.ARABIC -> "ar-SA"
        else -> "en-US"
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    launcher.launch(intent)
}

private fun translateToSpanish(text: String, sourceCode: String, onResult: (String?, String?) -> Unit) {
    try {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceCode)
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
            .addOnFailureListener { e -> onResult(null, e.message ?: "Modelo no disponible. Descargue primero.") }
    } catch (e: Throwable) {
        onResult(null, e.message ?: "Error")
    }
}

private fun downloadModelFor(sourceCode: String, onDone: (Boolean) -> Unit) {
    try {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceCode)
            .setTargetLanguage(TranslateLanguage.SPANISH)
            .build()
        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    } catch (_: Throwable) {
        onDone(false)
    }
}
