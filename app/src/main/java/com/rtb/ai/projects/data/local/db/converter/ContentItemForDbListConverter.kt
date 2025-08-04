package com.rtb.ai.projects.data.local.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rtb.ai.projects.data.model.ContentItemForDbSaving

class ContentItemForDbListConverter {

    @TypeConverter
    fun fromContentItemList(value: List<ContentItemForDbSaving>?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toContentItemList(value: String?): List<ContentItemForDbSaving>? {
        val listType = object : TypeToken<List<ContentItemForDbSaving>?>() {}.type
        return Gson().fromJson(value, listType)
    }
}