package com.singularitycoder.playbooks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.singularitycoder.playbooks.helpers.IntentExtraKey
import com.singularitycoder.playbooks.helpers.IntentKey

class ThisBroadcastReceiver : BroadcastReceiver() {

    /** Data sent to [PlayBookForegroundService] */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            IntentKey.NOTIF_BTN_CLICK_BROADCAST -> {
                val intent2 = Intent().apply {
                    action = IntentKey.NOTIF_BTN_CLICK_BROADCAST_2
                    putExtra(IntentExtraKey.NOTIF_BTN_CLICK_TYPE_2, intent.getStringExtra(IntentExtraKey.NOTIF_BTN_CLICK_TYPE))
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent2)
            }

            else -> Unit
        }
    }
}