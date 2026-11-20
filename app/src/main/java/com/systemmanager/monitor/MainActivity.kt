package com.systemmanager.monitor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.systemmanager.monitor.data.CpuUsageReader
import com.systemmanager.monitor.data.DeviceSnapshot
import com.systemmanager.monitor.data.DeviceSnapshotReader
import com.systemmanager.monitor.diagnostics.DiagnosticEngine
import com.systemmanager.monitor.diagnostics.DiagnosticItem
import com.systemmanager.monitor.diagnostics.DiagnosticStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SystemManagerApp() }
    }
}

private val Navy = Color(0xFF101A2A)
private val Panel = Color(0xFF1B2B40)
private val Cyan = Color(0xFF36D9E8)
private val Green = Color(0xFF38D996)
private val Amber = Color(0xFFFFB547)
private val Muted = Color(0xFFB7C4D3)

private data class Sample(val time: Long, val ram: Int, val cpu: Int?, val battery: Int?)

@Composable
fun SystemManagerApp() {
    val context = LocalContext.current
    val cpuReader = remember { CpuUsageReader() }
    var snapshot by remember { mutableStateOf(DeviceSnapshotReader.read(context)) }
    var cpuUsage by remember { mutableStateOf<Int?>(null) }
    var refreshSeconds by remember { mutableIntStateOf(5) }
    val history = remember { mutableStateListOf<Sample>() }

    LaunchedEffect(refreshSeconds) {
        while (true) {
            snapshot = DeviceSnapshotReader.read(context)
            cpuUsage = cpuReader.readPercent()
            history.add(Sample(System.currentTimeMillis(), snapshot.ramPercent, cpuUsage, snapshot.batteryPercent))
            while (history.size > 60) history.removeAt(0)
            delay(refreshSeconds * 1_000L)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Green, background = Navy, surface = Panel)) {
        Surface(Modifier.fillMaxSize(), color = Navy) {
            var page by remember { mutableStateOf("Inicio") }
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Inicio", "ADB", "Terminal").forEach { item ->
                        FilterChip(selected = page == item, onClick = { page = item }, label = { Text(item, fontSize = 11.sp) })
                    }
                }
                when (page) {
                    "ADB" -> com.systemmanager.monitor.adb.AdbHubScreen()
                    "Terminal" -> com.systemmanager.monitor.terminal.TerminalScreen()
                    else -> Dashboard(snapshot, cpuUsage, history.toList(), refreshSeconds,
                        onRefreshChange = { refreshSeconds = it },
                        onExport = { exportReport(context, snapshot, cpuUsage, history.toList()) })
                }
            }
        }
    }
}

@Composable
private fun Dashboard(snapshot: DeviceSnapshot, cpuUsage: Int?, history: List<Sample>, refresh: Int,
                      onRefreshChange: (Int) -> Unit, onExport: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 600.dp) 2 else 1
        val cpu = snapshot.cpuProfile
        val diagnostics = DiagnosticEngine.evaluate(snapshot.ramPercent, snapshot.storageTotalGb,
            snapshot.storageFreeGb, snapshot.batteryPercent, snapshot.batteryTemperatureC,
            snapshot.sensorCount, cpu.processorDescription != "No disponible" || cpu.hardware != "No disponible")
        LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) { Header(snapshot) }
            item { MetricCard("USO DE CPU", cpuUsage?.let { "$it%" } ?: "Midiendo…", "Carga global reportada por /proc/stat", Green) }
            item { MetricCard("MEMORIA RAM", "${snapshot.ramUsedMb} / ${snapshot.ramTotalMb} MB", "${snapshot.ramPercent}% utilizada", Cyan) }
            item { MetricCard("PROCESADOR", "${cpu.onlineCores} núcleos en línea", "Disponibles para app: ${snapshot.cpuCores}", Green) }
            item { MetricCard("BATERÍA", snapshot.batteryPercent?.let { "$it%" } ?: "No disponible", "${snapshot.batteryStatus} · Salud: ${snapshot.batteryHealth}", Amber) }
            item { MetricCard("TEMPERATURA DE BATERÍA", snapshot.batteryTemperatureC?.let { "${it} °C" } ?: "No disponible", "No equivale a temperatura CPU/GPU", Amber) }
            item { MetricCard("ALMACENAMIENTO", "${snapshot.storageFreeGb} GB libres", "${snapshot.storageTotalGb} GB totales", Cyan) }
            item { MetricCard("SENSORES", "${snapshot.sensorCount} detectados", "Enumerados por Android", Green) }
            item(span = { GridItemSpan(maxLineSpan) }) { HistoryPanel(history) }
            item(span = { GridItemSpan(maxLineSpan) }) { CpuDetails(snapshot) }
            item(span = { GridItemSpan(maxLineSpan) }) { DiagnosticsPanel(diagnostics.items) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                CardPanel {
                    Text("CONTROLES", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Frecuencia de actualización", color = Muted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 5, 10).forEach { seconds ->
                            FilterChip(selected = refresh == seconds, onClick = { onRefreshChange(seconds) }, label = { Text("${seconds}s") })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Exportar informe de diagnóstico") }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { AppManagerPanel(androidx.compose.ui.platform.LocalContext.current) }
            item(span = { GridItemSpan(maxLineSpan) }) { BootStartupPanel(androidx.compose.ui.platform.LocalContext.current) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Historial local de hasta 60 muestras. La carga de CPU depende de que Android permita leer /proc/stat. Las lecturas térmicas disponibles corresponden a batería; CPU/GPU pueden estar restringidas.",
                    color = Muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable private fun Header(s: DeviceSnapshot) {
    Column {
        Text("SYSTEM MANAGER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("HARDWARE • PERFORMANCE • DIAGNOSTICS", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text("${s.brand} ${s.manufacturer} ${s.model}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("${s.deviceCode} · Android ${s.androidVersion} · API ${s.apiLevel}", color = Muted, fontSize = 12.sp)
    }
}

@Composable private fun HistoryPanel(history: List<Sample>) {
    CardPanel {
        Text("HISTORIAL DE RENDIMIENTO", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text("RAM utilizada (%)", color = Muted, fontSize = 11.sp)
        MiniChart(history.map { it.ram.toFloat() }, Cyan)
        Spacer(Modifier.height(10.dp))
        Text("CPU global (%) · sin datos cuando el sistema no lo permite", color = Muted, fontSize = 11.sp)
        MiniChart(history.mapNotNull { it.cpu?.toFloat() }, Green)
        Spacer(Modifier.height(10.dp))
        Text("Batería (%)", color = Muted, fontSize = 11.sp)
        MiniChart(history.mapNotNull { it.battery?.toFloat() }, Amber)
        Text("${history.size} / 60 muestras", color = Muted, fontSize = 10.sp)
    }
}

@Composable private fun MiniChart(values: List<Float>, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
        if (values.isEmpty()) return@Canvas
        val maxValue = 100f
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = if (values.size == 1) 0f else size.width * index / (values.size - 1)
            val y = size.height - (value.coerceIn(0f, maxValue) / maxValue * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawLine(color.copy(alpha = .18f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
    }
}

@Composable private fun CpuDetails(s: DeviceSnapshot) {
    val cpu = s.cpuProfile
    CardPanel {
        Text("IDENTIFICACIÓN AVANZADA DE CPU", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        DetailLine("Hardware", cpu.hardware)
        DetailLine("Descripción", cpu.processorDescription)
        DetailLine("ABI", s.abi)
        DetailLine("Núcleos enumerados / en línea", "${cpu.physicalCoreCandidates} / ${cpu.onlineCores}")
        DetailLine("Frecuencia máxima", cpu.maxFrequencyMhz.takeIf { it.isNotEmpty() }?.joinToString { "$it MHz" } ?: "No expuesta")
        DetailLine("Frecuencia mínima", cpu.minFrequencyMhz.takeIf { it.isNotEmpty() }?.joinToString { "$it MHz" } ?: "No expuesta")
        DetailLine("Pantalla", "${s.screenWidthPx} × ${s.screenHeightPx} px · ${s.screenDensityDpi} dpi")
    }
}

@Composable private fun DiagnosticsPanel(items: List<DiagnosticItem>) {
    CardPanel {
        Text("DIAGNÓSTICO AUTOMÁTICO", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            val color = when (item.status) { DiagnosticStatus.OK -> Green; DiagnosticStatus.WARNING -> Amber; DiagnosticStatus.UNAVAILABLE -> Muted }
            Text("${item.title} · ${item.status.name}", color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(item.detail, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

@Composable private fun MetricCard(title: String, value: String, detail: String, accent: Color) {
    CardPanel {
        Text(title, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(detail, color = Muted, fontSize = 12.sp)
    }
}

@Composable private fun CardPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable private fun DetailLine(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, color = Muted, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 13.sp)
    }
}

private fun exportReport(context: android.content.Context, s: DeviceSnapshot, cpu: Int?, history: List<Sample>) {
    val report = buildString {
        appendLine("SYSTEM MANAGER & MONITOR — INFORME")
        appendLine("Generado: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        appendLine("Dispositivo: ${s.manufacturer} ${s.brand} ${s.model} (${s.deviceCode})")
        appendLine("Android: ${s.androidVersion} · API ${s.apiLevel} · ABI ${s.abi}")
        appendLine("CPU: ${cpu?.let { "$it%" } ?: "No disponible"}; núcleos online: ${s.cpuProfile.onlineCores}; disponibles al proceso: ${s.cpuCores}")
        appendLine("RAM: ${s.ramUsedMb}/${s.ramTotalMb} MB (${s.ramPercent}%)")
        appendLine("Batería: ${s.batteryPercent?.let { "$it%" } ?: "No disponible"} · ${s.batteryStatus} · salud ${s.batteryHealth}")
        appendLine("Temperatura batería: ${s.batteryTemperatureC?.let { "$it °C" } ?: "No disponible"}")
        appendLine("Almacenamiento: ${s.storageFreeGb} GB libres de ${s.storageTotalGb} GB")
        appendLine("Sensores: ${s.sensorCount}")
        val diagnostics = DiagnosticEngine.evaluate(s.ramPercent, s.storageTotalGb, s.storageFreeGb,
            s.batteryPercent, s.batteryTemperatureC, s.sensorCount,
            s.cpuProfile.processorDescription != "No disponible" || s.cpuProfile.hardware != "No disponible")
        appendLine("DIAGNÓSTICO:")
        diagnostics.items.forEach { appendLine("- ${it.title}: ${it.status.name} — ${it.detail}") }
        appendLine("Muestras históricas: ${history.size}")
        appendLine("Nota: métricas sujetas a restricciones del fabricante y versión Android.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Informe System Manager"); putExtra(Intent.EXTRA_TEXT, report) }
    context.startActivity(Intent.createChooser(intent, "Compartir informe"))
}
