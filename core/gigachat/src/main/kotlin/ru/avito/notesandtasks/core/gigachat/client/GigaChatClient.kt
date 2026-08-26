package ru.avito.notesandtasks.core.gigachat.client

import android.content.Context
import ru.avito.notesandtasks.core.gigachat.GigaChatTlsClientFactory
import ru.avito.notesandtasks.core.gigachat.api.GigaChatApi
import ru.avito.notesandtasks.core.gigachat.api.GigaChatBalanceEntryDto
import ru.avito.notesandtasks.core.gigachat.api.GigaChatCompletionRequest
import ru.avito.notesandtasks.core.gigachat.api.GigaChatMessageDto
import ru.avito.notesandtasks.core.gigachat.auth.GigaChatBuildConfigCredentials
import ru.avito.notesandtasks.core.gigachat.auth.GigaChatCredentials
import ru.avito.notesandtasks.core.gigachat.auth.GigaChatTokenProvider
import ru.avito.notesandtasks.core.network.factory.RetrofitFactory
import ru.avito.notesandtasks.core.network.result.ApiResult
import ru.avito.notesandtasks.core.network.result.safeApiCall

private const val AUTH_BASE_URL = "https://ngw.devices.sberbank.ru:9443/"
private const val LEGACY_API_BASE_URL = "https://gigachat.devices.sberbank.ru/"
private const val GIGACHAT_MODEL = "GigaChat"
private const val GIGACHAT_USER_AGENT = "notes-and-tasks-android"
private const val TASK_REFINEMENT_TEMPERATURE = 0f
private const val TASK_REFINEMENT_SYSTEM_PROMPT =
    "Сформулируй одну краткую и ясную задачу на русском языке. Верни только текст задачи без пояснений."
private const val USER_ROLE = "user"
private const val SYSTEM_ROLE = "system"
private const val PAY_AS_YOU_GO_FORBIDDEN_CODE = 403

data class BalanceEntry(
    val usage: String,
    val value: Long,
)

interface GigaChatClient {
    suspend fun getBalance(): ApiResult<List<BalanceEntry>>

    suspend fun refineTaskText(rawText: String): ApiResult<String>
}

class GigaChatClientFactory(
    private val context: Context,
) {
    fun create(
        credentials: GigaChatCredentials = GigaChatBuildConfigCredentials.read(),
    ): GigaChatClient {
        val tlsClient = GigaChatTlsClientFactory(context).create()
        val authApi = RetrofitFactory.create(
            baseUrl = AUTH_BASE_URL,
            client = tlsClient,
        ).create(ru.avito.notesandtasks.core.gigachat.api.GigaChatAuthApi::class.java)
        val api = RetrofitFactory.create(
            baseUrl = LEGACY_API_BASE_URL,
            client = tlsClient,
        ).create(GigaChatApi::class.java)
        return GigaChatClientImpl(
            api = api,
            tokenProvider = GigaChatTokenProvider(
                api = authApi,
                credentials = credentials,
            ),
        )
    }
}

internal class GigaChatClientImpl(
    private val api: GigaChatApi,
    private val tokenProvider: GigaChatTokenProvider,
) : GigaChatClient {
    override suspend fun getBalance(): ApiResult<List<BalanceEntry>> = withAccessToken { accessToken ->
        when (
            val result = safeApiCall {
                api.getBalance(
                    authorization = "Bearer $accessToken",
                    userAgent = GIGACHAT_USER_AGENT,
                )
            }
        ) {
            is ApiResult.Success -> ApiResult.Success(result.data.balance.map(GigaChatBalanceEntryDto::toBalanceEntry))
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    override suspend fun refineTaskText(rawText: String): ApiResult<String> {
        if (rawText.isBlank()) {
            return ApiResult.UnknownError(IllegalArgumentException())
        }
        return withAccessToken { accessToken ->
            when (
                val result = safeApiCall {
                    api.completeChat(
                        authorization = "Bearer $accessToken",
                        userAgent = GIGACHAT_USER_AGENT,
                        request = GigaChatCompletionRequest(
                            model = GIGACHAT_MODEL,
                            temperature = TASK_REFINEMENT_TEMPERATURE,
                            messages = listOf(
                                GigaChatMessageDto(
                                    role = SYSTEM_ROLE,
                                    content = TASK_REFINEMENT_SYSTEM_PROMPT,
                                ),
                                GigaChatMessageDto(
                                    role = USER_ROLE,
                                    content = rawText,
                                ),
                            ),
                        ),
                    )
                }
            ) {
                is ApiResult.Success -> {
                    val refinedText = result.data.choices
                        .firstOrNull()
                        ?.message
                        ?.content
                        ?.takeIf(String::isNotBlank)
                    if (refinedText == null) {
                        ApiResult.UnknownError(IllegalStateException())
                    } else {
                        ApiResult.Success(refinedText)
                    }
                }

                is ApiResult.HttpError -> result
                is ApiResult.NetworkError -> result
                is ApiResult.UnknownError -> result
            }
        }
    }

    private suspend fun <T> withAccessToken(
        block: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> = when (val tokenResult = tokenProvider.accessToken()) {
        is ApiResult.Success -> block(tokenResult.data)
        is ApiResult.HttpError -> tokenResult
        is ApiResult.NetworkError -> tokenResult
        is ApiResult.UnknownError -> tokenResult
    }
}

fun ApiResult<List<BalanceEntry>>.isPayAsYouGoBalanceForbidden(): Boolean =
    this is ApiResult.HttpError && code == PAY_AS_YOU_GO_FORBIDDEN_CODE

private fun GigaChatBalanceEntryDto.toBalanceEntry(): BalanceEntry = BalanceEntry(
    usage = usage,
    value = value,
)
