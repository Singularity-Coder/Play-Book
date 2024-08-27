package com.singularitycoder.playbooks

import android.app.Application
import com.singularitycoder.playbooks.helpers.AppPreferences
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ThisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(applicationContext)
    }
}
