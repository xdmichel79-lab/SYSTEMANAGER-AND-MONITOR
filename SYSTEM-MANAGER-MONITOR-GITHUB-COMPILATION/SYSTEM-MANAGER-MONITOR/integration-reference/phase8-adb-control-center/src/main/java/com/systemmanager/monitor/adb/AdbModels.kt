package com.systemmanager.monitor.adb

data class AdbStatus(
    val shizukuInstalled: Boolean = false,
    val binderAvailable: Boolean = false,
    val permissionGranted: Boolean = false,
    val serverUid: Int? = null,
    val message: String = "Comprueba el estado de Shizuku"
)

data class AdbCommand(
    val title: String,
    val command: String,
    val description: String
)

val readOnlyAdbCommands = listOf(
    AdbCommand("Información Android", "getprop ro.build.version.release",
        "Versión Android informada por el sistema."),
    AdbCommand("Modelo", "getprop ro.product.model",
        "Modelo del dispositivo."),
    AdbCommand("Resolución", "wm size",
        "Resolución lógica de pantalla."),
    AdbCommand("Densidad", "wm density",
        "Densidad lógica de pantalla."),
    AdbCommand("Batería", "dumpsys battery",
        "Estado y lecturas de batería expuestas por Android."),
    AdbCommand("Memoria", "cat /proc/meminfo",
        "Resumen de memoria del kernel, si el acceso está permitido."),
    AdbCommand("Paquetes de terceros", "pm list packages -3",
        "Lista paquetes de terceros visibles para el shell ADB."),
    AdbCommand("Procesos", "ps -A",
        "Procesos visibles para el usuario shell en esa versión.")
)
