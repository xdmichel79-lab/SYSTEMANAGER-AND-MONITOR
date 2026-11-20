package com.systemmanager.monitor.adb

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import rikka.shizuku.Shizuku

@Composable
fun AdbControlCenterScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bridge = remember { ShizukuBridge() }
    var status by remember { mutableStateOf(bridge.readStatus()) }
    var feedback by remember { mutableStateOf("ADB está separado de los permisos normales de la app.") }

    val binderListener = remember {
        Shizuku.OnBinderReceivedListener {
            status = bridge.readStatus()
        }
    }
    val binderDeadListener = remember {
        Shizuku.OnBinderDeadListener {
            status = bridge.readStatus()
        }
    }
    val permissionListener = remember {
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuBridge.REQUEST_CODE) {
                status = bridge.readStatus()
                feedback = if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    "Permiso Shizuku concedido."
                } else {
                    "Permiso Shizuku denegado."
                }
            }
        }
    }

    DisposableEffect(Unit) {
        Shizuku.addBinderReceivedListener(binderListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        onDispose {
            Shizuku.removeBinderReceivedListener(binderListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        }
    }

    LaunchedEffect(Unit) { status = bridge.readStatus() }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("ADB CONTROL CENTER", style = MaterialTheme.typography.headlineSmall)
            Text("USB · Wireless · Shizuku · Diagnóstico",
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ESTADO DE SHIZUKU", style = MaterialTheme.typography.titleMedium)
                    Text(status.message)
                    Text("Binder: ${if (status.binderAvailable) "Disponible" else "No disponible"}")
                    Text("Permiso: ${if (status.permissionGranted) "Concedido" else "Pendiente"}")
                    status.serverUid?.let { Text("UID del servicio: $it") }
                    Button(
                        onClick = {
                            runCatching { bridge.requestPermission() }
                                .onFailure { feedback = it.message ?: "No se pudo solicitar permiso." }
                            status = bridge.readStatus()
                        },
                        enabled = status.binderAvailable && !status.permissionGranted
                    ) { Text("Solicitar autorización Shizuku") }
                    Text(feedback, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("CONEXIONES ADB", style = MaterialTheme.typography.titleMedium)
            Text("USB ADB: conecta el teléfono a una computadora, activa Depuración USB y acepta su huella RSA.")
            OutlinedButton(onClick = {
                AdbSettingsIntents.openDeveloperOptions(context)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir opciones de desarrollador")
            }
            Spacer(Modifier.height(4.dp))
            Text("Wireless ADB: Android 11 o posterior. En Opciones de desarrollador abre Depuración inalámbrica y usa Emparejar con código. Ejecuta `adb pair IP:PUERTO` desde Platform Tools en la computadora.")
            OutlinedButton(onClick = {
                AdbSettingsIntents.openWirelessDebuggingHelp(context)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir ajustes de depuración")
            }
            OutlinedButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Guía oficial de Shizuku")
            }
        }
        item {
            Text("CATÁLOGO DE COMANDOS DE SOLO LECTURA",
                style = MaterialTheme.typography.titleMedium)
            Text("Cópialos en la consola ADB de tu computadora. Esta pantalla no finge ejecutarlos.")
        }
        items(readOnlyAdbCommands) { item ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                    Text(item.command, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
