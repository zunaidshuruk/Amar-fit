package com.example.util.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File

object OnDeviceModelStore {

    private fun legacyModelsDir(context: Context): File {
        return File(context.filesDir, "models")
    }

    private fun externalModelsDir(context: Context): File? {
        return context.getExternalFilesDir("models")
    }

    fun findModelFile(context: Context): File? {
        val ext = externalModelsDir(context)
        val dirs = listOfNotNull(ext, legacyModelsDir(context))
        for (dir in dirs) {
            if (dir.exists()) {
                val files = dir.listFiles { _, name -> name.endsWith(".litertlm") }
                val found = files?.firstOrNull() ?: files?.maxByOrNull { it.lastModified() }
                if (found != null && found.exists() && found.length() > 0L) {
                    return found
                }
            }
        }
        return null
    }

    fun getEngineSetting(context: Context): AiEngineSetting {
        val prefs = context.getSharedPreferences("ShasthoPrefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("ai_engine", "cloud") ?: "cloud"
        return when (raw.lowercase()) {
            "auto" -> AiEngineSetting.AUTO
            "on_device", "ondevice" -> AiEngineSetting.ON_DEVICE
            else -> AiEngineSetting.CLOUD
        }
    }

    fun setEngineSetting(context: Context, setting: AiEngineSetting) {
        val prefs = context.getSharedPreferences("ShasthoPrefs", Context.MODE_PRIVATE)
        val raw = when (setting) {
            AiEngineSetting.CLOUD -> "cloud"
            AiEngineSetting.AUTO -> "auto"
            AiEngineSetting.ON_DEVICE -> "on_device"
        }
        prefs.edit().putString("ai_engine", raw).apply()
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
