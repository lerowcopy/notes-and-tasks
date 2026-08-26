package ru.avito.notesandtasks.core.network.factory

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ru.avito.notesandtasks.core.network.BuildConfig

object OkHttpClientFactory {
    private const val DEFAULT_TIMEOUT_SECONDS = 30L

    fun create(
        isDebug: Boolean = BuildConfig.DEBUG,
    ): OkHttpClient = createBuilder(isDebug).build()

    fun createBuilder(
        isDebug: Boolean = BuildConfig.DEBUG,
    ): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (isDebug) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
}
