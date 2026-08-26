package ru.avito.notesandtasks.core.voice.recognition

import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import ru.avito.notesandtasks.core.network.factory.OkHttpClientFactory
import ru.avito.notesandtasks.core.network.factory.RetrofitFactory
import ru.avito.notesandtasks.core.network.result.ApiResult
import ru.avito.notesandtasks.core.network.result.safeApiCall
import ru.avito.notesandtasks.core.voice.auth.SaluteSpeechCredentials
import ru.avito.notesandtasks.core.voice.auth.SaluteSpeechTokenProvider

private const val AUTH_BASE_URL = "https://ngw.devices.sberbank.ru:9443/"
private const val RECOGNITION_BASE_URL = "https://smartspeech.sber.ru/"
private const val MAX_SYNC_AUDIO_BYTES = 2_000_000L
private const val TRANSCRIPT_SEGMENT_SEPARATOR = " "
private val OPUS_OGG_MEDIA_TYPE = "audio/ogg;codecs=opus".toMediaType()

enum class SaluteSpeechLanguage(
    val value: String,
) {
    RUSSIAN("ru-RU"),
    ENGLISH("en-US"),
    KAZAKH("kk-KZ"),
    KYRGYZ("ky-KG"),
    UZBEK("uz-UZ"),
}

enum class SaluteSpeechRecognitionModel(
    val value: String,
) {
    GENERAL("general"),
    CALL_CENTER("callcenter"),
    MEDIA("media"),
    IVR("ivr"),
}

interface SpeechRecognizer {
    suspend fun recognize(file: File): ApiResult<String>
}

internal class SaluteSpeechRecognizer(
    private val api: SaluteSpeechRecognitionApi,
    private val tokenProvider: SaluteSpeechTokenProvider,
) : SpeechRecognizer {
    override suspend fun recognize(file: File): ApiResult<String> {
        if (!file.isFile || file.length() > MAX_SYNC_AUDIO_BYTES) {
            return ApiResult.UnknownError(cause = IllegalArgumentException())
        }

        return when (val tokenResult = tokenProvider.accessToken()) {
            is ApiResult.Success -> recognizeWithToken(file, tokenResult.data)
            is ApiResult.HttpError -> tokenResult
            is ApiResult.NetworkError -> tokenResult
            is ApiResult.UnknownError -> tokenResult
        }
    }

    private suspend fun recognizeWithToken(
        file: File,
        accessToken: String,
    ): ApiResult<String> = when (
        val result = safeApiCall {
            api.recognize(
                authorization = "Bearer $accessToken",
                requestId = java.util.UUID.randomUUID().toString(),
                contentType = OPUS_OGG_MEDIA_TYPE.toString(),
                language = SaluteSpeechLanguage.RUSSIAN.value,
                model = SaluteSpeechRecognitionModel.GENERAL.value,
                enableProfanityFilter = true,
                audio = file.asRequestBody(OPUS_OGG_MEDIA_TYPE),
            )
        }
    ) {
        is ApiResult.Success -> {
            val transcript = result.data.result.joinToString(TRANSCRIPT_SEGMENT_SEPARATOR)
            if (transcript.isBlank()) {
                ApiResult.UnknownError(cause = IllegalStateException())
            } else {
                ApiResult.Success(transcript)
            }
        }

        is ApiResult.HttpError -> result
        is ApiResult.NetworkError -> result
        is ApiResult.UnknownError -> result
    }
}

object SaluteSpeechRecognizerFactory {
    fun create(
        credentials: SaluteSpeechCredentials,
    ): SpeechRecognizer {
        val httpClient = OkHttpClientFactory.create()
        val authApi = RetrofitFactory.create(
            baseUrl = AUTH_BASE_URL,
            client = httpClient,
        ).create(SaluteSpeechAuthApi::class.java)
        val recognitionApi = RetrofitFactory.create(
            baseUrl = RECOGNITION_BASE_URL,
            client = httpClient,
        ).create(SaluteSpeechRecognitionApi::class.java)
        return SaluteSpeechRecognizer(
            api = recognitionApi,
            tokenProvider = SaluteSpeechTokenProvider(
                api = authApi,
                credentials = credentials,
            ),
        )
    }
}
