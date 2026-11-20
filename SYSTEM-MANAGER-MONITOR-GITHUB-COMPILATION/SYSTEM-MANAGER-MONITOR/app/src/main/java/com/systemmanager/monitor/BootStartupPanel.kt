package com.systemmanager.monitor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class BootReceiverApp(
    val label: String,
    val packageName: String,
    val system: Boolean
)

/** Shows packages that declare a BOOT_COMPLETED receiver, when package visibility allows it. */
@Composable
fun BootStartupPanel(context: Context) {
    var apps by remember { mutableStateOf(findBootReceiverApps(context)) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<BootReceiverApp?>(null) }
    val filtered = remember(apps, query) {
        apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2B40))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("INICIO AUTOMÁTICO Y PERMISOS", color = Color(0xFF36D9E8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                "Detecta aplicaciones que declaran receptores de arranque y permite revisar sus permisos y controles de actividad en segundo plano.",
                color = Color(0xFFB7C4D3), fontSize = 12.sp
            )
            OutlinedButton(onClick = { openBootSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir controles de inicio automático del fabricante")
            }
            OutlinedButton(onClick = { openBatterySettings(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir optimización de batería de Android")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar aplicación") }
            )
            Text("Aplicaciones con receptor de arranque visibles: ${filtered.size}", color = Color(0xFFB7C4D3), fontSize = 11.sp)
            if (filtered.isEmpty()) {
                Text("No se encontraron receptores visibles. Android limita la visibilidad de paquetes y algunos fabricantes gestionan el arranque por separado.", color = Color(0xFFFFB547), fontSize = 12.sp)
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Surface(color = Color(0xFF101A2A), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(app.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(app.packageName, color = Color(0xFF36D9E8), fontSize = 10.sp)
                                    Text(if (app.system) "Aplicación del sistema" else "Aplicación instalada", color = Color(0xFFB7C4D3), fontSize = 10.sp)
                                }
                                TextButton(onClick = { selected = app }) { Text("Revisar") }
                            }
                        }
                    }
                }
            }
            Text(
                "Importante: Android no ofrece a una APK normal una API pública para desactivar los receptores BOOT_COMPLETED de otras apps ni revocar masivamente todos sus permisos. La app no cambiará permisos ni bloqueará arranques en secreto. Usa los controles del fabricante o los ajustes oficiales de cada aplicación.",
                color = Color(0xFFFFB547), fontSize = 10.sp
            )
        }
    }

    selected?.let { app ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Revisar ${app.label}") },
            text = { Text("Se abrirá la información de la aplicación. Allí podrás revisar permisos, uso de batería y opciones de actividad en segundo plano que tu versión de Android o fabricante permita.") },
            confirmButton = {
                TextButton(onClick = {
                    openAppSettings(context, app.packageName)
                    selected = null
                }) { Text("Abrir ajustes") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Cancelar") } }
        )
    }
}

@Suppress("DEPRECATION")
private fun findBootReceiverApps(context: Context): List<BootReceiverApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
    return runCatching {
        pm.queryBroadcastReceivers(intent, PackageManager.MATCH_DISABLED_COMPONENTS)
            .mapNotNull { result ->
                val info = result.activityInfo?.applicationInfo ?: return@mapNotNull null
                val packageName = info.packageName
                val label = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(packageName)
                BootReceiverApp(label, packageName, (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())
}

private fun openAppSettings(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun openBatterySettings(context: Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

private fun openBootSettings(context: Context) {
    // There is no standardized Android intent for OEM-specific auto-start controls.
    // Try common vendor activities; fall back to the app settings screen.
    val candidates = listOf(
        Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.parse("package:${context.packageName}"))
    ).map { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    for (intent in candidates) {
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Try the next known/fallback destination.
        }
    }
    runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
