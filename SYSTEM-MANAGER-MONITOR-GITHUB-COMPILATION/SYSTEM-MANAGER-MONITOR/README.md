# SYSTEM MANAGER & MONITOR — GitHub Build Package

Android nativo con Kotlin, Jetpack Compose y Material 3. Esta entrega consolida la base del dashboard, diagnóstico, administración de aplicaciones, detección de receptores de arranque, centro ADB informativo y terminal local de comandos permitidos.

## Compilar en GitHub Actions

1. Crea un repositorio GitHub vacío.
2. Sube **todo el contenido de esta carpeta** a la raíz del repositorio (no subas la carpeta ZIP anidada).
3. Abre **Actions → Android CI → Run workflow** o haz `git push` a `main`.
4. Al terminar, descarga el artefacto `system-manager-monitor-debug-apk`.
5. Dentro encontrarás `app-debug.apk`.

El workflow usa JDK 17, Gradle 8.13, Android SDK 36 y ejecuta `testDebugUnitTest` y `assembleDebug`.

## Módulos incluidos

- Dashboard adaptable y monitor de hardware (CPU/RAM/batería/almacenamiento/sensores accesibles).
- Historial de métricas y exportación de informe.
- Administrador de aplicaciones mediante pantallas oficiales Android.
- Panel de detección de aplicaciones que declaran inicio BOOT_COMPLETED y accesos a ajustes del fabricante.
- ADB Control Center: acceso a opciones de desarrollador, guía de USB/Wireless ADB y catálogo de comandos de solo lectura para copiar a Platform Tools.
- Terminal local: ejecuta un conjunto fijo de comandos de consulta dentro del UID de la app; no acepta shell arbitrario.
- Launcher HOME seleccionable por el usuario.

## Límites reales de esta entrega

- No se garantiza que compile hasta que GitHub Actions termine correctamente; el entorno de preparación no dispone de Gradle/Android SDK para ejecutar el build aquí.
- La terminal local no es una shell ADB ni root. Está limitada al UID de la aplicación y a comandos incluidos en una allowlist.
- El panel ADB prepara el flujo y catálogo; no implementa todavía un cliente ADB host dentro del teléfono ni un Shizuku UserService para ejecutar comandos privilegiados. ADB USB/Wireless se opera normalmente desde una computadora emparejada.
- No se bloquea automáticamente el arranque de otras apps ni se revocan sus permisos sin APIs/roles privilegiados.
- No se borran silenciosamente datos de otras aplicaciones.

## Versiones

- applicationId: `com.systemmanager.monitor`
- versionName: `0.9.0`
- versionCode: `9`
- minSdk: 26
- targetSdk: 35
- compileSdk: 36

## Estructura

- `app/src/main/java/com/systemmanager/monitor/` código Kotlin.
- `app/src/main/AndroidManifest.xml` manifest.
- `app/src/test/` pruebas unitarias.
- `.github/workflows/android-build.yml` compilación automática.
