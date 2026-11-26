package com.kuntur.mediapro.billing

import com.kuntur.mediapro.BuildConfig

/**
 * Pagos Yape. Panel de administración solo en dispositivos del propietario.
 */
object BillingConfig {
    const val YAPE_NUMBER = "955238172"

    /** Uso interno al activar planes (no se pide en pantalla). */
    const val ADMIN_PIN = "874955"

    /**
     * IDs de sus dispositivos (formato dev_XXXXXXXX).
     * Solo en estos equipos se muestra el panel de administración,
     * sin PIN ni gestos. Cualquier otra instalación no lo ve.
     *
     * Cómo obtener su ID: Configuraciones → "ID de este dispositivo".
     */
    val OWNER_DEVICE_IDS: Set<String> = setOf(
        // Se completará con su ID real
    )

    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = supabaseUrl.startsWith("https://") && supabaseAnonKey.length > 20

    const val TABLE = "kuntur_subscriptions"
}
