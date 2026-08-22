package com.example.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object FlowHapticManager {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Subtle tactile cue when starting a focus session (Threshold enter / Timer start).
     * Two gentle ramping pulses that ground the user into deep presence.
     */
    fun performSessionStartHaptic(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Gentle double pulse pattern: 30ms soft, 80ms pause, 60ms medium
                val timings = longArrayOf(0, 35, 90, 65)
                val amplitudes = intArrayOf(0, 80, 0, 160)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 40, 80, 60), -1)
            }
        } catch (e: Exception) {
            // Silently ignore if haptics unavailable
        }
    }

    /**
     * Resonant tactile cue when a session successfully concludes.
     * Harmonic three-pulse vibration pattern matching the finishing singing bowl chime.
     */
    fun performSessionCompleteHaptic(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Harmonious chime pattern: soft, medium, firm
                val timings = longArrayOf(0, 50, 100, 70, 120, 110)
                val amplitudes = intArrayOf(0, 90, 0, 160, 0, 240)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 100, 70, 120, 110), -1)
            }
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    /**
     * Subtle micro-tick on breathing phase shifts (Inhale, Hold, Exhale)
     */
    fun performBreathPhaseHaptic(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, 60))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    /**
     * Tactile feedback on singing bowl strike
     */
    fun performBowlStrikeHaptic(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 40, 40)
                val amplitudes = intArrayOf(0, 200, 0, 100)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    /**
     * Light click on interactive controls
     */
    fun performClickHaptic(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 70))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (e: Exception) {
            // Silently ignore
        }
    }
}
