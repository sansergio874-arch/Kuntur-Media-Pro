package com.kuntur.mediapro.billing

import com.kuntur.mediapro.BuildConfig

/**
 * Pagos Yape + modo propietario (solo su dispositivo).
 */
object BillingConfig {
    const val YAPE_NUMBER = "955238172"

    /** PIN solo para usted. No se muestra en la interfaz de clientes. */
    const val ADMIN_PIN = "874955"

    /**
     * IDs de dispositivo del propietario (dev_XXXXXXXX).
     * Puede añadir el suyo aquí para que el panel aparezca sin desbloqueo.
     * Mientras esté vacío, se desbloquea una vez en Configuraciones con el PIN.
     */
    val OWNER_DEVICE_IDS: Set<String> = setOf(
        // Ejemplo: "dev_abc123..."
    )

    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = supabaseUrl.startsWith("https://") && supabaseAnonKey.length > 20

    const val TABLE = "kuntur_subscriptions"
}
