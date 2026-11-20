package com.systemmanager.monitor.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.roundToInt

object DeviceSnapshotReader {
    fun read(context: Context): DeviceSnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)

        val totalRamMb = bytesToMb(memory.totalMem)
        val availableRamMb = bytesToMb(memory.availMem)
        val usedRamMb = (totalRamMb - availableRamMb).coerceAtLeast(0)
        val ramPercent = if (totalRamMb > 0) {
            ((usedRamMb * 100.0) / totalRamMb).roundToInt()
        } else 0

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else null

        val rawStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val batteryStatus = when (rawStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Cargando"
            BatteryManager.BATTERY_STATUS_FULL -> "Completa"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descargando"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "No está cargando"
            else -> "Desconocido"
        }
        val rawHealth = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val batteryHealth = when (rawHealth) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Buena"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Sobrecalentamiento reportado"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Fin de vida reportado"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobrevoltaje reportado"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Fallo no especificado"
            BatteryManager.BATTERY_HEALTH_COLD -> "Temperatura baja reportada"
            else -> "Desconocido"
        }

        val rawTemperature = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE
        ) ?: Int.MIN_VALUE
        val batteryTemperature = if (rawTemperature != Int.MIN_VALUE) rawTemperature / 10f else null

        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(SensorManager.TYPE_ALL)
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)

        return DeviceSnapshot(
            manufacturer = Build.MANUFACTURER.orUnknown(),
            brand = Build.BRAND.orUnknown(),
            model = Build.MODEL.orUnknown(),
            deviceCode = Build.DEVICE.orUnknown(),
            androidVersion = Build.VERSION.RELEASE.orUnknown(),
            apiLevel = Build.VERSION.SDK_INT,
            abi = Build.SUPPORTED_ABIS.firstOrNull().orUnknown(),
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            cpuProfile = CpuInfoReader.read(),
            ramTotalMb = totalRamMb,
            ramUsedMb = usedRamMb,
            ramPercent = ramPercent,
            batteryPercent = batteryPercent,
            batteryStatus = batteryStatus,
            batteryHealth = batteryHealth,
            batteryTemperatureC = batteryTemperature,
            storageTotalGb = bytesToGb(stat.totalBytes),
            storageFreeGb = bytesToGb(stat.availableBytes),
            screenWidthPx = displayMetrics.widthPixels,
            screenHeightPx = displayMetrics.heightPixels,
            screenDensityDpi = displayMetrics.densityDpi,
            sensorCount = sensors.size,
            sensorNames = sensors.map { it.name }.distinct()
        )
    }

    private fun bytesToMb(bytes: Long): Long = bytes / (1024L * 1024L)
    private fun bytesToGb(bytes: Long): Long = bytes / (1024L * 1024L * 1024L)
    private fun String?.orUnknown(): String = this?.takeIf { it.isNotBlank() } ?: "No disponible"
}
