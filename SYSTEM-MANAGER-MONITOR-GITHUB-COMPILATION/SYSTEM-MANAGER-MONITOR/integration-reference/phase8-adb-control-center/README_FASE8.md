# SYSTEM MANAGER & MONITOR — Fase 8: ADB CONTROL CENTER

Este paquete es una **integración modular** para añadir el centro ADB al proyecto existente.
No es el proyecto completo ni una APK. El ZIP de la Fase 7 no estaba disponible en el entorno
de trabajo, por lo que no se han sobrescrito ni inventado archivos del proyecto original.

## Qué incluye

- Pantalla Compose `AdbControlCenterScreen`.
- Estado y comprobación de disponibilidad/autorización de Shizuku.
- Acceso a las pantallas oficiales de Opciones de desarrollador y depuración.
- Catálogo de comandos ADB de solo lectura para ejecutar desde Android SDK Platform Tools
  en una computadora conectada o emparejada.
- Diagnóstico básico local usando APIs Android.
- Dependencias y fragmento de Manifest de Shizuku.

## Importante: límites de cada modo

1. **USB ADB:** el teléfono actúa como dispositivo ADB; la computadora es el cliente/host.
   La APK instalada en el teléfono no puede controlar el daemon ADB USB del propio teléfono.
2. **Wireless ADB:** Android 11+ permite depurar el teléfono desde una estación de trabajo
   emparejada. El emparejamiento se realiza desde Ajustes > Opciones de desarrollador >
   Depuración inalámbrica. Esta APK no suplanta `adb pair` ni obtiene las claves privadas.
3. **Shizuku:** la app puede detectar el servicio y solicitar autorización. El usuario instala
   Shizuku y lo inicia por ADB o depuración inalámbrica. En dispositivos no rooteados puede
   ser necesario reiniciarlo después de cada arranque.
4. **Terminal:** esta entrega NO ejecuta comandos shell arbitrarios dentro de la app. El catálogo
   muestra comandos de diagnóstico de solo lectura para la consola ADB del ordenador. La API
   Shizuku actual recomienda `UserService` para ejecutar código con identidad shell/root;
   no se usa el antiguo `newProcess` ni se presenta el UID de la app como shell.
5. **Acciones destructivas:** no se incluyen `pm clear`, `pm disable-user`, `settings put`,
   `setprop`, `reboot`, borrado masivo ni revocación silenciosa de permisos.

## Integración

### 1. Dependencia Gradle

En `app/build.gradle.kts`, dentro de `dependencies`:

```kotlin
implementation("dev.rikka.shizuku:api:13.1.5")
implementation("dev.rikka.shizuku:provider:13.1.5")
```

La versión 13.1.5 corresponde al changelog visible en el repositorio oficial de Shizuku API
consultado al preparar este módulo. Comprueba Maven Central al sincronizar Gradle por si existe
una versión posterior.

### 2. Manifest

Añade el provider dentro de `<application>` de `app/src/main/AndroidManifest.xml`:

```xml
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:multiprocess="false"
    android:enabled="true"
    android:exported="true"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
```

No declares `INTERACT_ACROSS_USERS_FULL` como permiso propio; se usa como protección del
provider. No añadas permisos root, `WRITE_SECURE_SETTINGS` ni permisos de sistema ficticios.

### 3. Copiar Kotlin

Copia `src/main/java/com/systemmanager/monitor/adb/` al mismo paquete de tu proyecto.
Integra `AdbControlCenterScreen` en la navegación principal, por ejemplo en la ruta `adb`.

### 4. Requisitos

- minSdk recomendado: 26 (coincide con la base del proyecto).
- Android 11/API 30 o superior para depuración inalámbrica estándar.
- Shizuku instalado para la integración avanzada; el usuario debe iniciarlo y conceder permiso.
- Para USB ADB se requiere Android SDK Platform Tools en la computadora y aceptar la huella RSA.

## Próxima implementación necesaria para una consola Shizuku real

Crear un `UserService` AIDL versionado y vincularlo con `Shizuku.bindUserService`, aplicar una
lista permitida de operaciones y validar cada argumento. No ejecutar texto arbitrario recibido
desde UI. Esta entrega deja la detección/permiso listos, pero no declara falsamente que el
terminal privilegiado ya funciona.
