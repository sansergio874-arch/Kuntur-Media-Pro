package com.kuntur.mediapro.billing

import android.content.Context

data class Plan(val id: String, val name: String, val price: Int, val description: String)

object PurchaseManager {
    val plans = listOf(
        Plan("basico", "Básico", 10, "Acceso a todas las radios"),
        Plan("estandar", "Estándar", 25, "Radios + Canales de TV"),
        Plan("premium", "Premium", 45, "Todo el contenido sin restricciones")
    )

    private const val PREFS = "kuntur_prefs"

    fun getCurrentPlan(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("plan", "free") ?: "free"

    fun isApproved(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("approved", false)

    fun requestPlan(context: Context, planId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("plan", planId)
            .putBoolean("approved", false)
            .apply()
    }

    fun approve(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("approved", true)
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
