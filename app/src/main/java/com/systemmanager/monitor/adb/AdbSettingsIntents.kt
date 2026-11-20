package com.systemmanager.monitor.adb

import android.content.Context
import android.content.Intent
import android.provider.Settings

object AdbSettingsIntents {

    fun openDeveloperOptions(context: Context): Boolean =
        launch(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))

    fun openWirelessDebuggingHelp(context: Context): Boolean {
        // Android no ofrece un Intent público y uniforme para abrir directamente
        // la subpantalla de depuración inalámbrica en todos los fabricantes.
        return openDeveloperOptions(context)
    }

    private fun launch(context: Context, intent: Intent): Boolean {
        return runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
