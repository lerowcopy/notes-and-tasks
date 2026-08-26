package ru.avito.notesandtasks.core.voice.recognition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

internal interface SaluteSpeechAuthApi {
    @FormUrlEncoded
    @POST("api/v2/oauth")
    suspend fun exchangeToken(
        @Header("Authorization") authorization: String,
        @Header("RqUID") requestId: String,
        @Field("scope") scope: String,
    ): Response<SaluteSpeechTokenResponse>
}

internal interface SaluteSpeechRecognitionApi {
    @POST("rest/v1/speech:recognize")
    suspend fun recognize(
        @Header("Authorization") authorization: String,
        @Header("X-Request-ID") requestId: String,
        @Header("Content-Type") contentType: String,
        @Query("language") language: String,
        @Query("model") model: String,
        @Query("enable_profanity_filter") enableProfanityFilter: Boolean,
        @Body audio: RequestBody,
    ): Response<SaluteSpeechRecognitionResponse>
}

@Serializable
internal data class SaluteSpeechTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_at") val expiresAtMillis: Long,
)

@Serializable
internal data class SaluteSpeechRecognitionResponse(
    val result: List<String> = emptyList(),
    val status: Int? = null,
)
