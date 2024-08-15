package com.singularitycoder.playbooks

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.singularitycoder.playbooks.helpers.IntentExtraKey
import com.singularitycoder.playbooks.helpers.IntentExtraValue
import com.singularitycoder.playbooks.helpers.IntentKey
import com.singularitycoder.playbooks.helpers.NotificationAction
import com.singularitycoder.playbooks.helpers.NotificationsHelper
import com.singularitycoder.playbooks.helpers.TtsTag
import com.singularitycoder.playbooks.helpers.db.PlayBookDatabase
import com.singularitycoder.playbooks.helpers.showToast
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class PlayBookForegroundService : Service(), OnInitListener {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ThisEntryPoint {
        fun db(): PlayBookDatabase
//        fun networkStatus(): NetworkStatus
    }

    private var tts: TextToSpeech? = null

    private val availableLanguages = mutableListOf<Locale>()

    private val binder = LocalBinder()

    private val ttsParams = Bundle()

    private var localBroadcastManager: LocalBroadcastManager? = null

    private var bookDao: BookDao? = null
    private var bookDataDao: BookDataDao? = null

    private var bookId: String? = null
    private var currentBook: Book? = null
    private var currentBookData: BookData? = null

    inner class LocalBinder : Binder() {
        fun getService(): PlayBookForegroundService = this@PlayBookForegroundService
    }

    private val notificationButtonClickReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getStringExtra(IntentExtraKey.NOTIFICATION_BUTTON_CLICK_TYPE)
            when (action) {
                NotificationAction.PLAY_PAUSE.name -> {
                    Log.d(TAG, "PLAY_PAUSE CLICK")
                    showToast("PLAY_PAUSE")
//                    playOrPauseTrack()
                    playPause(isPlay = true)
                }

                NotificationAction.PREVIOUS_SENTENCE.name -> {
                    Log.d(TAG, "PREVIOUS_SENTENCE CLICK")
                    showToast("PREVIOUS_SENTENCE")
//                    rewindTrack()
                    previousSentence()
                }

                NotificationAction.NEXT_SENTENCE.name -> {
                    Log.d(TAG, "NEXT_SENTENCE CLICK")
                    showToast("NEXT_SENTENCE")
//                    forwardTrack()
                    nextSentence()
                }

                NotificationAction.PREVIOUS_PAGE.name -> {
                    Log.d(TAG, "PREVIOUS_PAGE CLICK")
                    showToast("PREVIOUS_PAGE")
//                    close()
                    previousPage()
                }

                NotificationAction.NEXT_PAGE.name -> {
                    Log.d(TAG, "NEXT_PAGE CLICK")
                    showToast("NEXT_PAGE")
//                    close()
                    nextPage()
                }
            }
        }
    }

    /** Foreground Service created */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate - First")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                /* receiver = */ notificationButtonClickReceiver,
                /* filter = */ IntentFilter(IntentKey.NOTIFICATION_BUTTON_CLICK_BROADCAST),
                /* flags = */ RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                /* receiver = */ notificationButtonClickReceiver,
                /* filter = */ IntentFilter(IntentKey.NOTIFICATION_BUTTON_CLICK_BROADCAST)
            )
        }
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        ttsParams.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM.toString())
        tts = TextToSpeech(/* context = */ this, /* listener = */ this)
        val appContext = this.applicationContext ?: throw IllegalStateException()
        val dbEntryPoint = EntryPointAccessors.fromApplication(appContext, com.singularitycoder.playbooks.PdfToTextWorker.ThisEntryPoint::class.java)
        bookDao = dbEntryPoint.db().bookDao()
        bookDataDao = dbEntryPoint.db().bookDataDao()
        // play book
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand - Second")
        bookId = intent?.getStringExtra(IntentExtraKey.BOOK_ID)
        CoroutineScope(Dispatchers.IO).launch {
            currentBook = bookDao?.getItemById(bookId ?: "")
            currentBookData = bookDataDao?.getItemById(bookId ?: "")

            withContext(Dispatchers.Main) {
                startAsForegroundService()
            }
        }
        // init TTS
        return super.onStartCommand(intent, flags, startId)
    }

    /** Return the communication channel to the service. */
    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind - third")
        return binder
    }

    /** Foreground Service destroyed */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        unregisterReceiver(notificationButtonClickReceiver)
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

            NotificationsHelper.createNotificationChannel(this@PlayBookForegroundService)
            NotificationsHelper.createNotificationLayout(this@PlayBookForegroundService)
            val notification = NotificationsHelper.createNotification(
                context = this@PlayBookForegroundService,
                playPauseResId = R.drawable.round_pause_24,
                title = currentBook?.title
            )
            ServiceCompat.startForeground(
                /* service = */ this@PlayBookForegroundService,
                /* id = */ 1, // Cannot be 0
                /* notification = */ notification,
                /* foregroundServiceType = */ ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
//            NotificationsHelper.updateNotification(context = this, playPauseResId = R.drawable.round_play_arrow_24)
        } catch (e: Exception) {
            if (e is ForegroundServiceStartNotAllowedException) {
                // App not in a valid state to start foreground service
                // (e.g. started from bg)
            }
        }
    }

    private fun sendBroadcastToMain(key: String) {
        val intent = Intent(IntentKey.MAIN_BROADCAST).apply {
            putExtra(IntentExtraKey.MESSAGE, key)
        }
        localBroadcastManager?.sendBroadcast(intent)
    }

    /**
     * Stops the foreground service and removes the notification.
     * Can be called from inside or outside the service.
     */
    fun stopForegroundService() {
        sendBroadcastToMain(IntentExtraValue.UNBIND)
        tts?.shutdown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onInit(p0: Int) {
        print("Text-To-Speech engine is ready.")

        // setOnUtteranceProgressListener must be set after tts is init
        // https://stackoverflow.com/questions/52233235/setonutteranceprogresslistener-not-at-all-working-for-text-to-speech-for-api-2
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) = Unit

            override fun onDone(uttId: String?) {
                if (uttId == TtsTag.UID_SPEAK) {
                    // do something
                }
            }

            override fun onError(uttId: String?) = Unit
        })
    }

    private fun speak(startIndex: Int, endIndex: Int) {
        val text = if (endIndex - startIndex > TextToSpeech.getMaxSpeechInputLength()) {
            "Sentence is too long."
        } else {
//            currentPlayingBookData?.text?.subSequence(startIndex, endIndex)
            ""
        }
        tts?.speak(
            /* text = */ text,
            /* queueMode = */ TextToSpeech.QUEUE_FLUSH,
            /* params = */ ttsParams,
            /* utteranceId = */ TtsTag.UID_SPEAK
        )
    }

    private fun speak2(charSequence: String) {
        val position: Int = 0

        val sizeOfChar = charSequence.length
        val testStr = charSequence.substring(position, sizeOfChar)

        var next = 20
        var pos = 0
        while (true) {
            var temp = ""
            Log.e("in loop", "" + pos)

            try {
                temp = testStr.substring(pos, next)
                val params = HashMap<String, String>()
                params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = temp
                tts?.speak(temp, TextToSpeech.QUEUE_ADD, params)

                pos += 20
                next += 20
            } catch (e: java.lang.Exception) {
                temp = testStr.substring(pos, testStr.length)
                tts?.speak(temp, TextToSpeech.QUEUE_ADD, null)
                break
            }
        }
    }

    fun splitText(text: String) {
        val length: Int = TextToSpeech.getMaxSpeechInputLength() - 1
//        val chunks: Iterable<String> = Splitter.fixedLength(length).split(text)
//        Lists.newArrayList(chunks)
    }

    private fun doOnReadingDone() {
        // This is probably not necessary. Add directly to speak
        ttsParams.putString(
            TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
            "end of wakeup message ID"
        )
        tts?.speak(
            /* text = */ "text to speak",
            /* queueMode = */ TextToSpeech.QUEUE_ADD,
            /* params = */ ttsParams,
            /* utteranceId = */ ""
        )
    }

    private fun saveAsAudioFile() {
        val wakeUpText = "Are you up yet?"
        val destFile = File("/sdcard/myAppCache/wakeUp.wav")
        ttsParams.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, wakeUpText) // this is unnecessary. Set in synthesizeToFile param utteranceId
        tts?.synthesizeToFile(
            /* text = */ wakeUpText,
            /* params = */ ttsParams,
            /* file = */ destFile,
            /* utteranceId = */ ""
        )
    }

    fun getTts(): TextToSpeech? = tts

    fun playPause(isPlay: Boolean) {
        if (tts?.isSpeaking == true || isPlay.not()) {
            tts?.stop()
            NotificationsHelper.updateNotification(
                context = this,
                playPauseResId = R.drawable.round_play_arrow_24,
                title = currentBook?.title
            )
        } else {
//            speak(startIndex = 0, endIndex = currentPlayingBookData?.periodPositionsList?.firstOrNull() ?: 0)
            NotificationsHelper.updateNotification(
                context = this,
                playPauseResId = R.drawable.round_pause_24,
                title = currentBook?.title
            )
        }
    }

    fun nextSentence() {

    }

    fun previousSentence() {

    }

    fun nextPage() {

    }

    fun previousPage() {

    }
}