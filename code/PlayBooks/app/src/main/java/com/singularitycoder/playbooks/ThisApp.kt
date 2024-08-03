package com.singularitycoder.playbooks

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ThisApp : Application() {
    var isCollectionsScreenLoaded = false
    lateinit var context: Context

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}
