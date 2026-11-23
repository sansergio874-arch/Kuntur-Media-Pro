package com.kuntur.mediapro.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object UpdateManager {

    private const val GITHUB_REPO = "sansergio874-arch/Kuntur-Media-Pro"
    const val CURRENT_VERSION = "1.0"

    data class UpdateInfo(val version: String, val downloadUrl: String)

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val json = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest").readText()
            if (!json.contains("tag_name")) return@withContext null
            val version = json.substringAfter("\"tag_name\":\"").substringBefore("\"").removePrefix("v")
            if (version <= CURRENT_VERSION) return@withContext null
            val apkUrl = json.substringAfter("\"browser_download_url\":\"").substringBefore("\"")
            if (apkUrl.isBlank()) return@withContext null
            UpdateInfo(version, apkUrl)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadAndInstall(context: Context, downloadUrl: String) = withContext(Dispatchers.IO) {
        try {
            val apkFile = File(context.cacheDir, "update.apk")
            URL(downloadUrl).openStream().use { input ->
                apkFile.outputStream().use { output -> input.copyTo(output) }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
