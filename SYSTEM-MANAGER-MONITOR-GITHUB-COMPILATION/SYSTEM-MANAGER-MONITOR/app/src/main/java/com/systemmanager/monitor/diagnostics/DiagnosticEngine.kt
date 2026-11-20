package com.systemmanager.monitor.diagnostics

data class DiagnosticItem(
    val title: String,
    val status: DiagnosticStatus,
    val detail: String
)

enum class DiagnosticStatus { OK, WARNING, UNAVAILABLE }

data class DiagnosticReport(
    val generatedAtMillis: Long,
    val items: List<DiagnosticItem>
) {
    val warningCount: Int get() = items.count { it.status == DiagnosticStatus.WARNING }
    val unavailableCount: Int get() = items.count { it.status == DiagnosticStatus.UNAVAILABLE }
}

object DiagnosticEngine {
    fun evaluate(
        ramPercent: Int,
        storageTotalGb: Long,
        storageFreeGb: Long,
        batteryPercent: Int?,
        batteryTemperatureC: Float?,
        sensorCount: Int,
        cpuDescriptionAvailable: Boolean
    ): DiagnosticReport {
        val items = buildList {
            add(
                DiagnosticItem(
                    "Memoria RAM",
                    when {
                        ramPercent >= 90 -> DiagnosticStatus.WARNING
                        else -> DiagnosticStatus.OK
                    },
                    "$ramPercent% de memoria utilizada"
                )
            )
            val storagePercent = if (storageTotalGb > 0) {
                ((storageTotalGb - storageFreeGb).coerceAtLeast(0) * 100 / storageTotalGb).toInt()
            } else -1
            add(
                DiagnosticItem(
                    "Almacenamiento",
                    when {
                        storagePercent < 0 -> DiagnosticStatus.UNAVAILABLE
                        storagePercent >= 90 -> DiagnosticStatus.WARNING
                        else -> DiagnosticStatus.OK
                    },
                    if (storagePercent >= 0) "$storagePercent% ocupado" else "No se pudo calcular"
                )
            )
            add(
                DiagnosticItem(
                    "Batería",
                    when {
                        batteryPercent == null -> DiagnosticStatus.UNAVAILABLE
                        batteryPercent <= 15 -> DiagnosticStatus.WARNING
                        else -> DiagnosticStatus.OK
                    },
                    batteryPercent?.let { "$it% de carga" } ?: "Lectura no disponible"
                )
            )
            add(
                DiagnosticItem(
                    "Temperatura de batería",
                    when {
                        batteryTemperatureC == null -> DiagnosticStatus.UNAVAILABLE
                        batteryTemperatureC >= 45f -> DiagnosticStatus.WARNING
                        else -> DiagnosticStatus.OK
                    },
                    batteryTemperatureC?.let { "$it °C reportados por Android" }
                        ?: "Sensor no disponible"
                )
            )
            add(
                DiagnosticItem(
                    "Sensores",
                    if (sensorCount > 0) DiagnosticStatus.OK else DiagnosticStatus.UNAVAILABLE,
                    if (sensorCount > 0) "$sensorCount sensores enumerados" else "No se detectaron sensores"
                )
            )
            add(
                DiagnosticItem(
                    "Identificación CPU",
                    if (cpuDescriptionAvailable) DiagnosticStatus.OK else DiagnosticStatus.UNAVAILABLE,
                    if (cpuDescriptionAvailable) "Se obtuvo información del procesador" else "El sistema no expuso una descripción"
                )
            )
        }
        return DiagnosticReport(System.currentTimeMillis(), items)
    }
}
