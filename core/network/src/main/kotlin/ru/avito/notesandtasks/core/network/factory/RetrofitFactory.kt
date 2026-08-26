package ru.avito.notesandtasks.core.network.factory

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object RetrofitFactory {
    private val defaultJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun create(
        baseUrl: String,
        client: OkHttpClient,
        json: Json = defaultJson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
