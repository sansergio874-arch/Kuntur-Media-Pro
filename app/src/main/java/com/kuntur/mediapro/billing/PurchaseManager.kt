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
    private const val KEY_OWNER = "owner_device"

    fun getCurrentPlan(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("plan", "free") ?: "free"

    fun isApproved(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("approved", false)

    fun getTransactionCode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("tx_code", "") ?: ""

    fun isOwnerDevice(context: Context): Boolean {
        val id = DeviceId.get(context)
        if (BillingConfig.OWNER_DEVICE_IDS.any { it.isNotBlank() && (id == it || id.endsWith(it)) }) {
            return true
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_OWNER, false)
    }

    fun unlockOwnerMode(context: Context, pin: String): Boolean {
        if (pin.trim() != BillingConfig.ADMIN_PIN) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_OWNER, true)
            .apply()
        return true
    }

    fun lockOwnerMode(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_OWNER, false)
            .apply()
    }

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

    fun activatePendingAsOwner(context: Context): Boolean {
        if (!isOwnerDevice(context)) return false
        return activateWithAdminPin(context, BillingConfig.ADMIN_PIN)
    }

    fun markPending(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("approved", false)
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

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
