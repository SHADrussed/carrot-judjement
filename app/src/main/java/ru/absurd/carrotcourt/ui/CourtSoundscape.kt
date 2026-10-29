package ru.absurd.carrotcourt.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.absurd.carrotcourt.R

/** Two original, bundled loops. Never plays in the background or over a focus-owning call. */
private class CourtSoundscape(context: Context) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val music = runCatching { MediaPlayer.create(context, R.raw.court_music) }.getOrNull()
    private val room = runCatching { MediaPlayer.create(context, R.raw.court_ambience) }.getOrNull()
    private var foreground = false
    private var musicEnabled = false
    private var roomEnabled = false
    private var focused = false
    private var ducked = false
    private var requested = false
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setOnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> { focused = true; ducked = false }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { focused = true; ducked = true }
                else -> { focused = false; ducked = false }
            }
            playOrPause()
        }.build()

    init { music?.isLooping = true; room?.isLooping = true }
    fun configure(musicOn: Boolean, roomOn: Boolean) {
        musicEnabled = musicOn
        roomEnabled = roomOn
        reconcile()
    }
    fun foreground(value: Boolean) { foreground = value; reconcile() }
    private fun reconcile() {
        if (foreground && (musicEnabled || roomEnabled)) {
            if (!requested) {
                focused = manager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                requested = focused
            }
        } else {
            if (requested) manager.abandonAudioFocusRequest(focusRequest)
            requested = false
            focused = false
        }
        playOrPause()
    }
    private fun playOrPause() {
        fun control(player: MediaPlayer?, enabled: Boolean, volume: Float) {
            runCatching {
                player ?: return
                val level = volume * if (ducked) .2f else 1f
                player.setVolume(level, level)
                if (foreground && focused && enabled) { if (!player.isPlaying) player.start() }
                else if (player.isPlaying) player.pause()
            }
        }
        control(music, musicEnabled, .28f)
        control(room, roomEnabled, .75f)
    }
    fun release() {
        foreground = false
        if (requested) manager.abandonAudioFocusRequest(focusRequest)
        music?.release()
        room?.release()
    }
}

@Composable
fun BackgroundAudio(music: Boolean, ambience: Boolean) {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    val soundscape = remember(context) { CourtSoundscape(context) }
    LaunchedEffect(music, ambience) { soundscape.configure(music, ambience) }
    DisposableEffect(owner, soundscape) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> soundscape.foreground(true)
                Lifecycle.Event.ON_PAUSE -> soundscape.foreground(false)
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        soundscape.foreground(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { owner.lifecycle.removeObserver(observer); soundscape.release() }
    }
}
