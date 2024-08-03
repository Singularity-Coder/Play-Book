package com.singularitycoder.playbooks

import android.os.Bundle
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.singularitycoder.playbooks.databinding.ActivityMainBinding
import com.singularitycoder.playbooks.helpers.FragmentsTag
import com.singularitycoder.playbooks.helpers.showScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

//    @Inject
//    lateinit var connectMeDatabase: ConnectMeDatabase

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
            fragment = MainFragment.newInstance(""),
            tag = FragmentsTag.MAIN,
            isAdd = true,
            isAddToBackStack = false
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // https://stackoverflow.com/questions/50372487/android-room-database-file-is-empty-db-db-shm-db-wal
//        connectMeDatabase.close() // close the primary database to ensure all the transactions are merged
    }
}