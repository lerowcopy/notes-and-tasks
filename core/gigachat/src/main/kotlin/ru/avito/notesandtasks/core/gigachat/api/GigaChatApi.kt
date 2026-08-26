package ru.avito.notesandtasks.core.gigachat.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

internal interface GigaChatAuthApi {
    @FormUrlEncoded
    @POST("api/v2/oauth")
    suspend fun exchangeToken(
        @Header("Authorization") authorization: String,
        @Header("RqUID") requestId: String,
        @Field("scope") scope: String,
    ): Response<GigaChatTokenResponse>
}

internal interface GigaChatApi {
    @GET("v1/balance")
    suspend fun getBalance(
        @Header("Authorization") authorization: String,
        @Header("User-Agent") userAgent: String,
    ): Response<GigaChatBalanceResponse>

    @POST("v1/chat/completions")
    suspend fun completeChat(
        @Header("Authorization") authorization: String,
        @Header("User-Agent") userAgent: String,
        @Body request: GigaChatCompletionRequest,
    ): Response<GigaChatCompletionResponse>
}

@Serializable
internal data class GigaChatTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_at") val expiresAt: Long,
)

@Serializable
internal data class GigaChatBalanceResponse(
    val balance: List<GigaChatBalanceEntryDto> = emptyList(),
)

@Serializable
internal data class GigaChatBalanceEntryDto(
    val usage: String,
    val value: Long,
)

@Serializable
internal data class GigaChatCompletionRequest(
    val model: String,
    val messages: List<GigaChatMessageDto>,
    val temperature: Float? = null,
)

@Serializable
internal data class GigaChatMessageDto(
    val role: String,
    val content: String,
)

@Serializable
internal data class GigaChatCompletionResponse(
    val choices: List<GigaChatChoiceDto> = emptyList(),
)

@Serializable
internal data class GigaChatChoiceDto(
    val message: GigaChatMessageDto,
)
