package com.systemmanager.monitor.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

private data class SafeCommand(val label: String, val executable: String, val args: List<String>)

@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val commands = remember(context) {
        listOf(
            SafeCommand("pwd", "/system/bin/pwd", emptyList()),
            SafeCommand("ls", "/system/bin/ls", listOf("-la", context.filesDir.absolutePath)),
            SafeCommand("whoami", "/system/bin/id", emptyList()),
            SafeCommand("df -h", "/system/bin/df", listOf("-h", context.filesDir.absolutePath)),
            SafeCommand("Espacio app", "/system/bin/du", listOf("-sh", context.filesDir.absolutePath))
        )
    }
    var output by remember { mutableStateOf("SYSTEM MANAGER LOCAL TERMINAL\nEntorno limitado al UID y archivos privados de esta aplicación.\n") }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TERMINAL & COMMAND CENTER", style = MaterialTheme.typography.titleLarge, color = Color(0xFF36D9E8))
        Text("Terminal local · sin root · comandos permitidos", color = Color(0xFFB7C4D3), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            commands.forEach { command ->
                FilledTonalButton(enabled = !running, onClick = {
                    running = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { executeSafe(command) }
                        output += "\n$ ${command.label}\n$result\n"
                        running = false
                    }
                }) { Text(command.label) }
            }
        }
        Surface(Modifier.weight(1f).fillMaxWidth(), color = Color(0xFF07111D), shape = MaterialTheme.shapes.large) {
            Text(output, Modifier.padding(12.dp).verticalScroll(rememberScrollState()), color = Color(0xFF38D996), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
        if (running) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("La terminal no acepta texto shell arbitrario. Los comandos se ejecutan directamente con argumentos fijos y permisos normales de la app.", color = Color(0xFFFFB547), style = MaterialTheme.typography.bodySmall)
    }
}

private fun executeSafe(command: SafeCommand): String = try {
    val process = ProcessBuilder(listOf(command.executable) + command.args)
        .redirectErrorStream(true).start()
    if (!process.waitFor(3, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        "Tiempo de ejecución agotado."
    } else {
        process.inputStream.bufferedReader().use { it.readText().take(12000) }
    }
} catch (error: Exception) {
    "Comando no disponible en este dispositivo: ${error.javaClass.simpleName}"
}
