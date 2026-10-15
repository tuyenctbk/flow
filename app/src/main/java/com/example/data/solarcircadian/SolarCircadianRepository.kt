package com.example.data.solarcircadian

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SolarCircadianRepository {

    private val apiService: SunriseSunsetApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(SunriseSunsetApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SunriseSunsetApiService::class.java)
    }

    suspend fun fetchSolarCircadianInfo(
        lat: Double,
        lng: Double,
        locationName: String
    ): CircadianSolarInfo = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSolarTimes(lat = lat, lng = lng, formatted = 0)
            val results = response.results
            if (response.status == "OK" && results != null) {
                return@withContext parseAndCalculateSolarInfo(results, lat, lng, locationName, isSynced = true)
            }
        } catch (e: Exception) {
            Log.e("SolarCircadianRepo", "Error fetching solar times from API, falling back to local calculation", e)
        }

        // Fallback to local clock solar estimation if API is unreachable
        calculateLocalFallbackSolarInfo(lat, lng, locationName)
    }

    private fun parseAndCalculateSolarInfo(
        results: SunriseSunsetResults,
        lat: Double,
        lng: Double,
        locationName: String,
        isSynced: Boolean
    ): CircadianSolarInfo {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val displayFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val now = Calendar.getInstance().time

        val sunriseDate = results.sunrise?.let { safeParseDate(isoFormat, it) } ?: getDefaultTime(6, 0)
        val sunsetDate = results.sunset?.let { safeParseDate(isoFormat, it) } ?: getDefaultTime(18, 30)
        val civilEnd = results.civilTwilightEnd?.let { safeParseDate(isoFormat, it) } ?: getDefaultTime(19, 0)
        val civilStart = results.civilTwilightBegin?.let { safeParseDate(isoFormat, it) } ?: getDefaultTime(5, 30)

        val formattedSunrise = displayFormat.format(sunriseDate)
        val formattedSunset = displayFormat.format(sunsetDate)
        val formattedCivilEnd = displayFormat.format(civilEnd)

        val diffMs = sunsetDate.time - now.time
        val minutesUntilSunset = (diffMs / (1000 * 60)).toInt()

        val (phase, dimRatio, freqRatio) = computeCircadianMetrics(
            now = now,
            civilStart = civilStart,
            sunrise = sunriseDate,
            sunset = sunsetDate,
            civilEnd = civilEnd
        )

        return CircadianSolarInfo(
            phase = phase,
            formattedSunrise = formattedSunrise,
            formattedSunset = formattedSunset,
            formattedCivilTwilightEnd = formattedCivilEnd,
            minutesUntilSunset = minutesUntilSunset,
            dimRatio = dimRatio,
            frequencyShiftRatio = freqRatio,
            lat = lat,
            lng = lng,
            locationName = locationName,
            isSyncedWithApi = isSynced
        )
    }

    private fun safeParseDate(format: SimpleDateFormat, dateStr: String): Date? {
        return try {
            format.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun getDefaultTime(hour24: Int, minute: Int): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour24)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        return cal.time
    }

    private fun calculateLocalFallbackSolarInfo(
        lat: Double,
        lng: Double,
        locationName: String
    ): CircadianSolarInfo {
        val cal = Calendar.getInstance()
        val now = cal.time

        val sunriseDate = getDefaultTime(6, 0)
        val sunsetDate = getDefaultTime(18, 30)
        val civilEnd = getDefaultTime(19, 0)
        val civilStart = getDefaultTime(5, 30)

        val displayFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val diffMs = sunsetDate.time - now.time
        val minutesUntilSunset = (diffMs / (1000 * 60)).toInt()

        val (phase, dimRatio, freqRatio) = computeCircadianMetrics(
            now = now,
            civilStart = civilStart,
            sunrise = sunriseDate,
            sunset = sunsetDate,
            civilEnd = civilEnd
        )

        return CircadianSolarInfo(
            phase = phase,
            formattedSunrise = displayFormat.format(sunriseDate),
            formattedSunset = displayFormat.format(sunsetDate),
            formattedCivilTwilightEnd = displayFormat.format(civilEnd),
            minutesUntilSunset = minutesUntilSunset,
            dimRatio = dimRatio,
            frequencyShiftRatio = freqRatio,
            lat = lat,
            lng = lng,
            locationName = locationName,
            isSyncedWithApi = false
        )
    }

    private fun computeCircadianMetrics(
        now: Date,
        civilStart: Date,
        sunrise: Date,
        sunset: Date,
        civilEnd: Date
    ): Triple<CircadianPhase, Float, Float> {
        val calNow = Calendar.getInstance().apply { time = now }
        val hour = calNow.get(Calendar.HOUR_OF_DAY)

        return when {
            // Dawn: between civil start and sunrise (approx 5:30 - 6:00 AM)
            now.before(sunrise) && now.after(civilStart) -> {
                Triple(CircadianPhase.DAWN, 0.2f, 0.2f)
            }
            // Daytime: between sunrise and 1 hour before sunset
            now.after(sunrise) && now.before(Date(sunset.time - 60 * 60 * 1000L)) -> {
                Triple(CircadianPhase.DAYLIGHT, 0.0f, 0.0f)
            }
            // Golden Hour: 1 hour before sunset
            now.after(Date(sunset.time - 60 * 60 * 1000L)) && now.before(sunset) -> {
                val progress = 1.0f - ((sunset.time - now.time).toFloat() / (60 * 60 * 1000L))
                val dim = 0.15f + (progress * 0.25f)
                val freq = 0.2f + (progress * 0.3f)
                Triple(CircadianPhase.GOLDEN_HOUR, dim, freq)
            }
            // Sunset Twilight: between sunset and civil twilight end
            now.after(sunset) && now.before(civilEnd) -> {
                val progress = ((now.time - sunset.time).toFloat() / (civilEnd.time - sunset.time).coerceAtLeast(1L))
                val dim = 0.4f + (progress * 0.3f)
                val freq = 0.5f + (progress * 0.3f)
                Triple(CircadianPhase.SUNSET_TWILIGHT, dim, freq)
            }
            // Nightfall: between civil twilight end and 11 PM
            hour in 19..22 -> {
                Triple(CircadianPhase.NIGHTFALL, 0.7f, 0.8f)
            }
            // Deep Sleep: 11 PM - 5:30 AM
            else -> {
                Triple(CircadianPhase.DEEP_SLEEP, 0.85f, 1.0f)
            }
        }
    }
}
