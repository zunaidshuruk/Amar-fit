package com.example.data.repository

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object UpdateChecker {
    private const val REPO = "zunaidshuruk/Amar-fit"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/$REPO/releases/latest"

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(LATEST_RELEASE_URL).build()
            val response = RetrofitClient.okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "").removePrefix("v")
            val remoteVersionCode = tagName.toIntOrNull() ?: return@withContext null
            if (remoteVersionCode <= currentVersionCode) return@withContext null

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name", "").endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    break
                }
            }
            if (downloadUrl.isEmpty()) return@withContext null

            val versionName = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "")

            UpdateInfo(
                versionCode = remoteVersionCode,
                versionName = versionName,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes
            )
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Failed to check for update", e)
            null
        }
    }

    private const val PREFS = "UpdateChecker"
    private const val KEY_PENDING_VERSION = "pending_version_code"
    private const val KEY_PENDING_STARTED = "pending_started_at"

    /** Deletes every update APK in the app's own storage and returns how many bytes that freed. */
    private fun deleteUpdateFiles(context: Context): Long {
        var freed = 0L
        val dirs = listOfNotNull(context.getExternalFilesDir(null), context.externalCacheDir, context.cacheDir, context.filesDir)
        for (dir in dirs) {
            dir.listFiles()?.filter { it.isFile && UpdateCleanupPolicy.isUpdateFile(it.name) }?.forEach { file ->
                val size = file.length()
                if (file.delete()) freed += size
            }
        }
        return freed
    }

    /**
     * Frees the storage the downloaded update APKs take (about 105 MB each): called when the app starts and right after an update is installed
     * (BootReceiver, MY_PACKAGE_REPLACED). Files of an update that is downloaded but not installed yet are kept (see UpdateCleanupPolicy).
     */
    fun cleanUpOldApks(context: Context, installedVersionCode: Int) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val pendingVersion = prefs.getInt(KEY_PENDING_VERSION, 0)
            val pendingStarted = prefs.getLong(KEY_PENDING_STARTED, 0L)
            if (!UpdateCleanupPolicy.shouldDelete(installedVersionCode, pendingVersion, pendingStarted, System.currentTimeMillis())) return
            val freed = deleteUpdateFiles(context.applicationContext)
            prefs.edit().remove(KEY_PENDING_VERSION).remove(KEY_PENDING_STARTED).apply()
            if (freed > 0) android.util.Log.i("UpdateChecker", "Deleted old update files, freed ${freed / 1024 / 1024} MB")
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Could not clean up old update files", e)
        }
    }

    fun downloadAndInstallApk(context: Context, updateInfo: UpdateInfo) {
        // an older download (or any other leftover APK) goes first, so there is never more than one file
        deleteUpdateFiles(context)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_PENDING_VERSION, updateInfo.versionCode)
            .putLong(KEY_PENDING_STARTED, System.currentTimeMillis())
            .apply()
        val destination = File(context.getExternalFilesDir(null), "amar_fit_update.apk")

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("Downloading KardIQ Update")
            .setDescription("Downloading version ${updateInfo.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // ignore
                    }
                    installApk(ctx, destination)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
