package com.example.data

enum class AmbientSoundProfile(val displayName: String, val emoji: String, val category: String) {
    NONE("Silent", "🔇", "Base"),
    BROWN_NOISE("Deep Brown Noise", "🌊", "Noise"),
    PINK_NOISE("Pink Rain Noise", "🌧️", "Noise"),
    WHITE_NOISE("Crisp White Noise", "📻", "Noise"),
    GAMMA_40HZ("40Hz Gamma Focus", "🧠", "Binaural"),
    ALPHA_10HZ("10Hz Alpha Flow", "✨", "Binaural"),
    THETA_6HZ("6Hz Theta Insight", "🌌", "Binaural"),
    SOLFEGGIO_528HZ("528Hz Miracle Resonance", "✨", "Solfeggio"),
    SOLFEGGIO_432HZ("432Hz Natural Harmony", "🎵", "Solfeggio"),
    CAFE_ATMOSPHERE("Zen Tea House", "🍵", "Atmosphere"),
    FOREST_STREAM("Forest Stream & Wind", "🍃", "Atmosphere"),
    NIGHT_CRICKETS("Still Night & Starlight", "🦗", "Atmosphere")
}

data class SoundscapeLayerConfig(
    val binauralVolume: Float = 0.4f,       // 0.0 to 1.0
    val natureVolume: Float = 0.5f,         // 0.0 to 1.0 (Rain / Stream / Wind)
    val noiseVolume: Float = 0.3f,          // 0.0 to 1.0 (Brown / Pink / White)
    val harmonicBellVolume: Float = 0.6f,   // 0.0 to 1.0 (Tibetan Singing Bowl)
    val masterVolume: Float = 0.7f          // Overall gain
)
