package com.kuntur.mediapro.billing

import com.kuntur.mediapro.BuildConfig

/**
 * Configuración del backend de pagos (Supabase).
 *
 * 1. Cree un proyecto en https://supabase.com
 * 2. Ejecute el SQL de supabase/subscriptions.sql
 * 3. Configure SUPABASE_URL y SUPABASE_ANON_KEY en GitHub Secrets
 *    o rellene los valores por defecto abajo para pruebas locales.
 */
object BillingConfig {
    /** Número Yape mostrado al usuario */
    const val YAPE_NUMBER = "955238172"

    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = supabaseUrl.startsWith("https://") && supabaseAnonKey.length > 20

    const val TABLE = "kuntur_subscriptions"
}
