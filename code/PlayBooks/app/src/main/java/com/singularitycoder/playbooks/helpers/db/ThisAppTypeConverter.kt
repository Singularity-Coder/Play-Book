package com.singularitycoder.playbooks.helpers.db

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.singularitycoder.playbooks.helpers.PlayBookUtils
import java.lang.reflect.Type

// Type converters must not contain any arguments in the constructor
// Classes that are used as TypeConverters must have no-argument public constructors.
// Use a ProvidedTypeConverter annotation if you need to take control over creating an instance of a TypeConverter.

// https://stackoverflow.com/questions/46585075/android-how-to-make-type-converters-for-room-generic-for-all-list-of-objects
abstract class ThisAppTypeConverter<T> {
    val type: Type = object : TypeToken<List<T>>() {}.type

    @TypeConverter
    fun listToString(value: List<T>): String = PlayBookUtils.gson.toJson(value, type)

    @TypeConverter
    fun stringToList(value: String): List<T> = PlayBookUtils.gson.fromJson(value, type)
}

class StringListConverter : ThisAppTypeConverter<String>()

class IntListConverter {
    @TypeConverter
    fun fromListInt(list: List<Int>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toListInt(data: String): List<Int> {
        return listOf(*data.split(",").map { it.toInt() }.toTypedArray())
    }
}

class IntHashMapConverter {
    @TypeConverter
    fun fromHashMap(value: HashMap<Int, Int>): String = PlayBookUtils.gson.toJson(value)

    @TypeConverter
    fun toHashMap(value: String): HashMap<Int, Int> =
        PlayBookUtils.gson.fromJson(value, object : TypeToken<Map<Int, Int>>() {}.type)
}
