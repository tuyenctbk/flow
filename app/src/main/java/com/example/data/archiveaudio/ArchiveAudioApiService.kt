package com.example.data.archiveaudio

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ArchiveAudioApiService {

    @GET("advancedsearch.php")
    suspend fun searchAudio(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = listOf("identifier", "title", "description", "downloads", "mediatype"),
        @Query("sort[]") sort: List<String> = listOf("downloads desc"),
        @Query("rows") rows: Int = 20,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json"
    ): ArchiveSearchResponse

    @GET("metadata/{identifier}")
    suspend fun getItemMetadata(
        @Path("identifier") identifier: String
    ): ArchiveMetadataResponse

    companion object {
        const val BASE_URL = "https://archive.org/"
    }
}
