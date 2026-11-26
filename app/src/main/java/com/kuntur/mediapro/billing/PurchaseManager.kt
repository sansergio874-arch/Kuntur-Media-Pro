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
            plan == "free" || plan.isBlank() -> "Sin plan activo"
            approved -> "Activo: ${planDisplayName(plan)}"
            else -> "Pendiente de pago / activación · código: ${getTransactionCode(context)}"
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

    /**
     * Activa el plan con el PIN de administrador (solo usted).
     * Ústil tras recibir el Yape del cliente, en el dispositivo del cliente.
     */
    fun activateWithAdminPin(
        context: Context,
        pin: String,
        planId: String? = null,
        transactionCode: String? = null
    ): Boolean {
        if (pin.trim() != BillingConfig.ADMIN_PIN) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val finalPlan = planId?.takeIf { it.isNotBlank() }
            ?: prefs.getString("plan", null)
            ?: return false
        if (finalPlan == "free") return false
        val code = transactionCode?.takeIf { it.isNotBlank() }
            ?: prefs.getString("tx_code", "")
            ?: "ADMIN"
        prefs.edit()
            .putString("plan", finalPlan)
            .putBoolean("approved", true)
            .putString("tx_code", code)
            .apply()
        return true
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
     * Reglas de acceso:
     * - Contenido no premium: siempre accesible
     * - Premium + plan aprobado:
     *   - basico: radios
     *   - estandar: radios + TV
     *   - premium: todo
     */
    fun canAccess(context: Context, item: MediaItem): Boolean {
        if (!item.isPremium) return true

        val plan = getCurrentPlan(context)
        val approved = isApproved(context)
        if (!approved) return false

        val cat = item.category.lowercase()
        return when (plan) {
            "premium" -> true
            "estandar" -> cat == "radio" || cat == "tv" || cat.contains("canal")
            "basico" -> cat == "radio"
            else -> false
        }
    }
}
