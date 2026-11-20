package com.systemmanager.monitor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

private data class ManagedApp(
    val label: String,
    val packageName: String,
    val version: String,
    val isSystem: Boolean
)

@Composable
fun AppManagerPanel(context: Context) {
    val pm = context.packageManager
    var apps by remember { mutableStateOf(loadLaunchableApps(pm)) }
    var filter by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ManagedApp?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var showGlobalInfo by remember { mutableStateOf(false) }

    val visibleApps = remember(apps, filter) {
        apps.filter { it.label.contains(filter, true) || it.packageName.contains(filter, true) }
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2B40))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ADMINISTRADOR AVANZADO DE APLICACIONES", color = Color(0xFF36D9E8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Gestiona aplicaciones instaladas, permisos y aplicaciones predeterminadas mediante las pantallas oficiales de Android.", color = Color(0xFFB7C4D3), fontSize = 12.sp)

            OutlinedButton(onClick = { showGlobalInfo = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Restablecer preferencias (asistente)")
            }
            OutlinedButton(onClick = { openSettings(context, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS) }, modifier = Modifier.fillMaxWidth()) {
                Text("Administrar apps predeterminadas")
            }
            OutlinedButton(onClick = { openSettings(context, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS) }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir ajustes de todas las aplicaciones")
            }

            OutlinedTextField(value = filter, onValueChange = { filter = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar aplicación o paquete") })
            Text("Aplicaciones visibles: ${visibleApps.size}", color = Color(0xFFB7C4D3), fontSize = 11.sp)
            if (visibleApps.isEmpty()) {
                Text("No se encontraron aplicaciones de inicio. Android limita la visibilidad de paquetes por privacidad.", color = Color(0xFFFFB547), fontSize = 12.sp)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visibleApps, key = { it.packageName }) { app ->
                        Surface(color = Color(0xFF101A2A), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(app.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(app.packageName, color = Color(0xFF36D9E8), fontSize = 10.sp)
                                Text("Versión ${app.version} · ${if (app.isSystem) "Sistema" else "Instalada"}", color = Color(0xFFB7C4D3), fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { selected = app }) { Text("Administrar") }
                                    TextButton(onClick = { openAppDetails(context, app.packageName) }) { Text("Permisos / datos") }
                                }
                            }
                        }
                    }
                }
            }
            Text("Limitación Android: esta app no puede borrar datos/caché de otras apps ni restablecerlas silenciosamente. Para borrar datos, usa la pantalla de información de cada app y confirma allí. Las apps sin icono de inicio pueden no aparecer en esta lista.", color = Color(0xFFB7C4D3), fontSize = 10.sp)
        }
    }

    selected?.let { app ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(app.label) },
            text = { Text("¿Qué deseas hacer? Android abrirá sus ajustes oficiales. Desde allí podrás revisar permisos, almacenamiento y, si el sistema lo permite, borrar datos o caché. También puedes solicitar su desinstalación.") },
            confirmButton = { TextButton(onClick = { openAppDetails(context, app.packageName); selected = null }) { Text("Abrir ajustes") } },
            dismissButton = { TextButton(onClick = { confirm = app.packageName; selected = null }) { Text("Desinstalar…") } }
        )
    }
    confirm?.let { packageName ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Solicitar desinstalación") },
            text = { Text("Se abrirá el diálogo del sistema para $packageName. La desinstalación solo ocurrirá si la confirmas allí.") },
            confirmButton = { TextButton(onClick = { requestUninstall(context, packageName); confirm = null }) { Text("Continuar") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } }
        )
    }
    if (showGlobalInfo) {
        AlertDialog(
            onDismissRequest = { showGlobalInfo = false },
            title = { Text("Restablecer preferencias de apps") },
            text = { Text("Android no ofrece a las aplicaciones normales una API pública para ejecutar este restablecimiento global directamente. Puedes abrir Ajustes > Aplicaciones y usar el menú del sistema para restablecer preferencias. Esto no borra los datos personales de las aplicaciones, pero puede reactivar apps deshabilitadas, restablecer restricciones y asociaciones según el fabricante.") },
            confirmButton = { TextButton(onClick = { showGlobalInfo = false; openSettings(context, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS) }) { Text("Abrir ajustes") } },
            dismissButton = { TextButton(onClick = { showGlobalInfo = false }) { Text("Cerrar") } }
        )
    }
}

private fun loadLaunchableApps(pm: PackageManager): List<ManagedApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .mapNotNull { resolve ->
            val info = resolve.activityInfo?.applicationInfo ?: return@mapNotNull null
            val pkg = info.packageName
            val label = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(pkg)
            val version = runCatching { packageVersion(pm, pkg) }.getOrDefault("?")
            ManagedApp(label, pkg, version, (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

@Suppress("DEPRECATION")
private fun packageVersion(pm: PackageManager, packageName: String): String {
    val info = pm.getPackageInfo(packageName, 0)
    return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toString() else info.versionCode.toString()
}

private fun openAppDetails(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun requestUninstall(context: Context, packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun openSettings(context: Context, action: String) {
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
