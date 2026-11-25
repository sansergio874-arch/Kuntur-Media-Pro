package com.kuntur.mediapro.auth

import android.content.Context

/**
 * Stores the signed-in profile only.
 * Owner privileges and paid access must be granted by a trusted backend after
 * validating identity and payment. An email saved on this device is not proof.
 */
object AuthManager {
    private const val PREFS = "kuntur_auth"

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("logged_in", false)

    // Local preferences are never accepted as proof of owner privileges.
    fun isOwner(context: Context): Boolean = false

    fun getEmail(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("email", "") ?: ""

    fun getDisplayName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("name", "Usuario") ?: "Usuario"

    fun login(context: Context, email: String, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("logged_in", true)
            .putBoolean("is_owner", false)
            .putString("email", email.trim())
            .putString("name", name.trim().ifBlank { "Usuario" })
            .apply()
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
