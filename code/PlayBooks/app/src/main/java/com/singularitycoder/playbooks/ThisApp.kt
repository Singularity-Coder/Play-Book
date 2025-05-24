package com.singularitycoder.playbooks

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class ThisApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // https://medium.com/android-news/android-dev-tool-best-practice-with-strictmode-a023e09030a5
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectAll() // Detect all potential issues
                    .penaltyLog() // Log violations to logcat
                    .build()
            )

            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }

        /** Preload Dispatchers.Main to reduce Jank, especially on Splash */
        CoroutineScope(Dispatchers.Default).launch {
            Dispatchers.Main
        }
    }
}
