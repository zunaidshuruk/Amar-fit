package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlayingState(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val hasPermission: Boolean = false,
    val packageName: String = ""
)

class MusicNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        MusicNotificationManager.updateSessions(applicationContext)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MusicNotificationManager.clearSessions()
    }
}

object MusicNotificationManager {
    private val _nowPlaying = MutableStateFlow(NowPlayingState())
    val nowPlaying: StateFlow<NowPlayingState> = _nowPlaying.asStateFlow()

    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromController(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromController(activeController)
        }
    }

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    private var sessionManagerRegistered = false

    fun checkPermission(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val hasAccess = flat != null && flat.contains(packageName)
        _nowPlaying.value = _nowPlaying.value.copy(hasPermission = hasAccess)
        return hasAccess
    }

    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Notification Settings", Toast.LENGTH_SHORT).show()
        }
    }

    fun register(context: Context) {
        val hasAccess = checkPermission(context)
        if (!hasAccess) return

        try {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                ?: return
            val componentName = ComponentName(context, MusicNotificationListenerService::class.java)

            if (!sessionManagerRegistered) {
                mediaSessionManager.addOnActiveSessionsChangedListener(
                    sessionsChangedListener,
                    componentName
                )
                sessionManagerRegistered = true
            }

            val controllers = mediaSessionManager.getActiveSessions(componentName)
            updateActiveController(controllers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unregister(context: Context) {
        try {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            if (mediaSessionManager != null && sessionManagerRegistered) {
                mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
                sessionManagerRegistered = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    fun updateSessions(context: Context) {
        register(context)
    }

    fun clearSessions() {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        _nowPlaying.value = NowPlayingState(hasPermission = _nowPlaying.value.hasPermission)
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)

        if (controllers.isNullOrEmpty()) {
            activeController = null
            _nowPlaying.value = _nowPlaying.value.copy(
                title = "",
                artist = "",
                isPlaying = false,
                packageName = ""
            )
            return
        }

        val selected = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()

        activeController = selected
        selected?.registerCallback(controllerCallback)
        updateFromController(selected)
    }

    private fun updateFromController(controller: MediaController?) {
        if (controller == null) {
            _nowPlaying.value = _nowPlaying.value.copy(
                title = "",
                artist = "",
                isPlaying = false,
                packageName = ""
            )
            return
        }

        val metadata = controller.metadata
        val state = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: ""
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING

        _nowPlaying.value = _nowPlaying.value.copy(
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            packageName = controller.packageName ?: ""
        )
    }

    fun togglePlayPause(context: Context): Boolean {
        val controller = activeController
        if (controller != null) {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            return true
        }
        WorkoutMusic.sendMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        return false
    }

    fun play(context: Context): Boolean {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.play()
            return true
        }
        WorkoutMusic.sendMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
        return false
    }

    fun pause(context: Context): Boolean {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.pause()
            return true
        }
        WorkoutMusic.sendMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
        return false
    }

    fun skipToNext(context: Context): Boolean {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.skipToNext()
            return true
        }
        WorkoutMusic.sendMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
        return false
    }

    fun skipToPrevious(context: Context): Boolean {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.skipToPrevious()
            return true
        }
        WorkoutMusic.sendMediaKey(context, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        return false
    }
}
