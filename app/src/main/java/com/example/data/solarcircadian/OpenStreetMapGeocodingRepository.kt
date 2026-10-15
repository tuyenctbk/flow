package com.example.data.solarcircadian

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NominatimLocationResult(
    @Json(name = "lat") val latStr: String? = null,
    @Json(name = "lon") val lonStr: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "name") val name: String? = null
)

interface NominatimApiService {
    @GET("search")
    suspend fun searchCity(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Header("User-Agent") userAgent: String = "FlowFocusApp/2.0 (com.soloprono.flow)"
    ): List<NominatimLocationResult>

    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
    }
}

class OpenStreetMapGeocodingRepository {

    private val apiService: NominatimApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(NominatimApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NominatimApiService::class.java)
    }

    suspend fun searchCityCoordinates(cityName: String): NominatimLocationResult? = withContext(Dispatchers.IO) {
        if (cityName.isBlank()) return@withContext null
        try {
            val results = apiService.searchCity(query = cityName)
            return@withContext results.firstOrNull()
        } catch (e: Exception) {
            android.util.Log.e("NominatimGeocoding", "Error geocoding city search", e)
            return@withContext null
        }
    }
}
