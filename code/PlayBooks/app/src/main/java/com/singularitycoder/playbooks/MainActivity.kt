package com.singularitycoder.playbooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.singularitycoder.playbooks.databinding.ActivityMainBinding
import com.singularitycoder.playbooks.helpers.FragmentsTag
import com.singularitycoder.playbooks.helpers.IntentExtraKey
import com.singularitycoder.playbooks.helpers.NotificationAction
import com.singularitycoder.playbooks.helpers.db.PlayBooksDatabase
import com.singularitycoder.playbooks.helpers.showScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = this::class.java.simpleName
    }

    @Inject
    lateinit var playBooksDatabase: PlayBooksDatabase

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_700)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        showScreen(
            fragment = MainFragment.newInstance(),
            tag = FragmentsTag.MAIN,
            isAdd = true,
            isAddToBackStack = false
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.getStringExtra(IntentExtraKey.NOTIF_BTN_CLICK_TYPE)
        when (action) {
            NotificationAction.PLAY_PAUSE.name -> {
                Log.d(TAG, "PLAY_PAUSE")
            }

            NotificationAction.PREVIOUS_SENTENCE.name -> {
                Log.d(TAG, "PREVIOUS_SENTENCE")
            }

            NotificationAction.NEXT_SENTENCE.name -> {
                Log.d(TAG, "NEXT_SENTENCE")
            }

            NotificationAction.PREVIOUS_PAGE.name -> {
                Log.d(TAG, "PREVIOUS_PAGE")
            }

            NotificationAction.NEXT_PAGE.name -> {
                Log.d(TAG, "NEXT_PAGE")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        /** Close the primary database to ensure all the transactions are merged.
         * This will mess up ur foreground service if u r using db in it
         * https://stackoverflow.com/questions/50372487/android-room-database-file-is-empty-db-db-shm-db-wal */
//        playBooksDatabase.close()
    }
}