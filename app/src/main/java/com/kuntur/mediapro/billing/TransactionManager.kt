package com.kuntur.mediapro.billing

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Transaction(
    val id: String,
    val planId: String,
    val planName: String,
    val price: Int,
    val status: String,
    val createdAt: String,
    val yapeRef: String = ""
)

object TransactionManager {

    private const val PREFS = "kuntur_transactions"

    fun createTransaction(context: Context, planId: String, planName: String, price: Int): Transaction {
        val tx = Transaction(
            id = UUID.randomUUID().toString().take(8),
            planId = planId,
            planName = planName,
            price = price,
            status = "pending",
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        )
        saveLocal(context, tx)
        return tx
    }

    fun getHistory(context: Context): List<Transaction> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString("history", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("||").mapNotNull { line ->
            val p = line.split("|")
            if (p.size >= 6) {
                Transaction(p[0], p[1], p[2], p[3].toIntOrNull() ?: 0, p[4], p[5], p.getOrElse(6) { "" })
            } else null
        }
    }

    private fun saveLocal(context: Context, tx: Transaction) {
        val list = getHistory(context).toMutableList()
        list.add(0, tx)
        val raw = list.joinToString("||") {
            "${it.id}|${it.planId}|${it.planName}|${it.price}|${it.status}|${it.createdAt}|${it.yapeRef}"
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("history", raw).apply()
    }
}
