package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

enum class FlowTimerPalette(
    val title: String,
    val description: String,
    val baseColor: Color,
    val midColor: Color,
    val peakColor: Color,
    val darkBackground: Color
) {
    SOLAR_AMBER(
        title = "Solar Amber",
        description = "Warm golden fire that ignites into radiant crimson energy",
        baseColor = Color(0xFFE0A96D),
        midColor = Color(0xFFF4A261),
        peakColor = Color(0xFFE76F51),
        darkBackground = Color(0xFF14100C)
    ),
    EMERALD_ZEN(
        title = "Emerald Zen",
        description = "Deep forest tranquility awakening into vivid luminous jade",
        baseColor = Color(0xFF52B788),
        midColor = Color(0xFF2EC4B6),
        peakColor = Color(0xFF70E000),
        darkBackground = Color(0xFF0C1410)
    ),
    OCEAN_AZURE(
        title = "Abyssal Azure",
        description = "Still ocean depths rising into electric glacial cyan",
        baseColor = Color(0xFF48CAE4),
        midColor = Color(0xFF0096C7),
        peakColor = Color(0xFF90E0EF),
        darkBackground = Color(0xFF0A1218)
    ),
    AMETHYST_AURA(
        title = "Amethyst Aura",
        description = "Mystic twilight violet shifting into vibrant cosmic magenta",
        baseColor = Color(0xFFB5838D),
        midColor = Color(0xFF9D4EDD),
        peakColor = Color(0xFFE0AAFF),
        darkBackground = Color(0xFF140D18)
    ),
    CRIMSON_FLAME(
        title = "Crimson Flame",
        description = "Intense passionate focus shifting into warm ember glow",
        baseColor = Color(0xFFE63946),
        midColor = Color(0xFFFF5964),
        peakColor = Color(0xFFFFB703),
        darkBackground = Color(0xFF180A0B)
    ),
    MONOCHROME_INK(
        title = "Zen Ink",
        description = "Minimalist charcoal and pure platinum starlight",
        baseColor = Color(0xFFCCCCCC),
        midColor = Color(0xFFEAEAEA),
        peakColor = Color(0xFFFFFFFF),
        darkBackground = Color(0xFF111111)
    )
}

object FlowIntensityColorEngine {

    /**
     * Calculates the dynamic color for the focus timer based on the chosen palette
     * and current focus intensity (0.0 = initial calm, 1.0 = deep peak intensity).
     */
    fun calculateIntensityColor(palette: FlowTimerPalette, intensity: Float): Color {
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        return if (clampedIntensity <= 0.5f) {
            val fraction = clampedIntensity / 0.5f
            lerp(palette.baseColor, palette.midColor, fraction)
        } else {
            val fraction = (clampedIntensity - 0.5f) / 0.5f
            lerp(palette.midColor, palette.peakColor, fraction)
        }
    }

    /**
     * Returns a 3-stop color list for drawing the circular gradient progress arc,
     * morphing smoothly according to intensity.
     */
    fun getProgressArcGradient(palette: FlowTimerPalette, intensity: Float): List<Color> {
        val currentColor = calculateIntensityColor(palette, intensity)
        val nextLighter = lerp(currentColor, palette.peakColor, 0.4f)
        val deepAccent = lerp(currentColor, palette.baseColor, 0.3f)
        return listOf(deepAccent, currentColor, nextLighter, currentColor)
    }

    /**
     * Returns the glowing halo ambient color for the timer canvas
     */
    fun getAmbientGlowColor(palette: FlowTimerPalette, intensity: Float): Color {
        val current = calculateIntensityColor(palette, intensity)
        val alphaBoost = (0.20f + (intensity * 0.18f)).coerceIn(0.15f, 0.45f)
        return current.copy(alpha = alphaBoost)
    }
}
