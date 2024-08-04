package com.singularitycoder.playbooks.helpers

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object PlayBookUtils {
    val gson: Gson = GsonBuilder().setLenient().create()
//    val typedValue = TypedValue()
//    val webpageFragmentIdList = mutableListOf<String?>()
}