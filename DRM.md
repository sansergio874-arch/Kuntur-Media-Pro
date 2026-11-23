# Protección DRM Widevine - Kuntur Media Pro

## Descripción

La aplicación incluye soporte para contenido protegido con **Widevine DRM** mediante ExoPlayer (Media3).

## Archivo principal

`app/src/main/java/com/kuntur/mediapro/player/DrmHelper.kt`

## Uso

```kotlin
// Contenido protegido
val mediaItem = DrmHelper.createProtectedMediaItem(
    videoUrl = "https://su-cdn.com/pelicula/manifest.mpd",
    licenseUrl = "https://su-license-server.com/widevine",
    authToken = "token_del_usuario" // opcional
)
player.setMediaItem(mediaItem)

// Contenido libre (radios, demos)
val clearItem = DrmHelper.createClearMediaItem("https://stream.ejemplo.com/radio.mp3")
player.setMediaItem(clearItem)
```

## Requisitos

1. El video debe estar cifrado con Widevine (CENC / CMAF).
2. Debe disponer de un **License Server** (Cloudflare Stream, Mux, Axinom, etc.).
3. No funciona con URLs inventadas: el servidor de licencias debe ser real.

## Proveedores recomendados

| Proveedor | DRM incluido | Notas |
|-----------|--------------|-------|
| Cloudflare Stream | Sí (opcional) | Fácil de activar |
| Mux | Sí | Buena documentación |
| Axinom / BuyDRM | Sí | Especializados en DRM |
| Bunny Stream | Limitado | Verificar disponibilidad |

## Notas

- Widevine L3 (software) funciona en la mayoría de dispositivos Android.
- L1 (hardware) solo en dispositivos certificados.
- Las radios y contenido no premium pueden seguir usando streams sin DRM.
