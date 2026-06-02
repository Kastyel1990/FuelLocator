package com.crimeafuel.app.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromStringMap(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                result[key] = obj.getString(key)
            }
        } catch (_: Exception) {}
        return result
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>): String {
        return JSONObject(map).toString()
    }

    @TypeConverter
    fun fromStringList(json: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                result.add(arr.getString(i))
            }
        } catch (_: Exception) {}
        return result
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return JSONArray(list).toString()
    }
}
