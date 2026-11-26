package com.kuntur.mediapro.billing

import com.kuntur.mediapro.BuildConfig

/**
 * Configuración de pagos (Yape + opcional Supabase).
 *
 * PIN de administrador: solo usted lo usa para activar planes en el dispositivo
 * del cliente tras recibir el Yape (o en su propio dispositivo de pruebas).
 */
object BillingConfig {
    /** Número Yape mostrado al usuario */
    const val YAPE_NUMBER = "955238172"

    /**
     * PIN secreto del administrador (usted).
     * El cliente paga por Yape e indica el código KNT-XXXX.
     * Usted introduce este PIN en la app del cliente (o en la suya) para activar.
     * Cámbielo si lo desea antes de distribuir el APK.
     */
    const val ADMIN_PIN = "874955"

    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = supabaseUrl.startsWith("https://") && supabaseAnonKey.length > 20

    const val TABLE = "kuntur_subscriptions"
}
