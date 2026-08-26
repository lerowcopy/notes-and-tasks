package ru.avito.notesandtasks.core.common.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
}

/**
 * Применяет только уже подтверждённый поисковый запрос. Presentation-слой должен передавать
 * сюда отдельный Flow отправленного запроса, а не значение поля ввода на каждое нажатие клавиши.
 */
fun <T> Flow<List<T>>.applySearchAndSort(
    submittedQuery: Flow<String>,
    sortOrder: Flow<SortOrder>,
    titleSelector: (T) -> String,
    createdAtSelector: (T) -> Long,
): Flow<List<T>> = combine(this, submittedQuery, sortOrder) { items, query, order ->
    val filteredItems = if (query.isBlank()) {
        items
    } else {
        items.filter { item ->
            titleSelector(item).contains(query.trim(), ignoreCase = true)
        }
    }

    when (order) {
        SortOrder.NEWEST_FIRST -> filteredItems.sortedByDescending(createdAtSelector)
        SortOrder.OLDEST_FIRST -> filteredItems.sortedBy(createdAtSelector)
    }
}
