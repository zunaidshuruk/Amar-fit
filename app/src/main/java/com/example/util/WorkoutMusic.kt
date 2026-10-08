package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.view.KeyEvent
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

object WorkoutMusic {
    enum class MusicApp(val label: String, val packageName: String?) {
        SPOTIFY("Spotify", "com.spotify.music"),
        YOUTUBE_MUSIC("YouTube Music", "com.google.android.apps.youtube.music"),
        YOUTUBE("YouTube", "com.google.android.youtube"),
        ANY("Any music app", null)
    }

    data class Playlist(val name: String, val link: String)

    private const val PREFS_NAME = "ShasthoPrefs"
    private const val KEY_MUSIC_APP = "music_app"
    private const val KEY_SPOTIFY_LINK = "music_spotify_link"
    private const val KEY_YOUTUBE_LINK = "music_youtube_link"
    private const val KEY_CONTROLS_ENABLED = "music_controls_enabled"
    private const val KEY_AUTO_START = "music_auto_start"
    private const val KEY_PLAYLISTS = "music_playlists"
    private const val KEY_ACTIVE_PLAYLIST = "music_active_playlist"

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

    fun getPlaylists(context: Context): List<Playlist> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_PLAYLISTS, null)
        if (jsonString.isNullOrBlank()) {
            val spotifyLink = getSpotifyLink(context).trim()
            val youtubeLink = getYoutubeLink(context).trim()
            val migrated = mutableListOf<Playlist>()
            if (spotifyLink.isNotBlank() && isValidSpotifyLink(spotifyLink)) {
                migrated.add(Playlist("Spotify playlist", spotifyLink))
            }
            if (youtubeLink.isNotBlank() && isValidYoutubeLink(youtubeLink)) {
                migrated.add(Playlist("YouTube playlist", youtubeLink))
            }
            if (migrated.isNotEmpty()) {
                savePlaylists(context, migrated)
                if (prefs.getString(KEY_ACTIVE_PLAYLIST, null).isNullOrBlank()) {
                    setActivePlaylist(context, migrated.first().name)
                }
                return migrated
            }
            return emptyList()
        }

        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Playlist>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.optString("name", "").trim()
                val link = obj.optString("link", "").trim()
                if (name.isNotBlank() && link.isNotBlank()) {
                    list.add(Playlist(name, link))
                }
            }
            if (list.isEmpty()) {
                val spotifyLink = getSpotifyLink(context).trim()
                val youtubeLink = getYoutubeLink(context).trim()
                val migrated = mutableListOf<Playlist>()
                if (spotifyLink.isNotBlank() && isValidSpotifyLink(spotifyLink)) {
                    migrated.add(Playlist("Spotify playlist", spotifyLink))
                }
                if (youtubeLink.isNotBlank() && isValidYoutubeLink(youtubeLink)) {
                    migrated.add(Playlist("YouTube playlist", youtubeLink))
                }
                if (migrated.isNotEmpty()) {
                    savePlaylists(context, migrated)
                    return migrated
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePlaylists(context: Context, list: List<Playlist>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("name", item.name)
            obj.put("link", item.link)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, jsonArray.toString()).apply()
    }

    fun addPlaylist(context: Context, name: String, link: String): String? {
        val trimmedName = name.trim()
        val trimmedLink = link.trim()

        if (trimmedName.length !in 1..30) {
            return "Name must be between 1 and 30 characters"
        }

        val existing = getPlaylists(context)
        if (existing.any { it.name.equals(trimmedName, ignoreCase = true) }) {
            return "A playlist with this name already exists"
        }

        if (!isValidSpotifyLink(trimmedLink) && !isValidYoutubeLink(trimmedLink)) {
            return "Link must be a valid Spotify or YouTube playlist link"
        }

        if (existing.size >= 12) {
            return "Maximum 12 playlists allowed"
        }

        val newList = existing + Playlist(trimmedName, trimmedLink)
        savePlaylists(context, newList)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeName = prefs.getString(KEY_ACTIVE_PLAYLIST, "") ?: ""
        if (activeName.isBlank() || existing.isEmpty()) {
            setActivePlaylist(context, trimmedName)
        }

        return null
    }

    fun removePlaylist(context: Context, name: String) {
        val trimmedName = name.trim()
        val existing = getPlaylists(context)
        val updated = existing.filterNot { it.name.equals(trimmedName, ignoreCase = true) }
        savePlaylists(context, updated)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeName = prefs.getString(KEY_ACTIVE_PLAYLIST, "") ?: ""
        if (activeName.equals(trimmedName, ignoreCase = true)) {
            if (updated.isNotEmpty()) {
                setActivePlaylist(context, updated.first().name)
            } else {
                setActivePlaylist(context, "")
            }
        }
    }

    fun getActivePlaylist(context: Context): Playlist? {
        val playlists = getPlaylists(context)
        if (playlists.isEmpty()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeName = prefs.getString(KEY_ACTIVE_PLAYLIST, "") ?: ""
        return playlists.find { it.name.equals(activeName, ignoreCase = true) } ?: playlists.first()
    }

    fun setActivePlaylist(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_PLAYLIST, name.trim()).apply()
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
        MusicNotificationManager.play(context)
    }

    fun togglePlayPause(context: Context) {
        MusicNotificationManager.togglePlayPause(context)
    }

    fun next(context: Context) {
        MusicNotificationManager.skipToNext(context)
    }

    fun previous(context: Context) {
        MusicNotificationManager.skipToPrevious(context)
    }

    fun openPlaylist(context: Context): Boolean {
        val active = getActivePlaylist(context)
        if (active == null) {
            Toast.makeText(context, "Add a playlist link in Settings > Workout music", Toast.LENGTH_SHORT).show()
            return false
        }
        return openPlaylist(context, active)
    }

    fun openPlaylist(context: Context, playlist: Playlist): Boolean {
        val rawLink = playlist.link.trim()
        if (rawLink.isBlank()) {
            Toast.makeText(context, "Add a playlist link in Settings > Workout music", Toast.LENGTH_SHORT).show()
            return false
        }

        val isSpotify = isValidSpotifyLink(rawLink)
        val isYoutube = isValidYoutubeLink(rawLink)

        if (!isSpotify && !isYoutube) {
            Toast.makeText(context, "Couldn't open the playlist", Toast.LENGTH_SHORT).show()
            return false
        }

        val uriString: String
        val targetPackage: String?

        if (isSpotify) {
            uriString = rawLink
            targetPackage = MusicApp.SPOTIFY.packageName
        } else {
            val preferredApp = getMusicApp(context)
            val targetHost = if (preferredApp == MusicApp.YOUTUBE) "www.youtube.com" else "music.youtube.com"
            val targetPkg = if (preferredApp == MusicApp.YOUTUBE) MusicApp.YOUTUBE.packageName else MusicApp.YOUTUBE_MUSIC.packageName
            uriString = rewriteYoutubeHost(rawLink, targetHost)
            targetPackage = targetPkg
        }

        val uri = try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            Toast.makeText(context, "Couldn't open the playlist", Toast.LENGTH_SHORT).show()
            return false
        }

        if (targetPackage != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage(targetPackage)
                }
                context.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
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
