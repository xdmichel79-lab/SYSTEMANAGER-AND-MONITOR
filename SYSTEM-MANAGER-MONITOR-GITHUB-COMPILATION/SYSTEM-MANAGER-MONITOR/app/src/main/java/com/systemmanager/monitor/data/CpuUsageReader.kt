package com.systemmanager.monitor.data

import java.io.File

/** Best-effort global CPU utilization from Linux /proc/stat. Returns null when unavailable. */
class CpuUsageReader {
    private var previousTotal: Long? = null
    private var previousIdle: Long? = null

    @Synchronized
    fun readPercent(): Int? = runCatching {
        val line = File("/proc/stat").useLines { lines -> lines.firstOrNull { it.startsWith("cpu ") } }
            ?: return null
        val values = line.trim().split(Regex("\\s+")).drop(1).map { it.toLong() }
        if (values.size < 4) return null
        val idle = values[3] + (values.getOrNull(4) ?: 0L)
        val total = values.sum()
        val oldTotal = previousTotal
        val oldIdle = previousIdle
        previousTotal = total
        previousIdle = idle
        if (oldTotal == null || oldIdle == null) return null
        val totalDelta = total - oldTotal
        val idleDelta = idle - oldIdle
        if (totalDelta <= 0) return null
        (((totalDelta - idleDelta).coerceAtLeast(0) * 100.0) / totalDelta).toInt().coerceIn(0, 100)
    }.getOrNull()
}
