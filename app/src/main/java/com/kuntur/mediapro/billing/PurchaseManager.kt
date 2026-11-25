package com.kuntur.mediapro.billing

import android.content.Context
import com.kuntur.mediapro.data.MediaItem

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

    fun getTransactionCode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("tx_code", "") ?: ""

    fun getStatusLabel(context: Context): String {
        val approved = isApproved(context)
        val plan = getCurrentPlan(context)
        return when {
            plan == "free" -> "Sin plan"
            approved -> "Activo: ${planDisplayName(plan)}"
            else -> "Pendiente de pago / activación"
        }
    }

    fun planDisplayName(planId: String): String =
        plans.find { it.id == planId }?.name ?: planId

    fun requestPlan(context: Context, planId: String, transactionCode: String = "") {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("plan", planId)
            .putBoolean("approved", false)
            .putString("tx_code", transactionCode)
            .apply()
    }

    fun applyRemoteApproved(context: Context, planId: String, transactionCode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("plan", planId)
            .putBoolean("approved", true)
            .putString("tx_code", transactionCode)
            .apply()
    }

    fun markPending(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("approved", false)
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * Reglas de acceso por plan:
     * - free: solo contenido no premium (radios/TV gratuitos actuales)
     * - basico aprobado: radios
     * - estandar aprobado: radios + TV
     * - premium aprobado: todo
     */
    fun canAccess(context: Context, item: MediaItem): Boolean {
        val plan = getCurrentPlan(context)
        val approved = isApproved(context)
        val cat = item.category.lowercase()

        if (!item.isPremium) {
            // Contenido gratuito siempre accesible
            if (cat == "radio" || cat == "tv") return true
            // Películas no premium solo con plan o si no requieren pago
            if (!approved) return cat != "película" || !item.isPremium
            return true
        }

        // Contenido marcado premium
        if (!approved) return false
        return when (plan) {
            "premium" -> true
            "estandar" -> cat == "radio" || cat == "tv"
            "basico" -> cat == "radio"
            else -> false
        }
    }
}
