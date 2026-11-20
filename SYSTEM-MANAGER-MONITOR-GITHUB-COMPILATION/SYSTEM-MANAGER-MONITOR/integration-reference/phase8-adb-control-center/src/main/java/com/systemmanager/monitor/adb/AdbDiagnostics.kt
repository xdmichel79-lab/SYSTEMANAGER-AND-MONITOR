package com.systemmanager.monitor.adb

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import java.io.File

data class LocalAdbDiagnostics(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdk: Int,
    val processPssKb: Int,
    val availableRamBytes: Long,
    val totalRamBytes: Long,
    val batteryPercent: Int?
)

object AdbDiagnostics {
    fun collect(context: Context): LocalAdbDiagnostics {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo().also(am::getMemoryInfo)
        val battery = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else null

        return LocalAdbDiagnostics(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            sdk = Build.VERSION.SDK_INT,
            processPssKb = Debug.getPss(),
            availableRamBytes = memory.availMem,
            totalRamBytes = memory.totalMem,
            batteryPercent = percent
        )
    }

    fun readableProcMemInfo(): String? = runCatching {
        File("/proc/meminfo").takeIf { it.canRead() }?.readText()
    }.getOrNull()
}
