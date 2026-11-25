package com.kuntur.mediapro.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.kuntur.mediapro.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val GITHUB_REPO = "sansergio874-arch/Kuntur-Media-Pro"
    private const val PREFS = "kuntur_update"

    data class UpdateInfo(val version: String, val downloadUrl: String)

    fun wasDismissed(context: Context, version: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("dismissed_version", "") == version

    fun dismiss(context: Context, version: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("dismissed_version", version)
            .apply()
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (remotePart != currentPart) return remotePart > currentPart
        }
        return false
    }

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Kuntur-Media-Pro-Android")
            }
            if (connection.responseCode !in 200..299) return@withContext null

            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val version = release.optString("tag_name").removePrefix("v")
            if (version.isBlank() || !isNewer(version, BuildConfig.VERSION_NAME)) return@withContext null
            if (wasDismissed(context, version)) return@withContext null

            val assets = release.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://")) {
                    apkUrl = url
                    if (name == "app-release.apk") break
                }
            }
            apkUrl?.let { UpdateInfo(version, it) }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun downloadAndInstall(context: Context, downloadUrl: String) = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        val tempFile = File(context.cacheDir, "update.apk.part")
        try {
            require(
                downloadUrl.startsWith("https://github.com/") ||
                    downloadUrl.startsWith("https://objects.githubusercontent.com/")
            ) { "URL de actualización no permitida" }

            connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Kuntur-Media-Pro-Android")
            }
            if (connection.responseCode !in 200..299) error("No se pudo descargar la actualización")
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            require(tempFile.length() > 0L) { "El archivo APK está vacío" }

            val apkFile = File(context.cacheDir, "update.apk")
            if (apkFile.exists()) apkFile.delete()
            check(tempFile.renameTo(apkFile)) { "No se pudo preparar el APK" }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }
}
