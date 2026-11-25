# Kuntur Media Pro

Aplicación Android de entretenimiento construida con Kotlin, Jetpack Compose y AndroidX Media3/ExoPlayer.

## Secciones

- **TV en vivo e IPTV:** canales del catálogo y posibilidad de importar listas M3U/M3U8 desde una URL HTTPS.
- **Radios:** emisoras de audio en directo.
- **Películas:** catálogo de títulos con reproducción de vídeo.
- **Reproductor unificado:** HLS, DASH y MP4 cuando el formato y el servidor son compatibles.
- **Imagen completa:** modo FIT para no recortar el vídeo y zoom por pellizco de hasta 3×.
- **Carga y errores:** indicador hasta que comienza la reproducción; no hay reintentos automáticos.
- **Build:** GitHub Actions ejecuta pruebas unitarias y genera el APK.

## Limpieza de pagos

La aplicación ya no presenta planes, pantallas de compra, instrucciones de pago ni datos de Yape. Las funciones locales de compras e historial se retiraron del flujo de la app. El contenido del catálogo no queda bloqueado por una compra local.

## Importar IPTV

Entra en TV en vivo e IPTV, pulsa Agregar lista IPTV (M3U) y pega una URL HTTPS de una lista que tengas derecho a utilizar. Se admiten entradas M3U/M3U8 estándar con EXTINF y enlaces HTTP(S). La lista se procesa en el dispositivo, se limita a 5 MB y se evitan URLs duplicadas durante la sesión.

La aplicación no proporciona listas de canales de pago, credenciales ni enlaces de terceros. Debes utilizar fuentes propias o autorizadas. La disponibilidad, calidad y continuidad dependen del proveedor de cada stream.

## Compilar

En GitHub: Actions → Build and Release APK → Run workflow.

## DRM

Widevine requiere contenido cifrado y un servidor de licencias válido proporcionado por el titular del contenido. La aplicación no genera licencias ni desbloquea streams protegidos por sí sola.

## Requisitos técnicos

- JDK 17
- Android Gradle Plugin 8.2.2
- Gradle 8.4
- Kotlin 1.9.22
- Android SDK 34
- AndroidX Media3 1.2.1
