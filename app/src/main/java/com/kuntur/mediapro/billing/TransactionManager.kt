package com.kuntur.mediapro.billing

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun createTransaction(
        context: Context,
        planId: String,
        planName: String,
        price: Int,
        id: String,
        status: String = "pending"
    ): Transaction {
        val tx = Transaction(
            id = id,
            planId = planId,
            planName = planName,
            price = price,
            status = status,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        )
        saveLocal(context, tx)
        return tx
    }

    fun updateStatus(context: Context, id: String, status: String) {
        val list = getHistory(context).map {
            if (it.id == id) it.copy(status = status) else it
        }
        persistAll(context, list)
    }

    fun remove(context: Context, id: String) {
        val list = getHistory(context).filter { it.id != id }
        persistAll(context, list)
    }

    fun clearHistory(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove("history").apply()
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
        list.removeAll { it.id == tx.id }
        list.add(0, tx)
        persistAll(context, list)
    }

    private fun persistAll(context: Context, list: List<Transaction>) {
        val raw = list.joinToString("||") {
            "${it.id}|${it.planId}|${it.planName}|${it.price}|${it.status}|${it.createdAt}|${it.yapeRef}"
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("history", raw).apply()
    }
}
