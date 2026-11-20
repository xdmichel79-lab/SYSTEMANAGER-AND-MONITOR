package com.systemmanager.monitor.data

data class DeviceSnapshot(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val deviceCode: String,
    val androidVersion: String,
    val apiLevel: Int,
    val abi: String,
    val cpuCores: Int,
    val cpuProfile: CpuProfile,
    val ramTotalMb: Long,
    val ramUsedMb: Long,
    val ramPercent: Int,
    val batteryPercent: Int?,
    val batteryStatus: String,
    val batteryHealth: String,
    val batteryTemperatureC: Float?,
    val storageTotalGb: Long,
    val storageFreeGb: Long,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val screenDensityDpi: Int,
    val sensorCount: Int,
    val sensorNames: List<String>
)
