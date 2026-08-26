package ru.avito.notesandtasks.core.voice.auth

import java.util.UUID
import ru.avito.notesandtasks.core.voice.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.avito.notesandtasks.core.network.result.ApiResult
import ru.avito.notesandtasks.core.network.result.safeApiCall
import ru.avito.notesandtasks.core.voice.recognition.SaluteSpeechAuthApi

private const val TOKEN_EXPIRATION_SAFETY_WINDOW_MILLIS = 60_000L

data class SaluteSpeechCredentials(
    val authorizationKey: String,
    val scope: SaluteSpeechScope,
)

enum class SaluteSpeechScope(
    val value: String,
) {
    PERSONAL("SALUTE_SPEECH_PERS"),
    CORPORATE_POSTPAID("SALUTE_SPEECH_CORP"),
    CORPORATE_PREPAID("SALUTE_SPEECH_B2B"),
}

object SaluteSpeechBuildConfigCredentials {
    fun readOrNull(): SaluteSpeechCredentials? {
        val authorizationKey = BuildConfig.SALUTE_SPEECH_AUTH_KEY
        val scope = SaluteSpeechScope.entries.firstOrNull {
            it.value == BuildConfig.SALUTE_SPEECH_SCOPE
        }
        return authorizationKey.takeIf(String::isNotBlank)?.let { key ->
            scope?.let { SaluteSpeechCredentials(authorizationKey = key, scope = it) }
        }
    }
}

data object SaluteSpeechConfigurationException : IllegalStateException()

internal class SaluteSpeechTokenProvider(
    private val api: SaluteSpeechAuthApi,
    private val credentials: SaluteSpeechCredentials,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var cachedToken: CachedAccessToken? = null

    suspend fun accessToken(): ApiResult<String> = mutex.withLock {
        if (credentials.authorizationKey.isBlank() || credentials.scope.value.isBlank()) {
            return@withLock ApiResult.UnknownError(cause = SaluteSpeechConfigurationException)
        }
        cachedToken
            ?.takeIf(::isUsable)
            ?.let { return@withLock ApiResult.Success(it.value) }

        when (
            val result = safeApiCall {
                api.exchangeToken(
                    authorization = "Basic ${credentials.authorizationKey}",
                    requestId = UUID.randomUUID().toString(),
                    scope = credentials.scope.value,
                )
            }
        ) {
            is ApiResult.Success -> {
                if (result.data.accessToken.isBlank()) {
                    ApiResult.UnknownError(cause = SaluteSpeechConfigurationException)
                } else {
                    cachedToken = CachedAccessToken(
                        value = result.data.accessToken,
                        expiresAtMillis = result.data.expiresAtMillis,
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

private data class CachedAccessToken(
    val value: String,
    val expiresAtMillis: Long,
)
