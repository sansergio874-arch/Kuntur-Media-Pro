package com.kuntur.mediapro.auth

import android.content.Context
import com.kuntur.mediapro.billing.PurchaseManager

object AuthManager {
    private const val PREFS = "kuntur_auth"

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("logged_in", false)

    fun isOwner(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("is_owner", false)

    fun getEmail(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("email", "") ?: ""

    fun getDisplayName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("name", "Usuario") ?: "Usuario"

    fun login(context: Context, email: String, name: String) {
        val owner = email.equals(GoogleAuthConfig.OWNER_EMAIL, ignoreCase = true) ||
            email.contains("santacruztapullima", ignoreCase = true)

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("logged_in", true)
            .putBoolean("is_owner", owner)
            .putString("email", email)
            .putString("name", if (owner) "$name (Dueño)" else name)
            .apply()

        if (owner) {
            PurchaseManager.requestPlan(context, "premium")
            PurchaseManager.approve(context)
        }
    }

    fun loginAsOwner(context: Context) {
        login(context, GoogleAuthConfig.OWNER_EMAIL, "Sérgio Santa Cruz")
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
