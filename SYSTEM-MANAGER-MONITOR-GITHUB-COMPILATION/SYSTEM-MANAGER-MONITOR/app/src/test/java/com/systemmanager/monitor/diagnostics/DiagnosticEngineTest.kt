package com.systemmanager.monitor.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticEngineTest {
    @Test
    fun highRamUsageProducesWarning() {
        val report = DiagnosticEngine.evaluate(
            ramPercent = 94,
            storageTotalGb = 128,
            storageFreeGb = 40,
            batteryPercent = 80,
            batteryTemperatureC = 30f,
            sensorCount = 8,
            cpuDescriptionAvailable = true
        )
        assertTrue(report.items.any {
            it.title == "Memoria RAM" && it.status == DiagnosticStatus.WARNING
        })
    }

    @Test
    fun missingBatteryDataIsUnavailable() {
        val report = DiagnosticEngine.evaluate(
            ramPercent = 40,
            storageTotalGb = 128,
            storageFreeGb = 90,
            batteryPercent = null,
            batteryTemperatureC = null,
            sensorCount = 0,
            cpuDescriptionAvailable = false
        )
        assertEquals(
            DiagnosticStatus.UNAVAILABLE,
            report.items.first { it.title == "Batería" }.status
        )
    }
}
