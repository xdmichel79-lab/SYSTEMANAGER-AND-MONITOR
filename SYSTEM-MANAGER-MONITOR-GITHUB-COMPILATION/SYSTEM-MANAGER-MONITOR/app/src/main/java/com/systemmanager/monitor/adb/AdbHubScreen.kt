package com.systemmanager.monitor.adb

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

private val adbExamples = listOf(
    "getprop ro.build.version.release" to "Versión Android",
    "dumpsys battery" to "Estado de batería",
    "dumpsys meminfo" to "Memoria",
    "pm list packages -3" to "Paquetes de terceros",
    "ps -A" to "Procesos visibles",
    "wm size" to "Resolución lógica",
    "wm density" to "Densidad de pantalla"
)

@Composable
fun AdbHubScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("ADB CONTROL CENTER", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("El teléfono puede ser el dispositivo ADB; para ejecutar ADB shell normalmente se necesita una computadora emparejada o Shizuku autorizado.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }, modifier = Modifier.fillMaxWidth()) { Text("Abrir opciones de desarrollador") }
            OutlinedButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://shizuku.rikka.app/"))) }
            }, modifier = Modifier.fillMaxWidth()) { Text("Abrir guía oficial de Shizuku") }
        }
        item { Text("COMANDOS ADB DE SOLO LECTURA", style = MaterialTheme.typography.titleMedium) }
        items(adbExamples) { (command, description) ->
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(description, style = MaterialTheme.typography.labelMedium)
                    Text(command, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { clipboard.setText(AnnotatedString(command)) }) { Text("Copiar comando") }
                }
            }
        }
        item {
            Text("USB ADB: conecta el teléfono a una computadora, activa Depuración USB y acepta la huella RSA. Wireless ADB: Android 11+ permite emparejar una computadora desde Depuración inalámbrica. Esta APK no puede habilitar ADB ni emparejarse a sí misma.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
