package com.example.data.archiveaudio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InternetArchiveAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _activeItem = MutableStateFlow<ArchiveAudioItem?>(null)
    val activeItem: StateFlow<ArchiveAudioItem?> = _activeItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _volume = MutableStateFlow(0.6f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    fun playArchiveItem(item: ArchiveAudioItem) {
        if (_activeItem.value?.id == item.id && mediaPlayer != null) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        stopAndRelease()
        _activeItem.value = item
        _isBuffering.value = true

        scope.launch(Dispatchers.IO) {
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(item.streamUrl)
                    isLooping = true

                    setOnPreparedListener { player ->
                        _isBuffering.value = false
                        player.setVolume(_volume.value, _volume.value)
                        player.start()
                        _isPlaying.value = true
                    }

                    setOnBufferingUpdateListener { _, percent ->
                        if (percent < 100 && !_isPlaying.value) {
                            _isBuffering.value = true
                        } else if (percent > 20) {
                            _isBuffering.value = false
                        }
                    }

                    setOnErrorListener { _, what, extra ->
                        Log.e("ArchiveAudioPlayer", "MediaPlayer error: what=$what, extra=$extra")
                        _isBuffering.value = false
                        _isPlaying.value = false
                        true
                    }
                }

                mediaPlayer = mp
                mp.prepareAsync()
            } catch (e: Exception) {
                Log.e("ArchiveAudioPlayer", "Error initializing streaming MediaPlayer", e)
                _isBuffering.value = false
                _isPlaying.value = false
            }
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        mediaPlayer?.let { player ->
            try {
                player.setVolume(clamped, clamped)
            } catch (e: Exception) {
                Log.e("ArchiveAudioPlayer", "Error setting volume", e)
            }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e("ArchiveAudioPlayer", "Error pausing player", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    _isPlaying.value = true
                }
            }
        } catch (e: Exception) {
            Log.e("ArchiveAudioPlayer", "Error resuming player", e)
        }
    }

    fun stopAndRelease() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("ArchiveAudioPlayer", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _isBuffering.value = false
        }
    }
}
