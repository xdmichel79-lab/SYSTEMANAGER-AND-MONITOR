package com.systemmanager.monitor.data

import java.io.File

data class CpuProfile(
    val hardware: String,
    val processorDescription: String,
    val physicalCoreCandidates: Int,
    val onlineCores: Int,
    val maxFrequencyMhz: List<Int>,
    val minFrequencyMhz: List<Int>
)

object CpuInfoReader {
    fun read(): CpuProfile {
        val cpuInfo = runCatching { File("/proc/cpuinfo").readText() }.getOrDefault("")
        fun firstValue(vararg keys: String): String {
            val line = cpuInfo.lineSequence().firstOrNull { line ->
                keys.any { key -> line.trim().startsWith("$key", ignoreCase = true) }
            } ?: return "No disponible"
            return line.substringAfter(':', "").trim().ifBlank { "No disponible" }
        }

        val cpuDirs = File("/sys/devices/system/cpu")
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory && Regex("cpu[0-9]+").matches(it.name) }

        val online = cpuDirs.count { dir ->
            val onlineFile = File(dir, "online")
            if (!onlineFile.exists()) dir.name == "cpu0"
            else runCatching { onlineFile.readText().trim() == "1" }.getOrDefault(false)
        }.coerceAtLeast(1)

        fun frequencies(fileName: String): List<Int> = cpuDirs.mapNotNull { dir ->
            val file = File(dir, "cpufreq/$fileName")
            val khz = runCatching { file.readText().trim().toLong() }.getOrNull()
            khz?.takeIf { it > 0 }?.let { (it / 1000L).toInt() }
        }.distinct().sorted()

        val maxFreq = frequencies("cpuinfo_max_freq")
        val minFreq = frequencies("cpuinfo_min_freq")
        val hardware = firstValue("Hardware", "hardware")
        val processor = firstValue("model name", "Processor", "cpu model")

        return CpuProfile(
            hardware = hardware,
            processorDescription = processor,
            physicalCoreCandidates = cpuDirs.size.coerceAtLeast(online),
            onlineCores = online,
            maxFrequencyMhz = maxFreq,
            minFrequencyMhz = minFreq
        )
    }
}
