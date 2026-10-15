# In-app updates and the downloaded APK

**How updating works:** `UpdateChecker.checkForUpdate` reads the latest GitHub release; the tag must equal the new `versionCode`. The Today banner's Update button calls
`downloadAndInstallApk`: the APK (about 105 MB) is downloaded by Android's DownloadManager into the app's own storage (`getExternalFilesDir/amar_fit_update.apk`) and the
system installer opens.

**The storage problem (2026-10-15, Zunaid):** the file was never deleted after installing, so one ~105 MB APK stayed on the phone until the next update replaced it.

**Fix:** every update APK is deleted automatically.
* **Right after an update is installed:** `BootReceiver` (`MY_PACKAGE_REPLACED`) calls `UpdateChecker.cleanUpOldApks`, even if the app is never opened.
* **Whenever the app starts:** `MainActivity.onCreate` calls the same function (catches leftovers, for example from a cancelled download).
* **Before every new download:** every APK in the app's storage is deleted first, so there is never more than one.
* **Not deleted too early:** while an update is downloaded but not installed yet, its file is kept (the version code is remembered when the download starts); an
  abandoned download is dropped after 24 hours. The rule is `UpdateCleanupPolicy` (pure, tested).
* It looks at `.apk` files and `amar_fit_update*` files in the app's own folders only (external files, external cache, cache, files); nothing else is touched.
* The first launch of the version that contains this fix also deletes the APK left by the previous update (the existing 105 MB).
