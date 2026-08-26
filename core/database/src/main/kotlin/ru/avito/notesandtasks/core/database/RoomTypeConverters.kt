package ru.avito.notesandtasks.core.database

import androidx.room.TypeConverter
import ru.avito.notesandtasks.core.common.flow.SortOrder

class RoomTypeConverters {
    @TypeConverter
    fun sortOrderToStorageValue(value: SortOrder): String = value.name

    @TypeConverter
    fun storageValueToSortOrder(value: String): SortOrder = SortOrder.valueOf(value)
}
