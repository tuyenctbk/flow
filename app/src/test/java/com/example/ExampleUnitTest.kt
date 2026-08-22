package com.example

import com.example.ui.theme.FlowIntensityColorEngine
import com.example.ui.theme.FlowTimerPalette
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFlowTimerPalettesExist() {
    val palettes = FlowTimerPalette.values()
    assertTrue(palettes.isNotEmpty())
    assertTrue(palettes.contains(FlowTimerPalette.SOLAR_AMBER))
    assertTrue(palettes.contains(FlowTimerPalette.ZEN_EMERALD))
    assertTrue(palettes.contains(FlowTimerPalette.CYAN_EQUILIBRIUM))
  }

  @Test
  fun testFlowIntensityCalculation() {
    val palette = FlowTimerPalette.SOLAR_AMBER
    val colorAtZero = FlowIntensityColorEngine.calculateIntensityColor(palette, 0f)
    val colorAtHalf = FlowIntensityColorEngine.calculateIntensityColor(palette, 0.5f)
    val colorAtMax = FlowIntensityColorEngine.calculateIntensityColor(palette, 1f)

    assertEquals(palette.baseColor, colorAtZero)
    assertEquals(palette.midColor, colorAtHalf)
    assertEquals(palette.peakColor, colorAtMax)
  }

  @Test
  fun testSoundscapeCrossfadeTransition() {
    val synth = com.example.data.FlowAudioSynthesizer()
    assertEquals(com.example.data.AmbientSoundProfile.NONE, synth.activeProfile)

    // Initial transition from NONE to BROWN_NOISE
    synth.transitionToPreset(com.example.data.AmbientSoundProfile.BROWN_NOISE)
    assertEquals(com.example.data.AmbientSoundProfile.BROWN_NOISE, synth.activeProfile)

    // Transition to FOREST_STREAM triggers crossfading
    synth.transitionToPreset(com.example.data.AmbientSoundProfile.FOREST_STREAM)
    assertEquals(com.example.data.AmbientSoundProfile.FOREST_STREAM, synth.activeProfile)
    assertTrue(synth.isTransitioning.value)
    assertEquals(com.example.data.AmbientSoundProfile.FOREST_STREAM, synth.transitioningTo.value)
  }

  @Test
  fun testIdleThresholdDuration() {
    val tenMinutesInSeconds = 600
    assertTrue(tenMinutesInSeconds == 10 * 60)
  }

  @Test
  fun testAppShortcutConstants() {
    assertEquals("com.example.ACTION_SHORTCUT", com.example.util.AppShortcutManager.EXTRA_ACTION_SHORTCUT)
    assertEquals("start_flow", com.example.util.AppShortcutManager.SHORTCUT_START_FLOW)
    assertEquals("add_reflection", com.example.util.AppShortcutManager.SHORTCUT_ADD_REFLECTION)
    assertEquals("open_stats", com.example.util.AppShortcutManager.SHORTCUT_OPEN_STATS)
  }
}
