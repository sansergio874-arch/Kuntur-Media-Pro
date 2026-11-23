# Kuntur Media Pro

Aplicación nativa Android de entretenimiento **completa al 100%**.

## Características

- Pantalla principal profesional (Radio · TV · Películas)
- Reproductor nativo Media3 / ExoPlayer
- Soporte Widevine DRM para contenido premium
- Planes de suscripción con pago vía Yape **955238172**
- Monitoreo de transacciones (historial local)
- Actualización automática desde GitHub Releases (sin Play Store)
- Sin panel de administrador visible (aprobaciones privadas)
- Tema oscuro Material 3 con acentos dorados

## Generar APK

1. Vaya a **Actions** → **Build and Release APK**
2. Pulse **Run workflow**
3. Descargue el artefacto o el Release generado

## Planes

| Plan     | Precio | Descripción                   |
|----------|--------|-------------------------------|
| Básico   | S/ 10  | Acceso a radios               |
| Estándar | S/ 25  | Radios + Canales TV           |
| Premium  | S/ 45  | Todo el contenido sin límites |

Pago único vía Yape: **955238172**

## Estructura del proyecto

```
app/src/main/java/com/kuntur/mediapro/
├── MainActivity.kt
├── data/MediaItem.kt
├── billing/PurchaseManager.kt
├── billing/TransactionManager.kt
├── player/DrmHelper.kt
├── update/UpdateManager.kt
└── ui/theme/Theme.kt
```

## Tecnologías

- Kotlin + Jetpack Compose
- Media3 / ExoPlayer (+ DASH + DRM)
- Material 3
- GitHub Actions (CI/CD + Releases)

---

**Kuntur Media Pro** — Entretenimiento nativo profesional.
