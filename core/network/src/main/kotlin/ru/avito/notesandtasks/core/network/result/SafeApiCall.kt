package ru.avito.notesandtasks.core.network.result

import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import retrofit2.Response

suspend fun <T> safeApiCall(
    request: suspend () -> Response<T>,
): ApiResult<T> = try {
    val response = request()
    if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            ApiResult.Success(body)
        } else {
            ApiResult.UnknownError(
                cause = IllegalStateException("Successful response does not contain a body"),
            )
        }
    } else {
        ApiResult.HttpError(
            code = response.code(),
            errorBody = response.readErrorBody(),
        )
    }
} catch (exception: HttpException) {
    ApiResult.HttpError(
        code = exception.code(),
        errorBody = exception.response()?.readErrorBody(),
    )
} catch (exception: IOException) {
    ApiResult.NetworkError(cause = exception)
} catch (exception: SerializationException) {
    ApiResult.UnknownError(cause = exception)
} catch (exception: Throwable) {
    ApiResult.UnknownError(cause = exception)
}

private fun Response<*>.readErrorBody(): String? = runCatching {
    errorBody()?.string()
}.getOrNull()
