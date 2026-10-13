package com.example.data.notifications

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** Reads and writes [NotificationPrefs] as JSON. Anything unreadable falls back to the defaults. */
object NotificationPrefsCodec {
    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(NotificationPrefs::class.java)

    fun decode(json: String?): NotificationPrefs {
        if (json.isNullOrBlank()) return NotificationPrefs()
        return try {
            adapter.fromJson(json) ?: NotificationPrefs()
        } catch (e: Exception) {
            NotificationPrefs()
        }
    }

    fun encode(prefs: NotificationPrefs): String = adapter.toJson(prefs)
}
