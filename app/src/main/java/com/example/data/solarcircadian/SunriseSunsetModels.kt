package com.example.data.solarcircadian

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SunriseSunsetResponse(
    @Json(name = "results") val results: SunriseSunsetResults?,
    @Json(name = "status") val status: String?
)

@JsonClass(generateAdapter = true)
data class SunriseSunsetResults(
    @Json(name = "sunrise") val sunrise: String? = null,
    @Json(name = "sunset") val sunset: String? = null,
    @Json(name = "solar_noon") val solarNoon: String? = null,
    @Json(name = "day_length") val dayLength: String? = null,
    @Json(name = "civil_twilight_begin") val civilTwilightBegin: String? = null,
    @Json(name = "civil_twilight_end") val civilTwilightEnd: String? = null,
    @Json(name = "nautical_twilight_begin") val nauticalTwilightBegin: String? = null,
    @Json(name = "nautical_twilight_end") val nauticalTwilightEnd: String? = null,
    @Json(name = "astronomical_twilight_begin") val astronomicalTwilightBegin: String? = null,
    @Json(name = "astronomical_twilight_end") val astronomicalTwilightEnd: String? = null
)

enum class CircadianPhase(
    val displayNameResId: Int,
    val iconEmoji: String,
    val descriptionResId: Int
) {
    DAWN(
        displayNameResId = com.example.R.string.circadian_phase_dawn,
        iconEmoji = "🌅",
        descriptionResId = com.example.R.string.circadian_phase_dawn_desc
    ),
    DAYLIGHT(
        displayNameResId = com.example.R.string.circadian_phase_daylight,
        iconEmoji = "☀️",
        descriptionResId = com.example.R.string.circadian_phase_daylight_desc
    ),
    GOLDEN_HOUR(
        displayNameResId = com.example.R.string.circadian_phase_golden_hour,
        iconEmoji = "🌆",
        descriptionResId = com.example.R.string.circadian_phase_golden_hour_desc
    ),
    SUNSET_TWILIGHT(
        displayNameResId = com.example.R.string.circadian_phase_sunset_twilight,
        iconEmoji = "🌆",
        descriptionResId = com.example.R.string.circadian_phase_sunset_twilight_desc
    ),
    NIGHTFALL(
        displayNameResId = com.example.R.string.circadian_phase_nightfall,
        iconEmoji = "🌙",
        descriptionResId = com.example.R.string.circadian_phase_nightfall_desc
    ),
    DEEP_SLEEP(
        displayNameResId = com.example.R.string.circadian_phase_deep_sleep,
        iconEmoji = "🌌",
        descriptionResId = com.example.R.string.circadian_phase_deep_sleep_desc
    )
}

data class CircadianSolarInfo(
    val phase: CircadianPhase = CircadianPhase.DAYLIGHT,
    val formattedSunrise: String = "06:00 AM",
    val formattedSunset: String = "06:30 PM",
    val formattedCivilTwilightEnd: String = "07:00 PM",
    val minutesUntilSunset: Int = 120,
    val dimRatio: Float = 0.0f,               // 0.0f (Full daylight) -> 0.85f (Deep night)
    val frequencyShiftRatio: Float = 0.0f,    // 0.0f (Crisp daytime) -> 1.0f (Deep warm low-pass twilight)
    val lat: Double = 10.7769,
    val lng: Double = 106.7009,
    val locationName: String = "Ho Chi Minh City",
    val isSyncedWithApi: Boolean = false
)
