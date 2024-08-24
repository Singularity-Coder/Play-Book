package com.singularitycoder.playbooks

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.singularitycoder.playbooks.helpers.IntentExtraKey
import com.singularitycoder.playbooks.helpers.IntentExtraValue
import com.singularitycoder.playbooks.helpers.IntentKey
import com.singularitycoder.playbooks.helpers.NotificationAction
import com.singularitycoder.playbooks.helpers.NotificationsHelper
import com.singularitycoder.playbooks.helpers.TtsConstants
import com.singularitycoder.playbooks.helpers.TtsTag
import com.singularitycoder.playbooks.helpers.db.PlayBookDatabase
import com.singularitycoder.playbooks.helpers.getBookCoversFileDir
import com.singularitycoder.playbooks.helpers.toBitmap
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

class PlayBookForegroundService : Service() {
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
    private var currentPlayingBook: Book? = null
    private var currentPlayingBookData: BookData? = null

    private var currentPeriodPosition: Int = -1
    private var currentPagePosition: Int = 0

    private var currentlyPlayingText: CharSequence? = null

    private var bookCoverBitmap: Bitmap? = null

    /** This receiver should be here as when app is killed this service must be self sufficient and cannot depend on killed app resources */
    private val notificationButtonClickReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != IntentKey.NOTIF_BTN_CLICK_BROADCAST_2) return
            val actionExtra = intent.getStringExtra(IntentExtraKey.NOTIF_BTN_CLICK_TYPE_2)
            when (actionExtra) {
                NotificationAction.PLAY_PAUSE.name -> {
                    Log.d(TAG, "PLAY_PAUSE CLICK")
                    playPauseTts()
                }

                NotificationAction.PREVIOUS_SENTENCE.name -> {
                    Log.d(TAG, "PREVIOUS_SENTENCE CLICK")
                    previousSentence()
                }

                NotificationAction.NEXT_SENTENCE.name -> {
                    Log.d(TAG, "NEXT_SENTENCE CLICK")
                    nextSentence()
                }

                NotificationAction.PREVIOUS_PAGE.name -> {
                    Log.d(TAG, "PREVIOUS_PAGE CLICK")
                    previousPage()
                }

                NotificationAction.NEXT_PAGE.name -> {
                    Log.d(TAG, "NEXT_PAGE CLICK")
                    nextPage()
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlayBookForegroundService = this@PlayBookForegroundService
    }

    /** Foreground Service created */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate - First")
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager?.registerReceiver(
            /* receiver = */ notificationButtonClickReceiver,
            /* filter = */ IntentFilter(IntentKey.NOTIF_BTN_CLICK_BROADCAST_2)
        )
        ttsParams.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM.toString())
        val appContext = this.applicationContext ?: throw IllegalStateException()
        val dbEntryPoint = EntryPointAccessors.fromApplication(appContext, com.singularitycoder.playbooks.PdfToTextWorker.ThisEntryPoint::class.java)
        bookDao = dbEntryPoint.db().bookDao()
        bookDataDao = dbEntryPoint.db().bookDataDao()
        // play book
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand - Second")
        bookId = intent?.getStringExtra(IntentExtraKey.BOOK_ID)
        tts = TextToSpeech(
            /* context = */ this,
            /* listener = */ object : TextToSpeech.OnInitListener {
                override fun onInit(p0: Int) {
                    doWhenTtsIsReady()
                }
            }
        )

        return super.onStartCommand(intent, flags, startId)
    }

    private fun doWhenTtsIsReady() {
        print("Text-To-Speech engine is ready.")

        tts?.availableLanguages?.forEach { locale: Locale ->
            if (tts?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                availableLanguages.add(locale)
            }
        }
        tts?.setLanguage(Locale.US)
        setTtsPitch(TtsConstants.DEFAULT.toFloat())
        setTtsSpeechRate(TtsConstants.DEFAULT.toFloat())

        // setOnUtteranceProgressListener must be set after tts is init
        // https://stackoverflow.com/questions/52233235/setonutteranceprogresslistener-not-at-all-working-for-text-to-speech-for-api-2
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) = Unit

            override fun onDone(uttId: String?) {
                if (uttId == TtsTag.UID_SPEAK) {
                    // do something
                    nextSentence()
                }
            }

            override fun onError(uttId: String?) = Unit

            override fun onRangeStart(
                utteranceId: String?,
                start: Int,
                end: Int,
                frame: Int
            ) {
                val intent = Intent(IntentKey.MAIN_BROADCAST_FROM_SERVICE).apply {
                    putExtra(IntentExtraKey.MESSAGE, IntentExtraValue.TTS_WORD_HIGHLIGHT)
                    putExtra(IntentExtraKey.TTS_WORD_HIGHLIGHT_START, start)
                    putExtra(IntentExtraKey.TTS_WORD_HIGHLIGHT_END, end)
                }
                localBroadcastManager?.sendBroadcast(intent)
            }
        })

        loadData(bookId)
        startAsForegroundService()

        sendBroadcastToMain(IntentExtraValue.TTS_READY)
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
        localBroadcastManager?.unregisterReceiver(notificationButtonClickReceiver)
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
                title = ":)",
                image = null
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

    fun loadData(bookId: String?) {
        this.bookId = bookId
        currentPeriodPosition = -1
        CoroutineScope(Dispatchers.IO).launch {
            currentPlayingBook = bookDao?.getItemById(bookId ?: "")
            currentPlayingBookData = bookDataDao?.getItemById(bookId ?: "")
            bookCoverBitmap = File(
                /* parent = */ this@PlayBookForegroundService.getBookCoversFileDir(),
                /* child = */ "${currentPlayingBook?.id}.jpg"
            ).toBitmap()

            withContext(Dispatchers.Main) {
                sendBroadcastToMain(IntentExtraValue.FOREGROUND_SERVICE_READY)
                speak(
                    startIndex = 0,
                    endIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(0) ?: 0
                )
                updatePlayerPlayingState()
            }
        }
    }

    private fun sendBroadcastToMain(key: String) {
        val intent = Intent(IntentKey.MAIN_BROADCAST_FROM_SERVICE).apply {
            putExtra(IntentExtraKey.MESSAGE, key)
        }
        localBroadcastManager?.sendBroadcast(intent)
    }

    /**
     * Stops the foreground service and removes the notification.
     * Can be called from inside or outside the service.
     */
    fun stopForegroundService() {
        tts?.shutdown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun speak(startIndex: Int, endIndex: Int) {
//        var newStartIndex = startIndex
//        var newEndIndex = endIndex
//        if (newStartIndex == 0 || newEndIndex == 0) {
//            currentPeriodPosition += 0
//            newStartIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition) ?: 0
//            newEndIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition + 1) ?: 0
//        }
//        Log.d("POSITIONSSSSSS:", "$newStartIndex $newEndIndex")
        CoroutineScope(Dispatchers.Main).launch {
            val text = try {
                if (endIndex - startIndex > TextToSpeech.getMaxSpeechInputLength()) {
                    "Sentence is too long. Skipping to next sentence."
                } else {
                    /** startIndex + 1 to avoid reading periods "." */
                    val modifiedStartIndex = if (currentPlayingBookData?.text?.get(startIndex) == '.') {
                        startIndex + 1
                    } else {
                        startIndex
                    }
                    currentPlayingBookData?.text?.subSequence(modifiedStartIndex, endIndex)
                }
            } catch (_: Exception) {
                ""
            }
            currentlyPlayingText = text
            sendBroadcastToMain(IntentExtraValue.READING_COMPLETE)
            tts?.speak(
                /* text = */ text,
                /* queueMode = */ TextToSpeech.QUEUE_FLUSH,
                /* params = */ ttsParams,
                /* utteranceId = */ TtsTag.UID_SPEAK
            )
        }
    }

    fun setTtsPitch(pitch: Float) {
        tts?.setPitch(pitch / 5)
    }

    fun setTtsSpeechRate(speed: Float) {
        tts?.setSpeechRate(speed / 5)
    }

    fun setTtsLanguage(locale: Locale) {
        tts?.setLanguage(locale)
    }

    fun playPauseTts() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
            updatePlayerPausedState()
        } else {
            speak(
                startIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition) ?: 0,
                endIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition + 1) ?: 0
            )
            updatePlayerPlayingState()
        }
    }

    private fun updatePlayerPlayingState() {
        sendBroadcastToMain(IntentExtraValue.TTS_PLAYING)
        NotificationsHelper.updateNotification(
            context = this,
            playPauseResId = R.drawable.round_pause_24,
            title = currentPlayingBook?.title,
            image = bookCoverBitmap
        )
    }

    private fun updatePlayerPausedState() {
        sendBroadcastToMain(IntentExtraValue.TTS_PAUSED)
        NotificationsHelper.updateNotification(
            context = this,
            playPauseResId = R.drawable.round_play_arrow_24,
            title = currentPlayingBook?.title,
            image = bookCoverBitmap
        )
    }

    fun stopTts() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    fun stopAndPlayTts() {
        stopTts()
        speak(
            startIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition) ?: 0,
            endIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition + 1) ?: 0
        )
        updatePlayerPlayingState()
    }

    fun nextSentence() {
        currentPeriodPosition += 1
        speak(
            startIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition) ?: 0,
            endIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition + 1) ?: 0
        )
        updatePlayerPlayingState()
    }

    fun previousSentence() {
        currentPeriodPosition -= 1
        speak(
            startIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition - 1) ?: 0,
            endIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition) ?: 0
        )
        updatePlayerPlayingState()
    }

    fun nextPage(pagePosition: Int? = null) {
        if (pagePosition != null) {
            currentPeriodPosition += pagePosition
        } else {
            currentPeriodPosition += 3
        }
        speak(
            startIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition) ?: 0,
            endIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition + 1) ?: 0
        )
//        currentPeriodPosition += currentPlayingBookData?.periodCountPerPageList?.find { it >= currentPeriodPosition } ?: 0
//        speak(
//            startIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition) ?: 0,
//            endIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition + 1) ?: 0
//        )
        updatePlayerPlayingState()
    }

    fun previousPage(pagePosition: Int? = null) {
        if (pagePosition != null) {
            currentPeriodPosition -= pagePosition
        } else {
            currentPeriodPosition -= 3
        }
        speak(
            startIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition - 1) ?: 0,
            endIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition) ?: 0
        )
//        currentPeriodPosition -= currentPlayingBookData?.periodCountPerPageList?.find { it <= currentPeriodPosition } ?: 0
//        speak(
//            startIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition) ?: 0,
//            endIndex = currentPlayingBookData?.periodPositionsList?.getOrNull(currentPeriodPosition + 1) ?: 0
//        )
        updatePlayerPlayingState()
    }

    /** Any call to speak() for the same string content as wakeUpText will result in the playback of destFileName.
     * This is done to avoid synthesizing text in tts again and save resources.
     * You can provide custom audio file as path as well */
    private fun playSavedAudioFile() {
        val wakeUpText = "Are you up yet?"
        val destFile = File("/sdcard/myAppCache/wakeUp.wav")
        tts?.addSpeech(wakeUpText, destFile)
    }

    fun saveAsAudioFile() {
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

    fun getAvailableTtsLanguages(): List<Locale> = availableLanguages

    fun getCurrentPlayingBook(): Book? = currentPlayingBook

    fun getCurrentPeriodPosition(): Int = currentPeriodPosition

    fun getCurrentlyPlayingText(): CharSequence? = currentlyPlayingText
}