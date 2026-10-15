package com.example.data.archiveaudio

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    @Json(name = "response") val response: ArchiveSearchInnerResponse?
)

@JsonClass(generateAdapter = true)
data class ArchiveSearchInnerResponse(
    @Json(name = "numFound") val numFound: Int? = 0,
    @Json(name = "docs") val docs: List<ArchiveDoc>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveDoc(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: Any? = null,
    @Json(name = "downloads") val downloads: Int? = 0,
    @Json(name = "mediatype") val mediatype: String? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveMetadataResponse(
    @Json(name = "files") val files: List<ArchiveFile>? = emptyList(),
    @Json(name = "server") val server: String? = null,
    @Json(name = "dir") val dir: String? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveFile(
    @Json(name = "name") val name: String,
    @Json(name = "format") val format: String? = null,
    @Json(name = "size") val size: String? = null,
    @Json(name = "length") val length: String? = null,
    @Json(name = "title") val title: String? = null
)

data class ArchiveAudioItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val streamUrl: String,
    val durationText: String,
    val iconEmoji: String,
    val downloads: Int = 0
)

object PresetArchiveAudioCollections {
    val BUILT_IN_COLLECTIONS = listOf(
        ArchiveAudioItem(
            id = "rainforest_symphony",
            title = "Amazon Rainforest Symphony",
            description = "Limitless field recording of tropical rainforest rainfall, wind, & canopy birds.",
            category = "Rainforest",
            streamUrl = "https://archive.org/download/NatureSoundsRainforest/Rainforest_Ambiance.mp3",
            durationText = "60 mins",
            iconEmoji = "🌿",
            downloads = 48200
        ),
        ArchiveAudioItem(
            id = "thunderstorm_night",
            title = "Deep Midnight Thunderstorm",
            description = "Rolling thunder and steady rooftop rain for deep slumber and intense focus.",
            category = "Thunderstorm",
            streamUrl = "https://archive.org/download/ThunderstormSoundscape/Thunderstorm_3D_Audio.mp3",
            durationText = "45 mins",
            iconEmoji = "⛈️",
            downloads = 93100
        ),
        ArchiveAudioItem(
            id = "nasa_space_audio",
            title = "NASA Vintage Space Audio",
            description = "Historic NASA Voyager interstellar plasma emissions and deep space radio waves.",
            category = "NASA Space",
            streamUrl = "https://archive.org/download/NASASpaceSoundscapes/Voyager_Plasma_Waves.mp3",
            durationText = "30 mins",
            iconEmoji = "🚀",
            downloads = 125000
        ),
        ArchiveAudioItem(
            id = "ocean_waves_ambient",
            title = "Pacific Ocean Waves & Coast",
            description = "Gentle rhythmic ocean tides washing onto smooth pebble shores.",
            category = "Ocean",
            streamUrl = "https://archive.org/download/OceanWavesRelaxing/Pacific_Ocean_Surf.mp3",
            durationText = "60 mins",
            iconEmoji = "🌊",
            downloads = 67400
        ),
        ArchiveAudioItem(
            id = "night_crickets_slumber",
            title = "Still Night Crickets & Starlight",
            description = "Calm countryside evening with gentle crickets and soft summer breeze.",
            category = "Night Slumber",
            streamUrl = "https://archive.org/download/NightCricketsAmbient/Summer_Night_Crickets.mp3",
            durationText = "50 mins",
            iconEmoji = "🦗",
            downloads = 38900
        ),
        ArchiveAudioItem(
            id = "campfire_warmth",
            title = "Cozy Midnight Campfire",
            description = "Crackling cedar wood campfire with quiet ambient forest atmosphere.",
            category = "Campfire",
            streamUrl = "https://archive.org/download/CampfireSoundscapes/Cedar_Campfire_Crackle.mp3",
            durationText = "40 mins",
            iconEmoji = "🔥",
            downloads = 51200
        ),
        ArchiveAudioItem(
            id = "cosmic_binaural_sleep",
            title = "Cosmic White Noise & Delta Waves",
            description = "Deep space cosmic hum combined with low frequency delta waves for instant sleep.",
            category = "Cosmic Delta",
            streamUrl = "https://archive.org/download/CosmicDeltaWaves/Delta_Deep_Sleep_Hum.mp3",
            durationText = "60 mins",
            iconEmoji = "🌌",
            downloads = 110400
        )
    )
}
