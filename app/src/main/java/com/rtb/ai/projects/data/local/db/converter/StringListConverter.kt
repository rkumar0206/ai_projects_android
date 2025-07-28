package com.rtb.ai.projects.data.local.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rtb.ai.projects.util.AppUtil.convertToJsonString

class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { convertToJsonString() }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(it, listType)
        }
    }
}