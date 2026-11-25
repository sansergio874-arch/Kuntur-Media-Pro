package com.kuntur.mediapro.billing

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.util.UUID

object DeviceId {
    private const val PREFS = "kuntur_device"
    private const val KEY = "device_id"

    @SuppressLint("HardwareIds")
    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY, null)
        if (!saved.isNullOrBlank()) return saved

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Throwable) {
            null
        }
        val id = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            "dev_$androidId"
        } else {
            "dev_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        prefs.edit().putString(KEY, id).apply()
        return id
    }
}
