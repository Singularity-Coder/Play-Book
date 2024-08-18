package com.singularitycoder.playbooks.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.IntegerRes
import androidx.core.app.NotificationCompat
import com.singularitycoder.playbooks.MainActivity
import com.singularitycoder.playbooks.R
import com.singularitycoder.playbooks.ThisBroadcastReceiver

internal object NotificationsHelper {

    private const val NOTIFICATION_CHANNEL_ID = "general_notification_channel"
    private const val MUSIC_PLAYER_NOTIFICATION_ID: Int = 1

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager

        // create the notification channel
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.foreground_service_sample_notification_channel_general_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

//    fun buildNotification(context: Context): Notification {
//        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
//            .setContentTitle(context.getString(R.string.foreground_service_sample_notification_title))
//            .setContentText(context.getString(R.string.foreground_service_sample_notification_description))
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
//            .setContentIntent(Intent(context, MainActivity::class.java).let { notificationIntent ->
//                PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
//            })
//            .build()
//    }

    private var notificationLayoutExpanded: RemoteViews? = null

    fun createNotificationLayout(context: Context) {
        /** RemoteViews only accepts old layouts like Linear, Relative, etc. It cannot render constraintLayout
         * https://developer.android.com/develop/ui/views/notifications/build-notification
         * https://stackoverflow.com/questions/45396426/crash-when-using-constraintlayout-in-notification
         * https://developer.android.com/develop/ui/views/notifications/custom-notification */
        notificationLayoutExpanded = RemoteViews(context.packageName, R.layout.custom_service_notification)

        fun setPendingIntent(
            notificationAction: NotificationAction,
            @IntegerRes viewId: Int
        ): PendingIntent {
            val intent = Intent(context, ThisBroadcastReceiver::class.java).apply {
                action = IntentKey.NOTIFICATION_BUTTON_CLICK_BROADCAST
                putExtra(IntentExtraKey.NOTIFICATION_BUTTON_CLICK_TYPE, notificationAction.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                /* context = */ context,
                /* requestCode = */ notificationAction.ordinal,
                /* intent = */ intent,
                /* flags = */ PendingIntent.FLAG_IMMUTABLE
            )
            notificationLayoutExpanded?.setOnClickPendingIntent(viewId, pendingIntent)
            return pendingIntent
        }

        setPendingIntent(notificationAction = NotificationAction.NEXT_PAGE, viewId = R.id.iv_next_page)
        setPendingIntent(notificationAction = NotificationAction.PREVIOUS_PAGE, viewId = R.id.iv_previous_page)
        setPendingIntent(notificationAction = NotificationAction.PLAY_PAUSE, viewId = R.id.iv_play_pause)
        setPendingIntent(notificationAction = NotificationAction.NEXT_SENTENCE, viewId = R.id.iv_next_sentence)
        setPendingIntent(notificationAction = NotificationAction.PREVIOUS_SENTENCE, viewId = R.id.iv_previous_sentence)
    }

    fun createNotification(
        context: Context,
        @DrawableRes playPauseResId: Int,
        title: String?
    ): Notification {
        notificationLayoutExpanded?.setTextViewText(R.id.tv_notification_track_title, title ?: "")
        notificationLayoutExpanded?.setImageViewResource(R.id.iv_play_pause, playPauseResId)

        val pendingIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ Intent(context, MainActivity::class.java),
            /* flags = */ PendingIntent.FLAG_IMMUTABLE
        )
//        val intent = Intent(IntentKey.NOTIFICATION_BUTTON_CLICK_BROADCAST).apply {
//            putExtra(IntentExtraKey.NOTIFICATION_BUTTON_CLICK_TYPE, NotificationAction.PLAY_PAUSE)
//        }
//        val pendingIntentAction = PendingIntent.getBroadcast(
//            /* context = */ context,
//            /* requestCode = */ NotificationAction.PLAY_PAUSE.ordinal,
//            /* intent = */ intent,
//            /* flags = */ PendingIntent.FLAG_IMMUTABLE
//        )
        val notification: Notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle("Book name playing from play books application")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayoutExpanded)
            .setCustomBigContentView(notificationLayoutExpanded)
//            .setOngoing(true)
//            .setContent(notificationLayout)
//            .addAction(R.id.iv_play_pause, "play", pendingIntentAction)
            .setContentIntent(pendingIntent)
            .build()

        return notification
    }

    /** Update play pause view state in notification */
    fun updateNotification(
        context: Context,
        @DrawableRes playPauseResId: Int,
        title: String?
    ) {
        val notification = createNotification(context, playPauseResId, title)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.notify(MUSIC_PLAYER_NOTIFICATION_ID, notification)
    }
}
