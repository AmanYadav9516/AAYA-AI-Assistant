package com.aaya.assistant.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aaya.assistant.data.model.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AayaMusicPlayer private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AayaMusicPlayer"

        @Volatile
        private var instance: AayaMusicPlayer? = null

        fun getInstance(context: Context): AayaMusicPlayer {
            return instance ?: synchronized(this) {
                instance ?: AayaMusicPlayer(context.applicationContext).also { instance = it }
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mixerEngine = AudioMixerEngine.getInstance(context)

    private val _currentSong = MutableStateFlow<SongItem?>(null)
    val currentSong: StateFlow<SongItem?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressMs = MutableStateFlow(0)
    val progressMs: StateFlow<Int> = _progressMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val playlist = mutableListOf<SongItem>()
    private var currentIndex = -1

    private val progressUpdater = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        _progressMs.value = player.currentPosition
                        _durationMs.value = player.duration.coerceAtLeast(1)
                    }
                } catch (_: Exception) {}
            }
            if (_isPlaying.value) {
                mainHandler.postDelayed(this, 500)
            }
        }
    }

    fun setPlaylist(songs: List<SongItem>) {
        playlist.clear()
        playlist.addAll(songs)
    }

    fun playSong(song: SongItem) {
        val index = playlist.indexOfFirst { it.id == song.id || it.audioUrl == song.audioUrl }
        if (index != -1) {
            currentIndex = index
        }
        _currentSong.value = song

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(song.audioUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _durationMs.value = mp.duration.coerceAtLeast(1)
                    _progressMs.value = 0
                    mainHandler.post(progressUpdater)

                    // Attach Equalizer & Audio Effects to MediaPlayer session!
                    mixerEngine.attachToAudioSession(mp.audioSessionId)
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    playNext()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start song playback", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: run {
            _currentSong.value?.let { playSong(it) }
            return
        }
        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.start()
                _isPlaying.value = true
                mainHandler.post(progressUpdater)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Toggle play/pause failed", e)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _progressMs.value = positionMs
        } catch (_: Exception) {}
    }

    fun playNext() {
        if (playlist.isEmpty()) return
        currentIndex = (currentIndex + 1) % playlist.size
        playSong(playlist[currentIndex])
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
        playSong(playlist[currentIndex])
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _progressMs.value = 0
    }
}
