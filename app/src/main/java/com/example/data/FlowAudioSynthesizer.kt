package com.example.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundProfile(val displayName: String, val emoji: String) {
    NONE("Silent", "🔇"),
    RAINFALL("Rainfall", "🌧️"),
    WHITE_NOISE("White Noise", "📻"),
    CAFE_ATMOSPHERE("Cafe Atmosphere", "☕"),
    GAMMA_40HZ("40Hz Gamma", "🧠")
}

class FlowAudioSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Current ambient profile
    @Volatile var activeProfile: AmbientSoundProfile = AmbientSoundProfile.NONE
    @Volatile var masterVolume = 0.5f

    // Layer volumes (0.0f to 1.0f)
    @Volatile var gammaVolume = 0.0f
    @Volatile var rainVolume = 0.0f
    @Volatile var bowlVolume = 0.0f

    // Singing bowl strike envelope
    @Volatile private var bowlEnvelope = 0.0f
    private val bowlDecay = 0.99996f // Very slow decay

    // Filter states
    private var lastBrownLeft = 0.0f
    private var lastBrownRight = 0.0f
    private var cafeResonanceTimer = 0

    fun start() {
        if (audioTrack != null) return

        try {
            synthJob = scope.launch(Dispatchers.IO) {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val bufferSize = 2048 // samples (stereo pair: L, R)
                val buffer = ShortArray(bufferSize)
                var sampleIndex = 0L

                while (isActive) {
                    val track = audioTrack
                    if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        try {
                            for (i in 0 until bufferSize step 2) {
                                val t = sampleIndex / 44100.0

                                var ambLeft = 0.0f
                                var ambRight = 0.0f

                                when (activeProfile) {
                                    AmbientSoundProfile.RAINFALL -> {
                                        // Brown noise filtered rain
                                        val rL = Random.nextFloat() * 2f - 1f
                                        lastBrownLeft = (lastBrownLeft + (0.02f * rL)) / 1.02f
                                        val rR = Random.nextFloat() * 2f - 1f
                                        lastBrownRight = (lastBrownRight + (0.02f * rR)) / 1.02f
                                        ambLeft = lastBrownLeft * 0.6f * masterVolume
                                        ambRight = lastBrownRight * 0.6f * masterVolume
                                    }
                                    AmbientSoundProfile.WHITE_NOISE -> {
                                        // Gentle white noise
                                        ambLeft = (Random.nextFloat() * 2f - 1f) * 0.15f * masterVolume
                                        ambRight = (Random.nextFloat() * 2f - 1f) * 0.15f * masterVolume
                                    }
                                    AmbientSoundProfile.CAFE_ATMOSPHERE -> {
                                        // Warm ambient room noise + subtle coffee shop harmonics
                                        val rL = Random.nextFloat() * 2f - 1f
                                        lastBrownLeft = (lastBrownLeft + (0.01f * rL)) / 1.01f
                                        val roomHum = (sin(2.0 * Math.PI * 120.0 * t) * 0.05).toFloat()
                                        var clink = 0.0f
                                        if (Random.nextInt(20000) == 1) {
                                            cafeResonanceTimer = 800
                                        }
                                        if (cafeResonanceTimer > 0) {
                                            clink = (sin(2.0 * Math.PI * 2400.0 * t) * 0.08 * (cafeResonanceTimer / 800.0)).toFloat()
                                            cafeResonanceTimer--
                                        }
                                        ambLeft = (lastBrownLeft * 0.3f + roomHum + clink) * masterVolume
                                        ambRight = (lastBrownLeft * 0.3f + roomHum - clink * 0.5f) * masterVolume
                                    }
                                    AmbientSoundProfile.GAMMA_40HZ -> {
                                        // Binaural 40Hz beat
                                        ambLeft = (sin(2.0 * Math.PI * 150.0 * t) * 0.25 * masterVolume.toDouble()).toFloat()
                                        ambRight = (sin(2.0 * Math.PI * 190.0 * t) * 0.25 * masterVolume.toDouble()).toFloat()
                                    }
                                    AmbientSoundProfile.NONE -> {
                                        ambLeft = 0.0f
                                        ambRight = 0.0f
                                    }
                                }

                                // Additional manual layer support
                                if (gammaVolume > 0.01f && activeProfile != AmbientSoundProfile.GAMMA_40HZ) {
                                    ambLeft += (sin(2.0 * Math.PI * 150.0 * t) * 0.2 * gammaVolume.toDouble()).toFloat()
                                    ambRight += (sin(2.0 * Math.PI * 190.0 * t) * 0.2 * gammaVolume.toDouble()).toFloat()
                                }
                                if (rainVolume > 0.01f && activeProfile != AmbientSoundProfile.RAINFALL) {
                                    val rL = Random.nextFloat() * 2f - 1f
                                    lastBrownLeft = (lastBrownLeft + (0.02f * rL)) / 1.02f
                                    ambLeft += lastBrownLeft * 0.4f * rainVolume
                                    ambRight += lastBrownLeft * 0.4f * rainVolume
                                }

                                // Tibetan Singing Bowl (Resonance)
                                var bowlLeft = 0.0f
                                var bowlRight = 0.0f
                                if (bowlEnvelope > 0.001f) {
                                    val b1 = (sin(2.0 * Math.PI * 220.0 * t) * 0.4).toFloat()
                                    val b2 = (sin(2.0 * Math.PI * 330.0 * t) * 0.3).toFloat()
                                    val b3 = (sin(2.0 * Math.PI * 440.0 * t) * 0.2).toFloat()
                                    val b4 = (sin(2.0 * Math.PI * 550.0 * t) * 0.1).toFloat()

                                    val lfo = sin(2.0 * Math.PI * 0.2 * t).toFloat()
                                    val modulatedBowl = (b1 + b2 + b3 + b4) * bowlEnvelope * bowlVolume

                                    bowlLeft = modulatedBowl * (0.5f + 0.3f * lfo)
                                    bowlRight = modulatedBowl * (0.5f - 0.3f * lfo)

                                    bowlEnvelope *= bowlDecay
                                } else {
                                    bowlEnvelope = 0.0f
                                }

                                val outLeft = (ambLeft + bowlLeft).coerceIn(-1.0f, 1.0f)
                                val outRight = (ambRight + bowlRight).coerceIn(-1.0f, 1.0f)

                                buffer[i] = (outLeft * 32767).toInt().toShort()
                                buffer[i + 1] = (outRight * 32767).toInt().toShort()

                                sampleIndex++
                            }

                            if (isActive && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                track.write(buffer, 0, bufferSize)
                            }
                        } catch (e: Exception) {
                            Log.e("FlowAudioSynthesizer", "Error in synth loop", e)
                        }
                    } else {
                        kotlinx.coroutines.delay(20)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FlowAudioSynthesizer", "Error starting synthesizer", e)
        }
    }

    fun strikeSingingBowl() {
        bowlEnvelope = 1.0f
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e("FlowAudioSynthesizer", "Error stopping AudioTrack", e)
        }
        audioTrack = null
    }
}

