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
    const val CURRENT_VERSION = "1.3"

    private const val PREFS = "kuntur_update"

    data class UpdateInfo(val version: String, val downloadUrl: String)

    fun wasDismissed(context: Context, version: String): Boolean {
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("dismissed_version", "") ?: ""
        return last == version
    }

    fun dismiss(context: Context, version: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("dismissed_version", version)
            .apply()
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val c = current.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val max = maxOf(r.size, c.size)
        for (i in 0 until max) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val json = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest").readText()
            if (!json.contains("tag_name")) return@withContext null
            val version = json.substringAfter("\"tag_name\":\"").substringBefore("\"").removePrefix("v")
            if (!isNewer(version, CURRENT_VERSION)) return@withContext null
            if (wasDismissed(context, version)) return@withContext null
            val apkUrl = Regex("\"browser_download_url\":\"([^\"]+\\.apk)\"")
                .find(json)?.groupValues?.get(1)
                ?: return@withContext null
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
