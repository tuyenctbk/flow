package com.example.data.solarcircadian

import retrofit2.http.GET
import retrofit2.http.Query

interface SunriseSunsetApiService {
    @GET("json")
    suspend fun getSolarTimes(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("formatted") formatted: Int = 0
    ): SunriseSunsetResponse

    companion object {
        const val BASE_URL = "https://api.sunrise-sunset.org/"
    }
}
