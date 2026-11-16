package com.example.ailearning.data.local.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return if (value.isNullOrBlank()) emptyList()
        else gson.fromJson<List<String>>(value, object : TypeToken<List<String>>() {}.type)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return gson.toJson(list)
    }
}
