package com.example.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance real-time audio synthesizer for ambient soundscapes,
 * binaural brainwave entrainment, harmonic Solfeggio frequencies, and Tibetan singing bowl.
 * Includes smooth crossfade transitions between soundscape presets to prevent clicks and pops.
 */
class FlowAudioSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Master Soundscape Profile & Crossfade State
    @Volatile var activeProfile: AmbientSoundProfile = AmbientSoundProfile.NONE
    @Volatile private var outgoingProfile: AmbientSoundProfile = AmbientSoundProfile.NONE
    @Volatile private var incomingProfile: AmbientSoundProfile = AmbientSoundProfile.NONE
    @Volatile private var crossfadeProgress = 1.0f // 0.0f (outgoing) -> 1.0f (incoming)
    @Volatile private var isCrossfading = false

    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning = _isTransitioning.asStateFlow()

    private val _transitioningTo = MutableStateFlow<AmbientSoundProfile?>(null)
    val transitioningTo = _transitioningTo.asStateFlow()

    @Volatile var masterVolume = 0.7f

    // Layer Sliders (0.0f to 1.0f) for Multi-Layer Mixing
    @Volatile var binauralBeatVolume = 0.4f     // Brainwave entrainment (40Hz Gamma / 10Hz Alpha / 6Hz Theta)
    @Volatile var natureAtmosphereVolume = 0.5f // Wind, Rain, Stream
    @Volatile var noiseTextureVolume = 0.4f     // Brown / Pink Noise
    @Volatile var singingBowlVolume = 0.6f      // Singing Bowl Resonance

    // Selected Binaural Mode
    @Volatile var binauralBeatFreq = 40.0 // 40Hz Gamma default, 10Hz Alpha, 6Hz Theta
    @Volatile var carrierFreq = 180.0     // Base carrier frequency in Hz

    // Solfeggio / Harmonic resonance tone
    @Volatile var solfeggioFreq = 528.0

    // Singing bowl envelope state
    @Volatile private var bowlEnvelope = 0.0f
    private val bowlDecay = 0.99995f

    // Filter states for natural sound synthesis
    private var lastBrownLeft = 0.0f
    private var lastBrownRight = 0.0f
    private var pinkB0 = 0.0f
    private var pinkB1 = 0.0f
    private var pinkB2 = 0.0f
    private var cafeResonanceTimer = 0
    private var streamModulation = 0.0

    // Crossfade configuration: 1.5 seconds fade duration
    private val crossfadeStepPerSample = 1.0f / (44100.0f * 1.5f)

    fun transitionToPreset(newProfile: AmbientSoundProfile) {
        if (newProfile == activeProfile && !isCrossfading) return

        if (activeProfile == AmbientSoundProfile.NONE) {
            // Instant start if coming from silence
            activeProfile = newProfile
            incomingProfile = newProfile
            outgoingProfile = AmbientSoundProfile.NONE
            crossfadeProgress = 1.0f
            isCrossfading = false
            _isTransitioning.value = false
            _transitioningTo.value = null
            start()
            return
        }

        outgoingProfile = if (isCrossfading) incomingProfile else activeProfile
        incomingProfile = newProfile
        activeProfile = newProfile
        crossfadeProgress = 0.0f
        isCrossfading = true
        _isTransitioning.value = true
        _transitioningTo.value = newProfile

        if (audioTrack == null) {
            start()
        }
    }

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

                val bufferSize = 2048 // stereo pair samples: L, R
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

                                // 1. Base Preset Generation with Smooth Crossfade
                                if (isCrossfading) {
                                    crossfadeProgress += crossfadeStepPerSample
                                    if (crossfadeProgress >= 1.0f) {
                                        crossfadeProgress = 1.0f
                                        isCrossfading = false
                                        _isTransitioning.value = false
                                        _transitioningTo.value = null
                                    }

                                    // Equal-power crossfade curve for smooth perceived volume
                                    val inWeight = sin(crossfadeProgress * Math.PI.toFloat() * 0.5f)
                                    val outWeight = cos(crossfadeProgress * Math.PI.toFloat() * 0.5f)

                                    if (outgoingProfile != AmbientSoundProfile.NONE) {
                                        val (outL, outR) = generatePresetSamples(outgoingProfile, t)
                                        ambLeft += outL * outWeight
                                        ambRight += outR * outWeight
                                    }

                                    if (incomingProfile != AmbientSoundProfile.NONE) {
                                        val (inL, inR) = generatePresetSamples(incomingProfile, t)
                                        ambLeft += inL * inWeight
                                        ambRight += inR * inWeight
                                    }
                                } else {
                                    if (activeProfile != AmbientSoundProfile.NONE) {
                                        val (pL, pR) = generatePresetSamples(activeProfile, t)
                                        ambLeft += pL
                                        ambRight += pR
                                    }
                                }

                                // 2. Extra Layering: Discrete Binaural Beats Underlay
                                if (binauralBeatVolume > 0.02f && activeProfile !in listOf(AmbientSoundProfile.GAMMA_40HZ, AmbientSoundProfile.ALPHA_10HZ, AmbientSoundProfile.THETA_6HZ)) {
                                    val fLeft = carrierFreq
                                    val fRight = carrierFreq + binauralBeatFreq
                                    ambLeft += (sin(2.0 * Math.PI * fLeft * t) * 0.2 * binauralBeatVolume.toDouble()).toFloat()
                                    ambRight += (sin(2.0 * Math.PI * fRight * t) * 0.2 * binauralBeatVolume.toDouble()).toFloat()
                                }

                                // 3. Extra Layering: Discrete Rain/Brown Noise Filter Underlay
                                if (noiseTextureVolume > 0.02f && activeProfile !in listOf(AmbientSoundProfile.BROWN_NOISE, AmbientSoundProfile.PINK_NOISE, AmbientSoundProfile.WHITE_NOISE)) {
                                    val rL = Random.nextFloat() * 2f - 1f
                                    val rR = Random.nextFloat() * 2f - 1f
                                    lastBrownLeft = (lastBrownLeft + (0.02f * rL)) / 1.02f
                                    lastBrownRight = (lastBrownRight + (0.02f * rR)) / 1.02f
                                    ambLeft += lastBrownLeft * 0.45f * noiseTextureVolume
                                    ambRight += lastBrownRight * 0.45f * noiseTextureVolume
                                }

                                // 4. Tibetan Singing Bowl (Physical Acoustic Harmonics)
                                var bowlLeft = 0.0f
                                var bowlRight = 0.0f
                                if (bowlEnvelope > 0.001f) {
                                    val b1 = (sin(2.0 * Math.PI * 216.0 * t) * 0.4).toFloat()
                                    val b2 = (sin(2.0 * Math.PI * 324.0 * t) * 0.3).toFloat()
                                    val b3 = (sin(2.0 * Math.PI * 432.0 * t) * 0.2).toFloat()
                                    val b4 = (sin(2.0 * Math.PI * 540.0 * t) * 0.1).toFloat()

                                    val lfo = sin(2.0 * Math.PI * 0.25 * t).toFloat()
                                    val modulatedBowl = (b1 + b2 + b3 + b4) * bowlEnvelope * singingBowlVolume

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

    private fun generatePresetSamples(profile: AmbientSoundProfile, t: Double): Pair<Float, Float> {
        var left = 0.0f
        var right = 0.0f

        when (profile) {
            AmbientSoundProfile.BROWN_NOISE,
            AmbientSoundProfile.PINK_NOISE -> {
                val whiteL = Random.nextFloat() * 2f - 1f
                val whiteR = Random.nextFloat() * 2f - 1f
                lastBrownLeft = (lastBrownLeft + (0.02f * whiteL)) / 1.02f
                lastBrownRight = (lastBrownRight + (0.02f * whiteR)) / 1.02f

                pinkB0 = 0.99765f * pinkB0 + whiteL * 0.0990460f
                pinkB1 = 0.96300f * pinkB1 + whiteL * 0.2965164f
                pinkB2 = 0.57000f * pinkB2 + whiteL * 1.0526913f
                val pinkL = (pinkB0 + pinkB1 + pinkB2 + whiteL * 0.1848f) * 0.15f

                val chosenNoiseL = if (profile == AmbientSoundProfile.BROWN_NOISE) lastBrownLeft * 0.7f else pinkL
                val chosenNoiseR = if (profile == AmbientSoundProfile.BROWN_NOISE) lastBrownRight * 0.7f else (lastBrownRight * 0.4f + pinkL * 0.5f)

                left = chosenNoiseL * masterVolume
                right = chosenNoiseR * masterVolume
            }
            AmbientSoundProfile.WHITE_NOISE -> {
                left = (Random.nextFloat() * 2f - 1f) * 0.18f * masterVolume
                right = (Random.nextFloat() * 2f - 1f) * 0.18f * masterVolume
            }
            AmbientSoundProfile.GAMMA_40HZ -> {
                val fLeft = carrierFreq
                val fRight = carrierFreq + 40.0
                left = (sin(2.0 * Math.PI * fLeft * t) * 0.25 * masterVolume.toDouble()).toFloat()
                right = (sin(2.0 * Math.PI * fRight * t) * 0.25 * masterVolume.toDouble()).toFloat()
            }
            AmbientSoundProfile.ALPHA_10HZ -> {
                val fLeft = 140.0
                val fRight = 150.0
                left = (sin(2.0 * Math.PI * fLeft * t) * 0.25 * masterVolume.toDouble()).toFloat()
                right = (sin(2.0 * Math.PI * fRight * t) * 0.25 * masterVolume.toDouble()).toFloat()
            }
            AmbientSoundProfile.THETA_6HZ -> {
                val fLeft = 110.0
                val fRight = 116.0
                left = (sin(2.0 * Math.PI * fLeft * t) * 0.25 * masterVolume.toDouble()).toFloat()
                right = (sin(2.0 * Math.PI * fRight * t) * 0.25 * masterVolume.toDouble()).toFloat()
            }
            AmbientSoundProfile.SOLFEGGIO_528HZ -> {
                val pure528 = sin(2.0 * Math.PI * 528.0 * t) * 0.2
                val sub264 = sin(2.0 * Math.PI * 264.0 * t) * 0.1
                val warm = (pure528 + sub264).toFloat() * masterVolume
                left = warm
                right = warm
            }
            AmbientSoundProfile.SOLFEGGIO_432HZ -> {
                val pure432 = sin(2.0 * Math.PI * 432.0 * t) * 0.22
                val sub216 = sin(2.0 * Math.PI * 216.0 * t) * 0.08
                val warm = (pure432 + sub216).toFloat() * masterVolume
                left = warm
                right = warm
            }
            AmbientSoundProfile.CAFE_ATMOSPHERE -> {
                val rL = Random.nextFloat() * 2f - 1f
                lastBrownLeft = (lastBrownLeft + (0.015f * rL)) / 1.015f
                val roomHum = (sin(2.0 * Math.PI * 120.0 * t) * 0.04).toFloat()
                var clink = 0.0f
                if (Random.nextInt(18000) == 1) {
                    cafeResonanceTimer = 900
                }
                if (cafeResonanceTimer > 0) {
                    clink = (sin(2.0 * Math.PI * 2600.0 * t) * 0.07 * (cafeResonanceTimer / 900.0)).toFloat()
                    cafeResonanceTimer--
                }
                left = (lastBrownLeft * 0.35f + roomHum + clink) * masterVolume
                right = (lastBrownLeft * 0.35f + roomHum - clink * 0.5f) * masterVolume
            }
            AmbientSoundProfile.FOREST_STREAM -> {
                val rL = Random.nextFloat() * 2f - 1f
                val rR = Random.nextFloat() * 2f - 1f
                lastBrownLeft = (lastBrownLeft + (0.025f * rL)) / 1.025f
                lastBrownRight = (lastBrownRight + (0.025f * rR)) / 1.025f

                streamModulation = sin(2.0 * Math.PI * 0.15 * t) * 0.12
                val streamL = (lastBrownLeft * 0.5f * (0.85 + streamModulation)).toFloat()
                val streamR = (lastBrownRight * 0.5f * (0.85 - streamModulation)).toFloat()

                left = streamL * masterVolume
                right = streamR * masterVolume
            }
            AmbientSoundProfile.NIGHT_CRICKETS -> {
                val rL = Random.nextFloat() * 2f - 1f
                lastBrownLeft = (lastBrownLeft + (0.01f * rL)) / 1.01f

                val cricketPulse = (sin(2.0 * Math.PI * 4.5 * t).coerceAtLeast(0.0) * sin(2.0 * Math.PI * 4500.0 * t) * 0.04).toFloat()
                left = (lastBrownLeft * 0.25f + cricketPulse) * masterVolume
                right = (lastBrownLeft * 0.25f + (cricketPulse * 0.8f)) * masterVolume
            }
            AmbientSoundProfile.NONE -> {
                left = 0.0f
                right = 0.0f
            }
        }

        return Pair(left, right)
    }

    fun strikeSingingBowl() {
        bowlEnvelope = 1.0f
        if (audioTrack == null) {
            start()
        }
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
