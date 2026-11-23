package com.kuntur.mediapro.auth

/**
 * Client IDs de Google Cloud Console.
 * Para requestIdToken debe usarse el cliente tipo **Aplicación web**.
 */
object GoogleAuthConfig {
    /** Preferir el Client ID de tipo Web */
    const val WEB_CLIENT_ID =
        "891239728708-djumdpnesopi66ko6tshdeaeicbqfmb2.apps.googleusercontent.com"

    /** Cliente alternativo (Android u otro Web) */
    const val ALT_CLIENT_ID =
        "891239728708-98vtd61oibbpuke3lcgsnmqivma9vsda.apps.googleusercontent.com"

    /** Correo del dueño: acceso Premium automático */
    const val OWNER_EMAIL = "santacruztapullimas@gmail.com"
}
