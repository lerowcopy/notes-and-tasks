package ru.avito.notesandtasks.core.network.result

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    data class HttpError(
        val code: Int,
        val errorBody: String? = null,
    ) : ApiResult<Nothing>

    data class NetworkError(
        val cause: Throwable,
    ) : ApiResult<Nothing>

    data class UnknownError(
        val cause: Throwable,
    ) : ApiResult<Nothing>
}
