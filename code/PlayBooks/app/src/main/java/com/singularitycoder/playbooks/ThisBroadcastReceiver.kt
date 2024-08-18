package com.singularitycoder.playbooks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.os.bundleOf
import com.singularitycoder.playbooks.helpers.IntentExtraKey
import com.singularitycoder.playbooks.helpers.IntentKey
import com.singularitycoder.playbooks.helpers.NotificationAction
import com.singularitycoder.playbooks.helpers.sendCustomBroadcast

class ThisBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = this::class.java.simpleName
    }

    /** Data sent to [HomeFragment] */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            IntentKey.NOTIFICATION_BUTTON_CLICK_BROADCAST -> {
                val actionExtra = intent.getStringExtra(IntentExtraKey.NOTIFICATION_BUTTON_CLICK_TYPE)
                when (actionExtra) {
                    NotificationAction.PLAY_PAUSE.name -> {
                        Log.d(TAG, "PLAY_PAUSE CLICK")
                    }

                    NotificationAction.PREVIOUS_SENTENCE.name -> {
                        Log.d(TAG, "PREVIOUS_SENTENCE CLICK")
                    }

                    NotificationAction.NEXT_SENTENCE.name -> {
                        Log.d(TAG, "NEXT_SENTENCE CLICK")
                    }

                    NotificationAction.PREVIOUS_PAGE.name -> {
                        Log.d(TAG, "PREVIOUS_PAGE CLICK")
                    }

                    NotificationAction.NEXT_PAGE.name -> {
                        Log.d(TAG, "NEXT_PAGE CLICK")
                    }
                }
                val packageName = intent.data?.encodedSchemeSpecificPart
                val bundle = bundleOf(IntentKey.NOTIFICATION_BUTTON_CLICK_BROADCAST to packageName)
                context.sendCustomBroadcast(action = intent.action, bundle = bundle)
            }

            else -> Unit
        }
    }
}