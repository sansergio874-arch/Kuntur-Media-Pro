package com.kuntur.mediapro.billing

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class RemoteSubscription(
    val id: String,
    val deviceId: String,
    val planId: String,
    val planName: String,
    val price: Int,
    val status: String,
    val transactionCode: String,
    val yapeRef: String,
    val createdAt: String,
    val approvedAt: String?,
    val expiresAt: String?
)

/**
 * Cliente REST mínimo hacia Supabase (sin SDK extra).
 */
object SubscriptionApi {

    fun createPending(
        deviceId: String,
        planId: String,
        planName: String,
        price: Int
    ): Result<RemoteSubscription> {
        if (!BillingConfig.isConfigured) {
            return Result.failure(IllegalStateException("Backend no configurado (SUPABASE_URL / ANON_KEY)"))
        }
        val code = generateCode()
        val body = JSONObject()
            .put("device_id", deviceId)
            .put("plan_id", planId)
            .put("plan_name", planName)
            .put("price", price)
            .put("status", "pending")
            .put("transaction_code", code)
            .put("yape_ref", "")

        return try {
            val response = request(
                method = "POST",
                path = "/rest/v1/${BillingConfig.TABLE}",
                body = body.toString(),
                extraHeaders = mapOf("Prefer" to "return=representation")
            )
            val arr = JSONArray(response)
            if (arr.length() == 0) {
                Result.failure(IllegalStateException("Sin respuesta del servidor"))
            } else {
                Result.success(parseRow(arr.getJSONObject(0)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun fetchForDevice(deviceId: String): Result<List<RemoteSubscription>> {
        if (!BillingConfig.isConfigured) {
            return Result.failure(IllegalStateException("Backend no configurado"))
        }
        return try {
            val encoded = java.net.URLEncoder.encode("eq.$deviceId", "UTF-8")
            val path =
                "/rest/v1/${BillingConfig.TABLE}?device_id=$encoded&order=created_at.desc&limit=20"
            val response = request(method = "GET", path = path)
            val arr = JSONArray(response)
            val list = mutableListOf<RemoteSubscription>()
            for (i in 0 until arr.length()) {
                list.add(parseRow(arr.getJSONObject(i)))
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Devuelve la suscripción aprobada vigente más reciente, si existe. */
    fun latestApproved(deviceId: String): Result<RemoteSubscription?> {
        return fetchForDevice(deviceId).map { list ->
            list.firstOrNull { sub ->
                sub.status == "approved" && !isExpired(sub.expiresAt)
            }
        }
    }

    private fun isExpired(expiresAt: String?): Boolean {
        if (expiresAt.isNullOrBlank()) return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val normalized = expiresAt.replace("Z", "").substringBefore(".")
            val exp = sdf.parse(normalized) ?: return false
            exp.before(Date())
        } catch (_: Exception) {
            false
        }
    }

    private fun generateCode(): String {
        val part = UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)
        return "KNT-$part"
    }

    private fun parseRow(o: JSONObject): RemoteSubscription {
        return RemoteSubscription(
            id = o.optString("id"),
            deviceId = o.optString("device_id"),
            planId = o.optString("plan_id"),
            planName = o.optString("plan_name"),
            price = o.optInt("price"),
            status = o.optString("status"),
            transactionCode = o.optString("transaction_code"),
            yapeRef = o.optString("yape_ref"),
            createdAt = o.optString("created_at"),
            approvedAt = o.optString("approved_at").ifBlank { null },
            expiresAt = o.optString("expires_at").ifBlank { null }
        )
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        val url = URL(BillingConfig.supabaseUrl + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 25_000
            setRequestProperty("apikey", BillingConfig.supabaseAnonKey)
            setRequestProperty("Authorization", "Bearer ${BillingConfig.supabaseAnonKey}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
            doInput = true
            if (body != null) {
                doOutput = true
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code: ${text.take(200)}")
            }
            return text.ifBlank { "[]" }
        } finally {
            conn.disconnect()
        }
    }
}
