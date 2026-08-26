package ru.avito.notesandtasks.core.common.result

/**
 * Результат domain-операции. Состояние загрузки относится к presentation-слою
 * и поэтому намеренно не моделируется этим типом.
 */
sealed interface OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>

    data class Error(
        val cause: Throwable? = null,
        val message: String? = null,
    ) : OperationResult<Nothing>
}
