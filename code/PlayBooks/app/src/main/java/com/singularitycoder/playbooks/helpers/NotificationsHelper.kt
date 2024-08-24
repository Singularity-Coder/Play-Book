package com.singularitycoder.playbooks.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
//        channel.setSound(null, null)
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

    private var notificationLayoutBig: RemoteViews? = null
    private var notificationLayoutSmall: RemoteViews? = null

    fun createNotificationLayout(context: Context) {
        /** RemoteViews only accepts old layouts like Linear, Relative, etc. It cannot render constraintLayout
         * https://developer.android.com/develop/ui/views/notifications/build-notification
         * https://stackoverflow.com/questions/45396426/crash-when-using-constraintlayout-in-notification
         * https://developer.android.com/develop/ui/views/notifications/custom-notification */
        notificationLayoutBig = RemoteViews(context.packageName, R.layout.custom_service_notification_large)
        notificationLayoutSmall = RemoteViews(context.packageName, R.layout.custom_service_notification_small)

        fun setPendingIntentBigNotif(
            notificationAction: NotificationAction,
            @IntegerRes viewId: Int
        ): PendingIntent {
            val intent = Intent(context, ThisBroadcastReceiver::class.java).apply {
                action = IntentKey.NOTIF_BTN_CLICK_BROADCAST
                putExtra(IntentExtraKey.NOTIF_BTN_CLICK_TYPE, notificationAction.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                /* context = */ context,
                /* requestCode = */ notificationAction.ordinal,
                /* intent = */ intent,
                /* flags = */ PendingIntent.FLAG_IMMUTABLE
            )
            notificationLayoutBig?.setOnClickPendingIntent(viewId, pendingIntent)
            return pendingIntent
        }

        fun setPendingIntentSmallNotif(
            notificationAction: NotificationAction,
            @IntegerRes viewId: Int
        ): PendingIntent {
            val intent = Intent(context, ThisBroadcastReceiver::class.java).apply {
                action = IntentKey.NOTIF_BTN_CLICK_BROADCAST
                putExtra(IntentExtraKey.NOTIF_BTN_CLICK_TYPE, notificationAction.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                /* context = */ context,
                /* requestCode = */ notificationAction.ordinal,
                /* intent = */ intent,
                /* flags = */ PendingIntent.FLAG_IMMUTABLE
            )
            notificationLayoutSmall?.setOnClickPendingIntent(viewId, pendingIntent)
            return pendingIntent
        }

        setPendingIntentSmallNotif(notificationAction = NotificationAction.PLAY_PAUSE, viewId = R.id.iv_play_pause_small)

        setPendingIntentBigNotif(notificationAction = NotificationAction.NEXT_PAGE, viewId = R.id.iv_next_page)
        setPendingIntentBigNotif(notificationAction = NotificationAction.PREVIOUS_PAGE, viewId = R.id.iv_previous_page)
        setPendingIntentBigNotif(notificationAction = NotificationAction.PLAY_PAUSE, viewId = R.id.iv_play_pause)
        setPendingIntentBigNotif(notificationAction = NotificationAction.NEXT_SENTENCE, viewId = R.id.iv_next_sentence)
        setPendingIntentBigNotif(notificationAction = NotificationAction.PREVIOUS_SENTENCE, viewId = R.id.iv_previous_sentence)
    }

    fun createNotification(
        context: Context,
        @DrawableRes playPauseResId: Int,
        title: String?,
        image: Bitmap?
    ): Notification {
        notificationLayoutBig?.setTextViewText(R.id.tv_notification_track_title, title ?: ":)")
        notificationLayoutBig?.setImageViewResource(R.id.iv_play_pause, playPauseResId)

        notificationLayoutSmall?.setImageViewBitmap(R.id.iv_image, image)
        notificationLayoutSmall?.setTextViewText(R.id.tv_notification_track_title, title ?: ":)")
        notificationLayoutSmall?.setImageViewResource(R.id.iv_play_pause_small, playPauseResId)

        val pendingIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ Intent(context, MainActivity::class.java),
            /* flags = */ PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
//            .setVibrate()
//            .setSound()
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayoutSmall)
            .setCustomBigContentView(notificationLayoutBig)
//            .setOngoing(true)
//            .setContent(notificationLayout)
            .setContentIntent(pendingIntent)
            .build()

        return notification
    }

    /** Update play pause view state in notification */
    fun updateNotification(
        context: Context,
        @DrawableRes playPauseResId: Int,
        title: String?,
        image: Bitmap?
    ) {
        val notification = createNotification(context, playPauseResId, title, image)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.notify(MUSIC_PLAYER_NOTIFICATION_ID, notification)
    }
}
