package com.cs407.uhere.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromLocationCategory(value: LocationCategory): String {
        return value.name
    }

    @TypeConverter
    fun toLocationCategory(value: String): LocationCategory {
        return LocationCategory.valueOf(value)
    }
}