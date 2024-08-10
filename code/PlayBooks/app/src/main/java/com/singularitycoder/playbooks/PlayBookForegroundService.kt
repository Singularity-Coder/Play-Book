package com.singularitycoder.playbooks

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.PermissionChecker
import com.singularitycoder.playbooks.helpers.NotificationsHelper
import kotlinx.coroutines.cancelChildren
import kotlin.time.Duration.Companion.seconds

/**
 * https://developer.android.com/develop/background-work/services/foreground-services
 * https://developer.android.com/about/versions/14/changes/fgs-types-required#media
 * https://github.com/landomen/ForegroundService14Sample
 * https://medium.com/@sarafanshul/jni-101-introduction-to-java-native-interface-8a1256ca4d8e
 * https://github.com/szelinskip/MusicPlayer
 *
 * Grant Notification permission
 * If you want the notification non-dismissable by the user, pass true into the setOngoing() method when you create your notification using Notification.Builder.
 * */

class PlayBookForegroundService : Service() {
    companion object {
        private const val TAG = "PlayBookForegroundService"
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PlayBookForegroundService = this@PlayBookForegroundService
    }

    /** Return the communication channel to the service. */
    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        startAsForegroundService()
        // init TTS
        return super.onStartCommand(intent, flags, startId)
    }

    /** Foreground Service created */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        // play book
    }

    /** Foreground Service destroyed */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        // tts.shutdown
    }

    /**
     * Promotes the service to a foreground service, showing a notification to the user.
     * This needs to be called within 10 seconds of starting the service or the system will throw an exception.
     */
    private fun startAsForegroundService() {
        // Before starting the service as foreground check that the app has the
        // appropriate runtime permissions. In this case, verify that the user has
        // granted the CAMERA permission.
//        val cameraPermission =
//            PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA)
//        if (cameraPermission != PermissionChecker.PERMISSION_GRANTED) {
//            // Without camera permissions the service cannot run in the foreground
//            // Consider informing user or updating your app UI if visible.
//            stopSelf()
//            return
//        }

        try {
            // Create the notification to display while the service is running
//            val notification = NotificationCompat.Builder(this, "CHANNEL_ID").build()
            NotificationsHelper.createNotificationChannel(this)

            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ 1, // Cannot be 0
                /* notification = */ NotificationsHelper.buildNotification(this),
                /* foregroundServiceType = */ ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } catch (e: Exception) {
            if (e is ForegroundServiceStartNotAllowedException) {
                // App not in a valid state to start foreground service
                // (e.g. started from bg)
            }
        }
    }

    /**
     * Stops the foreground service and removes the notification.
     * Can be called from inside or outside the service.
     */
    fun stopForegroundService() {
        stopSelf()
    }
}