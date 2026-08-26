package ru.avito.notesandtasks.core.gigachat.auth

import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.avito.notesandtasks.core.gigachat.BuildConfig
import ru.avito.notesandtasks.core.gigachat.api.GigaChatAuthApi
import ru.avito.notesandtasks.core.network.result.ApiResult
import ru.avito.notesandtasks.core.network.result.safeApiCall

private const val TOKEN_EXPIRATION_SAFETY_WINDOW_MILLIS = 60_000L
private const val UNIX_MILLISECONDS_THRESHOLD = 100_000_000_000L

data class GigaChatCredentials(
    val authorizationKey: String,
    val scope: String,
)

object GigaChatBuildConfigCredentials {
    fun read(): GigaChatCredentials = GigaChatCredentials(
        authorizationKey = BuildConfig.GIGACHAT_AUTH_KEY,
        scope = BuildConfig.GIGACHAT_SCOPE,
    )
}

internal class GigaChatTokenProvider(
    private val api: GigaChatAuthApi,
    private val credentials: GigaChatCredentials,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var cachedToken: CachedAccessToken? = null

    suspend fun accessToken(): ApiResult<String> = mutex.withLock {
        if (credentials.authorizationKey.isBlank() || credentials.scope.isBlank()) {
            return@withLock ApiResult.UnknownError(GigaChatConfigurationException)
        }
        cachedToken
            ?.takeIf(::isUsable)
            ?.let { return@withLock ApiResult.Success(it.value) }

        when (
            val result = safeApiCall {
                api.exchangeToken(
                    authorization = "Basic ${credentials.authorizationKey}",
                    requestId = UUID.randomUUID().toString(),
                    scope = credentials.scope,
                )
            }
        ) {
            is ApiResult.Success -> {
                if (result.data.accessToken.isBlank()) {
                    ApiResult.UnknownError(GigaChatConfigurationException)
                } else {
                    cachedToken = CachedAccessToken(
                        value = result.data.accessToken,
                        expiresAtMillis = result.data.expiresAt.toEpochMillis(),
                    )
                    ApiResult.Success(result.data.accessToken)
                }
            }

            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
        }
    }

    private fun isUsable(token: CachedAccessToken): Boolean =
        token.expiresAtMillis - currentTimeMillis() > TOKEN_EXPIRATION_SAFETY_WINDOW_MILLIS
}

private fun Long.toEpochMillis(): Long =
    if (this < UNIX_MILLISECONDS_THRESHOLD) this * 1_000 else this

private data class CachedAccessToken(
    val value: String,
    val expiresAtMillis: Long,
)

private object GigaChatConfigurationException : IllegalStateException()
