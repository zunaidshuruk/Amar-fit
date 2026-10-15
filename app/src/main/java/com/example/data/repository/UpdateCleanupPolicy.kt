package com.example.data.repository

/**
 * When the downloaded update APKs (about 105 MB each) may be deleted. The rule: delete them all as soon as the update they belong to is
 * installed, or when nothing is being downloaded; keep them only while an update is downloaded but not installed yet (and give up on that
 * after [STALE_AFTER_MILLIS], for example when the person cancelled the installer).
 */
object UpdateCleanupPolicy {
    const val STALE_AFTER_MILLIS = 24L * 60L * 60L * 1000L

    /** The updater's own file name, any other APK left in the app's storage, and half-finished downloads of it. */
    fun isUpdateFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".apk") || lower.startsWith("amar_fit_update")
    }

    /**
     * [pendingVersionCode] is the version code of the update that was being downloaded (0 when none), [pendingStartedAt] when it started.
     * Returns true when the downloaded files can go.
     */
    fun shouldDelete(installedVersionCode: Int, pendingVersionCode: Int, pendingStartedAt: Long, now: Long): Boolean {
        if (pendingVersionCode <= 0) return true                         // nothing is being downloaded
        if (installedVersionCode >= pendingVersionCode) return true      // the update is installed: its file is no longer needed
        return now - pendingStartedAt > STALE_AFTER_MILLIS               // an old, abandoned download
    }
}
