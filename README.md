# Kuntur Media Pro

Aplicación Android nativa basada en Kotlin, Jetpack Compose y AndroidX Media3/ExoPlayer.

## Funciones

- Secciones de radio, TV y películas.
- Reproductor HLS/DASH/MP4 y soporte Widevine cuando se configura un servidor de licencias válido.
- Video en modo FIT para mostrar el cuadro completo sin recortar.
- Zoom con pellizco de dos dedos (hasta 3x); el encuadre inicial conserva el video completo.
- Indicador de carga hasta que ExoPlayer comienza a reproducir.
- Sin reintentos automáticos de red: los errores de la fuente se muestran al usuario.
- Catálogo con identificadores estables y logos de respaldo.
- Actualizaciones consultadas desde GitHub Releases con comparación contra la versión instalada.
- GitHub Actions ejecuta pruebas unitarias y genera el APK.

## Compilar

En GitHub: **Actions → Build and Release APK → Run workflow**.
El workflow ejecuta `gradle testDebugUnitTest assembleDebug`, sube el APK como artefacto y crea una Release cuando compila la rama `main`.

## Catálogo y derechos

Los enlaces de `MediaRepository` son fuentes de ejemplo y deben comprobarse periódicamente. No se deben presentar streams de prueba como canales oficiales. Sustituya o amplíe las fuentes únicamente con URLs públicas, estables y autorizadas por sus titulares. La disponibilidad y los cortes dependen también del servidor que entrega el stream y de la conexión del dispositivo.

## Pagos y autenticación: limitaciones importantes

La selección de planes y el historial actual se guardan localmente. **Esto no verifica pagos ni protege contenido premium frente a manipulación del APK o del almacenamiento local.** Antes de cobrar o distribuir planes, implemente un backend HTTPS que:
1. Verifique tokens de inicio de sesión en el servidor.
2. Verifique comprobantes/operaciones de pago con un proceso administrativo seguro.
3. Emita permisos de acceso firmados y con caducidad.
4. Nunca confíe en un correo, una preferencia local o una llamada de la app para conceder rol de propietario.

Actualmente no hay backend de autenticación, panel administrativo ni integración automática de verificación Yape. El número de pago mostrado en pantalla no constituye confirmación de una transacción.

## DRM

Widevine requiere contenido cifrado y un License Server real. La función de construcción de MediaItem DRM no crea licencias ni desbloquea streams por sí sola.

## Requisitos técnicos

- JDK 17
- Android Gradle Plugin 8.2.2
- Gradle 8.4
- Kotlin 1.9.22
- Android SDK 34
- AndroidX Media3 1.2.1
