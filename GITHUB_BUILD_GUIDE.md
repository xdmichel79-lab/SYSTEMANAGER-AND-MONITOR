# Guía rápida — GitHub Actions

1. En GitHub crea un repositorio llamado `SYSTEM-MANAGER-AND-MONITOR`.
2. Descomprime este paquete y carga los archivos y carpetas que están dentro de `SYSTEM-MANAGER-MONITOR` directamente en la raíz del repositorio.
3. Confirma el commit en la rama `main`.
4. Entra en la pestaña **Actions** y abre **Android CI**.
5. Ejecuta **Run workflow** si no comenzó automáticamente.
6. Revisa el resultado del job `Test and build debug APK`.
7. Si termina en verde, descarga `system-manager-monitor-debug-apk` en **Artifacts**.
8. Extrae el artefacto y encontrarás `app-debug.apk`.

Si falla, abre el paso rojo y comparte el log completo del primer error de Gradle/Kotlin. No compartas secretos ni tokens.

Esta es una APK Debug firmada con la clave de depuración de Gradle; sirve para pruebas. Para distribución pública hay que configurar firma release con secretos de GitHub y una keystore propia.
