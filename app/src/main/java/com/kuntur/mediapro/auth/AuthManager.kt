package com.kuntur.mediapro.auth

import android.content.Context
import com.kuntur.mediapro.billing.PurchaseManager

object AuthManager {
    private const val PREFS = "kuntur_auth"

    /** Correos o identificadores del dueño (acceso total) */
    private val OWNER_MARKERS = listOf(
        "owner",
        "dueño",
        "admin",
        "sansergio",
        "sergio"
    )

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("logged_in", false)

    fun isOwner(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("is_owner", false)

    fun getEmail(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("email", "") ?: ""

    fun getDisplayName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("name", "Usuario") ?: "Usuario"

    fun login(context: Context, email: String, name: String, asOwner: Boolean = false) {
        val owner = asOwner || OWNER_MARKERS.any {
            email.contains(it, ignoreCase = true) || name.contains(it, ignoreCase = true)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("logged_in", true)
            .putBoolean("is_owner", owner)
            .putString("email", email)
            .putString("name", if (owner) "$name (Dueño)" else name)
            .apply()

        if (owner) {
            // Acceso total a todos los planes
            PurchaseManager.requestPlan(context, "premium")
            PurchaseManager.approve(context)
        }
    }

    fun loginAsOwner(context: Context) {
        login(context, "owner@kuntur.pro", "Dueño", asOwner = true)
    }

    fun loginWithGoogleDemo(context: Context, asOwner: Boolean) {
        if (asOwner) {
            loginAsOwner(context)
        } else {
            login(context, "usuario@gmail.com", "Usuario Google", asOwner = false)
        }
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
