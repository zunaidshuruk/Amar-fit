package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.view.KeyEvent
import android.widget.Toast

object WorkoutMusic {
    enum class MusicApp(val label: String, val packageName: String?) {
        SPOTIFY("Spotify", "com.spotify.music"),
        YOUTUBE_MUSIC("YouTube Music", "com.google.android.apps.youtube.music"),
        YOUTUBE("YouTube", "com.google.android.youtube"),
        ANY("Any music app", null)
    }

    private const val PREFS_NAME = "ShasthoPrefs"
    private const val KEY_MUSIC_APP = "music_app"
    private const val KEY_SPOTIFY_LINK = "music_spotify_link"
    private const val KEY_YOUTUBE_LINK = "music_youtube_link"
    private const val KEY_CONTROLS_ENABLED = "music_controls_enabled"
    private const val KEY_AUTO_START = "music_auto_start"

    fun getMusicApp(context: Context): MusicApp {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_MUSIC_APP, MusicApp.ANY.name)
        return try {
            if (name != null) MusicApp.valueOf(name) else MusicApp.ANY
        } catch (e: IllegalArgumentException) {
            MusicApp.ANY
        }
    }

    fun setMusicApp(context: Context, app: MusicApp) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MUSIC_APP, app.name).apply()
    }

    fun getSpotifyLink(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SPOTIFY_LINK, "") ?: ""
    }

    fun setSpotifyLink(context: Context, link: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SPOTIFY_LINK, link).apply()
    }

    fun getYoutubeLink(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_YOUTUBE_LINK, "") ?: ""
    }

    fun setYoutubeLink(context: Context, link: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_YOUTUBE_LINK, link).apply()
    }

    fun isControlsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CONTROLS_ENABLED, true)
    }

    fun setControlsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_CONTROLS_ENABLED, enabled).apply()
    }

    fun isAutoStartEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_START, false)
    }

    fun setAutoStartEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun isValidSpotifyLink(link: String): Boolean {
        val trimmed = link.trim()
        return trimmed.startsWith("https://open.spotify.com/") || trimmed.startsWith("spotify:")
    }

    fun isValidYoutubeLink(link: String): Boolean {
        val trimmed = link.trim()
        return trimmed.startsWith("https://music.youtube.com/") ||
                trimmed.startsWith("https://www.youtube.com/") ||
                trimmed.startsWith("https://youtube.com/") ||
                trimmed.startsWith("https://m.youtube.com/") ||
                trimmed.startsWith("https://youtu.be/")
    }

    fun sendMediaKey(context: Context, keyCode: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        } catch (e: Exception) {
            // never throw
        }
    }

    fun play(context: Context) {
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    fun togglePlayPause(context: Context) {
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun next(context: Context) {
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previous(context: Context) {
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun openPlaylist(context: Context): Boolean {
        val preferredApp = getMusicApp(context)
        val spotifyLink = getSpotifyLink(context).trim()
        val youtubeLink = getYoutubeLink(context).trim()

        val rawLink: String = when (preferredApp) {
            MusicApp.SPOTIFY -> spotifyLink
            MusicApp.YOUTUBE_MUSIC -> rewriteYoutubeHost(youtubeLink, "music.youtube.com")
            MusicApp.YOUTUBE -> rewriteYoutubeHost(youtubeLink, "www.youtube.com")
            MusicApp.ANY -> {
                if (spotifyLink.isNotBlank() && isValidSpotifyLink(spotifyLink)) {
                    spotifyLink
                } else if (youtubeLink.isNotBlank() && isValidYoutubeLink(youtubeLink)) {
                    youtubeLink
                } else {
                    ""
                }
            }
        }

        val isValid = when {
            rawLink.isBlank() -> false
            preferredApp == MusicApp.SPOTIFY -> isValidSpotifyLink(rawLink)
            preferredApp == MusicApp.YOUTUBE_MUSIC || preferredApp == MusicApp.YOUTUBE -> isValidYoutubeLink(rawLink)
            else -> isValidSpotifyLink(rawLink) || isValidYoutubeLink(rawLink)
        }

        if (rawLink.isBlank() || !isValid) {
            Toast.makeText(context, "Add a playlist link in Settings > Workout music", Toast.LENGTH_SHORT).show()
            return false
        }

        val uri = try {
            Uri.parse(rawLink)
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't open the playlist", Toast.LENGTH_SHORT).show()
            return false
        }

        val preferredPackage = preferredApp.packageName
        if (preferredPackage != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage(preferredPackage)
                }
                context.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                // Retry once without package
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    return true
                } catch (fallbackEx: Exception) {
                    Toast.makeText(context, "Couldn't open the playlist", Toast.LENGTH_SHORT).show()
                    return false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't open the playlist", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            // ANY app: open directly
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't open the playlist", Toast.LENGTH_SHORT).show()
                return false
            }
        }
    }

    private fun rewriteYoutubeHost(link: String, targetHost: String): String {
        val trimmed = link.trim()
        if (trimmed.isBlank()) return ""
        return try {
            val parsed = Uri.parse(trimmed)
            val currentHost = parsed.host
            if (currentHost != null && (currentHost.contains("youtube.com") || currentHost == "youtu.be")) {
                if (currentHost == "youtu.be") {
                    // e.g. https://youtu.be/abc -> https://<targetHost>/watch?v=abc
                    val videoId = parsed.pathSegments.firstOrNull() ?: ""
                    "https://$targetHost/watch?v=$videoId"
                } else {
                    parsed.buildUpon().authority(targetHost).build().toString()
                }
            } else {
                trimmed
            }
        } catch (e: Exception) {
            trimmed
        }
    }
}
