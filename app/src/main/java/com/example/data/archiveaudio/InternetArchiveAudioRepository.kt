package com.example.data.archiveaudio

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class InternetArchiveAudioRepository {

    private val apiService: ArchiveAudioApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(ArchiveAudioApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ArchiveAudioApiService::class.java)
    }

    suspend fun searchAmbientRecordings(userQuery: String): List<ArchiveAudioItem> = withContext(Dispatchers.IO) {
        if (userQuery.isBlank()) {
            return@withContext PresetArchiveAudioCollections.BUILT_IN_COLLECTIONS
        }

        try {
            val formattedQuery = "mediatype:(audio) AND ($userQuery OR ambient OR nature OR sleep OR rainforest OR space)"
            val searchResponse = apiService.searchAudio(query = formattedQuery, rows = 15)

            val docs = searchResponse.response?.docs ?: emptyList()
            if (docs.isEmpty()) {
                return@withContext filterBuiltInCollections(userQuery)
            }

            val resultItems = mutableListOf<ArchiveAudioItem>()
            for (doc in docs.take(8)) {
                try {
                    val metadata = apiService.getItemMetadata(doc.identifier)
                    val audioFiles = metadata.files?.filter { file ->
                        val fmt = file.format?.lowercase() ?: ""
                        fmt.contains("mp3") || fmt.contains("vbr mp3") || fmt.contains("ogg") || fmt.contains("flac")
                    } ?: emptyList()

                    val bestFile = audioFiles.maxByOrNull { it.size?.toLongOrNull() ?: 0L }
                    if (bestFile != null && metadata.server != null && metadata.dir != null) {
                        val streamUrl = "https://${metadata.server}${metadata.dir}/${bestFile.name}"
                        val cleanTitle = doc.title ?: bestFile.title ?: doc.identifier.replace("_", " ")
                        val cleanDesc = when (val desc = doc.description) {
                            is String -> desc.take(120)
                            else -> "Public Domain Internet Archive Ambient Recording."
                        }

                        val emoji = assignCategoryEmoji(userQuery, cleanTitle)

                        resultItems.add(
                            ArchiveAudioItem(
                                id = doc.identifier,
                                title = cleanTitle,
                                description = cleanDesc,
                                category = userQuery.capitalizeFirst(),
                                streamUrl = streamUrl,
                                durationText = formatLengthText(bestFile.length),
                                iconEmoji = emoji,
                                downloads = doc.downloads ?: 1200
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w("ArchiveAudioRepo", "Skipping item ${doc.identifier} due to metadata error", e)
                }
            }

            if (resultItems.isEmpty()) {
                return@withContext filterBuiltInCollections(userQuery)
            }

            return@withContext resultItems
        } catch (e: Exception) {
            Log.e("ArchiveAudioRepo", "Error searching Internet Archive audio, returning built-in collections", e)
            return@withContext filterBuiltInCollections(userQuery)
        }
    }

    private fun filterBuiltInCollections(query: String): List<ArchiveAudioItem> {
        val q = query.lowercase()
        val filtered = PresetArchiveAudioCollections.BUILT_IN_COLLECTIONS.filter {
            it.title.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.category.lowercase().contains(q)
        }
        return filtered.ifEmpty { PresetArchiveAudioCollections.BUILT_IN_COLLECTIONS }
    }

    private fun assignCategoryEmoji(query: String, title: String): String {
        val combined = "$query $title".lowercase()
        return when {
            combined.contains("rain") || combined.contains("forest") -> "🌿"
            combined.contains("thunder") || combined.contains("storm") -> "⛈️"
            combined.contains("space") || combined.contains("nasa") -> "🚀"
            combined.contains("ocean") || combined.contains("wave") || combined.contains("sea") -> "🌊"
            combined.contains("cricket") || combined.contains("night") -> "🦗"
            combined.contains("fire") || combined.contains("camp") -> "🔥"
            else -> "📻"
        }
    }

    private fun formatLengthText(lengthSeconds: String?): String {
        val secs = lengthSeconds?.toDoubleOrNull()?.toInt() ?: return "30 mins"
        val mins = secs / 60
        return "$mins mins"
    }

    private fun String.capitalizeFirst(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
