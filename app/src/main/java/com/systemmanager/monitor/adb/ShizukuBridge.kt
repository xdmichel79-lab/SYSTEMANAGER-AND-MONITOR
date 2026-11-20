package com.systemmanager.monitor.adb

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Puente de estado/autorización Shizuku.
 * No inicia Shizuku, no eleva privilegios y no ejecuta comandos por sí mismo.
 */
class ShizukuBridge {

    companion object {
        const val REQUEST_CODE = 8408
    }

    fun readStatus(): AdbStatus {
        val binder = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!binder) {
            return AdbStatus(
                binderAvailable = false,
                message = "Shizuku no está iniciado. Inícialo desde su aplicación."
            )
        }

        val permission = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

        val uid = runCatching { Shizuku.getUid() }.getOrNull()

        return AdbStatus(
            shizukuInstalled = true,
            binderAvailable = true,
            permissionGranted = permission,
            serverUid = uid,
            message = when {
                !permission -> "Servicio detectado; falta autorización para SYSTEM MANAGER & MONITOR."
                uid == 2000 -> "Autorizado con identidad shell (ADB)."
                uid == 0 -> "Autorizado con identidad root. Esta app no requiere root."
                else -> "Shizuku autorizado. UID del servicio: ${uid ?: "desconocido"}."
            }
        )
    }

    fun requestPermission() {
        check(Shizuku.pingBinder()) { "El servicio Shizuku no está disponible." }
        if (Shizuku.isPreV11()) {
            error("La versión del servicio Shizuku no es compatible. Actualízala.")
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }
}
