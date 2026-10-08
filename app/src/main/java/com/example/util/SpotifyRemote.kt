package com.example.util

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.AuthenticationFailedException
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp
import com.spotify.android.appremote.api.error.NotLoggedInException
import com.spotify.android.appremote.api.error.SpotifyDisconnectedException
import com.spotify.android.appremote.api.error.UnsupportedFeatureVersionException
import com.spotify.android.appremote.api.error.UserNotAuthorizedException
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpotifyTrackState(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val isConnected: Boolean = false,
    val lastError: String? = null
)

object SpotifyRemote {
    private const val TAG = "SpotifyRemote"
    private const val PREFS_NAME = "ShasthoPrefs"
    private const val KEY_SPOTIFY_REMOTE_ENABLED = "spotify_remote_enabled"

    private val _state = MutableStateFlow(SpotifyTrackState())
    val state: StateFlow<SpotifyTrackState> = _state.asStateFlow()

    private var appRemote: SpotifyAppRemote? = null
    private var playerStateSubscription: Subscription<PlayerState>? = null
    private var isConnecting = false

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SPOTIFY_REMOTE_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SPOTIFY_REMOTE_ENABLED, enabled).apply()
        if (!enabled) {
            disconnect()
        }
    }

    fun getClientId(): String {
        return try {
            val key = BuildConfig::class.java.getField("SPOTIFY_CLIENT_ID").get(null) as? String ?: ""
            if (key.isBlank() || key.startsWith("MY_")) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    fun isConfigured(): Boolean = getClientId().isNotBlank()

    fun isConnected(): Boolean = appRemote?.isConnected == true

    fun connect(context: Context, onResult: ((Boolean, String?) -> Unit)? = null) {
        if (!isEnabled(context)) {
            onResult?.invoke(false, "Spotify integration is disabled in Settings")
            return
        }

        val clientId = getClientId()
        if (clientId.isBlank()) {
            val msg = "Spotify Client ID is not configured"
            _state.value = _state.value.copy(lastError = msg)
            onResult?.invoke(false, msg)
            return
        }

        if (isConnected()) {
            onResult?.invoke(true, null)
            return
        }

        if (isConnecting) return
        isConnecting = true

        val redirectUri = "${context.packageName}://spotify-callback"
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(
            context.applicationContext,
            connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(remote: SpotifyAppRemote) {
                    isConnecting = false
                    appRemote = remote
                    _state.value = _state.value.copy(
                        isConnected = true,
                        lastError = null
                    )
                    subscribeToPlayerState()
                    onResult?.invoke(true, null)
                }

                override fun onFailure(throwable: Throwable) {
                    isConnecting = false
                    appRemote = null
                    val friendlyMsg = mapThrowableToMessage(throwable)
                    Log.w(TAG, "Spotify connection failure: $friendlyMsg", throwable)
                    _state.value = _state.value.copy(
                        isConnected = false,
                        lastError = friendlyMsg
                    )
                    onResult?.invoke(false, friendlyMsg)
                }
            }
        )
    }

    fun disconnect() {
        isConnecting = false
        try {
            playerStateSubscription?.cancel()
        } catch (e: Exception) {
            // ignore
        }
        playerStateSubscription = null

        val remote = appRemote
        if (remote != null) {
            try {
                SpotifyAppRemote.disconnect(remote)
            } catch (e: Exception) {
                // ignore
            }
            appRemote = null
        }

        _state.value = SpotifyTrackState(
            title = "",
            artist = "",
            isPlaying = false,
            isConnected = false,
            lastError = null
        )
    }

    private fun subscribeToPlayerState() {
        val remote = appRemote ?: return
        try {
            playerStateSubscription?.cancel()
            playerStateSubscription = remote.playerApi.subscribeToPlayerState().apply {
                setEventCallback { playerState ->
                    val track = playerState.track
                    val title = track?.name ?: ""
                    val artist = track?.artist?.name ?: ""
                    val isPlaying = !playerState.isPaused
                    _state.value = _state.value.copy(
                        title = title,
                        artist = artist,
                        isPlaying = isPlaying,
                        isConnected = true
                    )
                }
                setErrorCallback { throwable ->
                    val msg = mapThrowableToMessage(throwable)
                    Log.w(TAG, "PlayerState error: $msg", throwable)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to subscribe to PlayerState", e)
        }
    }

    fun play(uri: String? = null): Boolean {
        val remote = appRemote ?: return false
        return try {
            if (!uri.isNullOrBlank()) {
                remote.playerApi.play(uri)
            } else {
                remote.playerApi.resume()
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play: ${e.message}")
            false
        }
    }

    fun pause(): Boolean {
        val remote = appRemote ?: return false
        return try {
            remote.playerApi.pause()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pause: ${e.message}")
            false
        }
    }

    fun togglePlayPause(): Boolean {
        val remote = appRemote ?: return false
        return try {
            if (_state.value.isPlaying) {
                remote.playerApi.pause()
            } else {
                remote.playerApi.resume()
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle play/pause: ${e.message}")
            false
        }
    }

    fun skipNext(): Boolean {
        val remote = appRemote ?: return false
        return try {
            remote.playerApi.skipNext()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to skipNext: ${e.message}")
            false
        }
    }

    fun skipPrevious(): Boolean {
        val remote = appRemote ?: return false
        return try {
            remote.playerApi.skipPrevious()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to skipPrevious: ${e.message}")
            false
        }
    }

    fun extractSpotifyUri(rawLink: String): String? {
        val trimmed = rawLink.trim()
        if (trimmed.startsWith("spotify:")) return trimmed

        val openPrefix = "https://open.spotify.com/"
        if (trimmed.startsWith(openPrefix)) {
            val pathWithQuery = trimmed.removePrefix(openPrefix)
            val path = pathWithQuery.substringBefore("?").trim('/')
            val parts = path.split("/")
            if (parts.size >= 2) {
                val type = parts[0]
                val id = parts[1]
                if (id.isNotBlank()) {
                    return "spotify:$type:$id"
                }
            }
        }
        return null
    }

    private fun mapThrowableToMessage(throwable: Throwable): String {
        return when (throwable) {
            is CouldNotFindSpotifyApp -> "Install the Spotify app"
            is NotLoggedInException -> "Log in to Spotify first"
            is UserNotAuthorizedException, is AuthenticationFailedException -> "Spotify did not allow KardIQ"
            is UnsupportedFeatureVersionException -> "Spotify app version not supported"
            is SpotifyDisconnectedException -> "Disconnected from Spotify"
            else -> throwable.message ?: "Spotify connection failed"
        }
    }
}
