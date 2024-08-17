package com.singularitycoder.playbooks

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.singularitycoder.playbooks.databinding.FragmentMainBinding
import com.singularitycoder.playbooks.helpers.IntentExtraKey
import com.singularitycoder.playbooks.helpers.IntentExtraValue
import com.singularitycoder.playbooks.helpers.IntentKey
import com.singularitycoder.playbooks.helpers.TtsTag
import com.singularitycoder.playbooks.helpers.WorkerData
import com.singularitycoder.playbooks.helpers.WorkerTag
import com.singularitycoder.playbooks.helpers.collectLatestLifecycleFlow
import com.singularitycoder.playbooks.helpers.deviceHeight
import com.singularitycoder.playbooks.helpers.dpToPx
import com.singularitycoder.playbooks.helpers.drawable
import com.singularitycoder.playbooks.helpers.getBookId
import com.singularitycoder.playbooks.helpers.getDownloadDirectory
import com.singularitycoder.playbooks.helpers.globalLayoutAnimation
import com.singularitycoder.playbooks.helpers.hasNotificationsPermission
import com.singularitycoder.playbooks.helpers.hasPdfs
import com.singularitycoder.playbooks.helpers.hasStoragePermission
import com.singularitycoder.playbooks.helpers.hideKeyboard
import com.singularitycoder.playbooks.helpers.layoutAnimationController
import com.singularitycoder.playbooks.helpers.onImeClick
import com.singularitycoder.playbooks.helpers.onSafeClick
import com.singularitycoder.playbooks.helpers.requestStoragePermission
import com.singularitycoder.playbooks.helpers.setMargins
import com.singularitycoder.playbooks.helpers.setNavigationBarColor
import com.singularitycoder.playbooks.helpers.shouldShowRationaleFor
import com.singularitycoder.playbooks.helpers.showAlertDialog
import com.singularitycoder.playbooks.helpers.showAppSettings
import com.singularitycoder.playbooks.helpers.showPopupMenuWithIcons
import com.singularitycoder.playbooks.helpers.showSingleSelectionPopupMenu
import com.singularitycoder.playbooks.helpers.showTtsSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

const val ARG_PARAM_SCREEN_TYPE = "ARG_PARAM_SCREEN_TYPE"

@AndroidEntryPoint
class MainFragment : Fragment(), OnInitListener {

    companion object {
        private val TAG = this::class.java.simpleName

        @JvmStatic
        fun newInstance(screenType: String) = MainFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PARAM_SCREEN_TYPE, screenType)
            }
        }
    }

    private var topicParam: String? = null

    private val booksAdapter = DownloadsAdapter()

    private var booksList = listOf<Book?>()

    private lateinit var binding: FragmentMainBinding

    private var tts: TextToSpeech? = null

    private val availableLanguages = mutableListOf<Locale>()

    private val ttsParams = Bundle()

    private var selectedTtsLanguage = Locale.getDefault().displayName

    private val bookViewModel by viewModels<BookViewModel>()

//    private var currentBookPosition = 0

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<MaterialCardView>

    private var bookLoadingSnackBar: Snackbar? = null

    private var currentPlayingBook: Book? = null
    private var currentPlayingBookData: BookData? = null
    private var currentPeriodPosition: Int = 0
    private var currentPagePosition: Int = 0

    private var playBookForegroundService: PlayBookForegroundService? = null

    private var isServiceBound = false

    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val msg = intent.getStringExtra(IntentExtraKey.MESSAGE)
            when (msg) {
                IntentExtraValue.TTS_READY -> {
                    Log.d(TAG, "PREPARED broadcast received")
//                    trackPrepared()
                }

                IntentExtraValue.READING_COMPLETE -> {
                    Log.d(TAG, "COMPLETION broadcast received")
//                    val newTrackIndex = intent.getIntExtra(Constants.CURRENT_TRACK_KEY, trackList.indexOf(currentTrack))
//                    trackCompletion(newTrackIndex)
                }

                IntentExtraValue.UPDATE_PROGRESS -> {
                    Log.d(TAG, "UPDATE PROGRESS broadcast received")
//                    updateProgressBar()
                }

                IntentExtraValue.UNBIND -> {
                    Log.d(TAG, "UNBIND REQ broadcast received")
//                    unbind()
                }

                IntentExtraValue.SERVICE_DESTROYED -> {
//                    onServiceDestroy()
                }

                IntentExtraValue.TTS_PAUSED -> {
//                    onTrackPaused()
                }
            }
        }
    }

    // needed to communicate with the service.
    private val ttsConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // we've bound to ExampleLocationForegroundService, cast the IBinder and get ExampleLocationForegroundService instance.
            print("onServiceConnected")
            val binder = service as PlayBookForegroundService.LocalBinder
            playBookForegroundService = binder.getService()
            isServiceBound = true
//            onServiceConnected()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // This is called when the connection with the service has been disconnected. Clean up.
            print("onServiceDisconnected")
            isServiceBound = false
            playBookForegroundService = null
        }
    }

    @SuppressLint("InlinedApi")
    private val notificationPermissionResult = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean? ->
        isGranted ?: return@registerForActivityResult
        if (isGranted.not()) {
            askNotificationPermission()
            val accessFineLocationNeedsRationale = activity?.shouldShowRationaleFor(android.Manifest.permission.POST_NOTIFICATIONS) == true
            if (accessFineLocationNeedsRationale) {
                requireContext().showAlertDialog(
                    title = "Grant permission",
                    message = "You must grant notification permission to play E-Books.",
                    positiveBtnText = "Settings",
                    negativeBtnText = "Cancel",
                    positiveAction = {
                        activity?.showAppSettings()
                    }
                )
            }
            return@registerForActivityResult
        }

        if (booksAdapter.bookList.isEmpty()) loadPdfs()
    }

    private val ttsLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            // success, create the TTS instance
            tts = TextToSpeech(context, this)
        } else {
            // missing data, install it
            val intent = Intent().apply {
                action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        topicParam = arguments?.getString(ARG_PARAM_SCREEN_TYPE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setupUI()
        binding.setupUserActionListeners()
        observeForData()
    }

    override fun onResume() {
        super.onResume()
        loadPdfs()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(messageReceiver, IntentFilter(IntentKey.MAIN_BROADCAST))
    }

    override fun onPause() {
        super.onPause()
        if (isServiceBound) {
            activity?.unbindService(ttsConnection)
            isServiceBound = false
        }

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(messageReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.unbindService(ttsConnection)
    }

    override fun onInit(p0: Int) {
        print("Text-To-Speech engine is ready.")
        tts?.availableLanguages?.forEach { locale: Locale ->
            if (tts?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                availableLanguages.add(locale)
            }
        }
        tts?.setLanguage(Locale.US)

        // setOnUtteranceProgressListener must be set after tts is init
        // https://stackoverflow.com/questions/52233235/setonutteranceprogresslistener-not-at-all-working-for-text-to-speech-for-api-2
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) = Unit

            override fun onDone(uttId: String?) {
                binding.layoutPersistentBottomSheet.ivPlay.setImageDrawable(context?.drawable(R.drawable.round_play_arrow_24))
                binding.layoutPersistentBottomSheet.ivHeaderPlay.setImageDrawable(context?.drawable(R.drawable.round_play_arrow_24))
                if (uttId == TtsTag.UID_SPEAK) {
                    // do something
                }
            }

            override fun onError(uttId: String?) = Unit
        })
    }

    private fun FragmentMainBinding.setupUI() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.layoutPersistentBottomSheet.root)
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        requireActivity().setNavigationBarColor(R.color.white)
        rvDownloads.apply {
            layoutAnimation = rvDownloads.context.layoutAnimationController(globalLayoutAnimation)
            layoutManager = LinearLayoutManager(context)
            adapter = booksAdapter
        }
//        ivShield.setMargins(top = (deviceHeight() / 2) - 200.dpToPx().toInt())
        layoutSearch.etSearch.hint = "Search in ${getDownloadDirectory().name}"
        setUpPersistentBottomSheet()

        ttsParams.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM.toString())
        checkTtsExists()

        layoutPersistentBottomSheet.layoutSliderPitch.tvSliderTitle.text = "Pitch"
        layoutPersistentBottomSheet.layoutSliderSpeed.tvSliderTitle.text = "Speed"
        layoutPersistentBottomSheet.layoutSliderPlayback.tvSliderTitle.text = "Page"
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun FragmentMainBinding.setupUserActionListeners() {
        root.setOnClickListener {}

        layoutPersistentBottomSheet.root.setOnClickListener {}

        progressCircular.viewTreeObserver.addOnGlobalLayoutListener {
            if (progressCircular.isVisible.not()) {
                dismissBookLoadingSnackbar()
            }
        }

        ivHeaderMore.onSafeClick { view: Pair<View?, Boolean> ->
            val optionsList = listOf(
                Pair("Refresh", R.drawable.round_refresh_24),
                Pair("Delete all books", R.drawable.outline_delete_24)
            )
            requireContext().showPopupMenuWithIcons(
                view = view.first,
                menuList = optionsList,
                customColor = R.color.md_red_700,
                customColorItemText = optionsList.last().first
            ) { it: MenuItem? ->
                when (it?.title?.toString()?.trim()) {
                    optionsList[0].first -> {
                        loadPdfs()
                    }

                    optionsList[1].first -> {
                        requireContext().showAlertDialog(
                            title = "Delete all books",
                            message = "Don't worry. The files on your device won't be deleted.",
                            positiveBtnText = "Delete All",
                            negativeBtnText = "Cancel",
                            positiveBtnColor = R.color.md_red_700,
                            positiveAction = {
                                stopPlayer()
                                bookViewModel.deleteAllBookDataItems()
                                bookViewModel.deleteAllBookItems()
                                booksAdapter.notifyDataSetChanged()
                            }
                        )
                    }
                }
            }
        }

        booksAdapter.setOnItemClickListener { book, position ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (activity?.hasNotificationsPermission()?.not() == true) {
                    askNotificationPermission()
                    return@setOnItemClickListener
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                val bookData = bookViewModel.getBookDataItemById(File(book?.path ?: "").getBookId())
                currentPlayingBook = book
                currentPlayingBookData = bookData

                withContext(Dispatchers.Main) {
                    layoutPersistentBottomSheet.layoutSliderPlayback.apply {
                        sliderCustom.max = currentPlayingBook?.pageCount ?: 0
                        tvValue.text = "${sliderCustom.progress}/${currentPlayingBook?.pageCount}"
                    }
                    layoutPersistentBottomSheet.root.isVisible = true
                    layoutPersistentBottomSheet.tvHeader.text = book?.title
                    layoutPersistentBottomSheet.tvCurrentlyReading.text = bookData.text?.subSequence(
                        startIndex = 0,
                        endIndex = bookData.periodPositionsList.firstOrNull() ?: 0
                    )
                    speak(startIndex = 0, endIndex = bookData.periodPositionsList.firstOrNull() ?: 0)
                    startForegroundService(book?.id ?: "")
                    playBookForegroundService?.playPause(isPlay = true)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        booksAdapter.setOnItemLongClickListener { book, view, position ->
            val optionsList = listOf(
                Pair("Open", R.drawable.round_open_in_new_24),
                Pair("Delete", R.drawable.outline_delete_24)
            )
            requireContext().showPopupMenuWithIcons(
                view = view,
                menuList = optionsList,
                customColor = R.color.md_red_700,
                customColorItemText = optionsList.last().first
            ) { it: MenuItem? ->
                when (it?.title?.toString()?.trim()) {
                    optionsList[0].first -> {
                        val file = File(book?.path ?: "")
                        val path = Uri.fromFile(file)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            setDataAndType(path, "application/pdf")
                        }
                        val chooserIntent = Intent.createChooser(intent, "Open with...").apply {
                            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intent))
                        }
                        try {
                            startActivity(chooserIntent)
                        } catch (_: ActivityNotFoundException) {
                        }
                    }

                    optionsList[1].first -> {
                        requireContext().showAlertDialog(
                            title = "Delete Book",
                            message = "${book?.title} \n\nDon't worry. The file on your device won't be deleted.",
                            positiveBtnText = "Delete",
                            negativeBtnText = "Cancel",
                            positiveBtnColor = R.color.md_red_700,
                            positiveAction = {
                                bookViewModel.deleteBookDataItem(book)
                                bookViewModel.deleteBookItem(book)
                                booksAdapter.notifyItemRemoved(position ?: 0)
                            }
                        )
                    }
                }
            }
        }

        layoutGrantPermission.btnGivePermission.onSafeClick {
            when (layoutGrantPermission.tvTitle.text) {
                getString(R.string.grant_notification_permission) -> {
                    askNotificationPermission()
                }

                getString(R.string.grant_storage_permission) -> {
                    requireActivity().requestStoragePermission()
                }

                else -> Unit
            }
        }

//        fabSearch.onSafeClick {
//        }

        layoutSearch.etSearch.onImeClick {
            layoutSearch.etSearch.hideKeyboard()
        }

        layoutSearch.ibClearSearch.onSafeClick {
            layoutSearch.etSearch.setText("")
        }

        layoutSearch.etSearch.doAfterTextChanged { query: Editable? ->
            layoutSearch.ibClearSearch.isVisible = query.isNullOrBlank().not()
            if (query.isNullOrBlank()) {
                booksAdapter.bookList = booksList
                booksAdapter.notifyDataSetChanged()
                return@doAfterTextChanged
            }
            booksAdapter.bookList = booksList.filter { it?.title?.contains(other = query, ignoreCase = true) == true }
            booksAdapter.notifyDataSetChanged()
        }

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {}

                    BottomSheetBehavior.STATE_EXPANDED -> {
                        layoutPersistentBottomSheet.ivHeaderPlay.isVisible = false
                        layoutPersistentBottomSheet.ivHeaderMore.isVisible = true
                    }

                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        layoutPersistentBottomSheet.ivHeaderPlay.isVisible = false
                        layoutPersistentBottomSheet.ivHeaderMore.isVisible = false
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        layoutPersistentBottomSheet.ivHeaderPlay.isVisible = true
                        layoutPersistentBottomSheet.ivHeaderMore.isVisible = false
                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {
                        layoutPersistentBottomSheet.ivHeaderPlay.isVisible = false
                        layoutPersistentBottomSheet.ivHeaderMore.isVisible = false
                    }

                    BottomSheetBehavior.STATE_SETTLING -> {
                        layoutPersistentBottomSheet.ivHeaderPlay.isVisible = false
                        layoutPersistentBottomSheet.ivHeaderMore.isVisible = false
                    }

                    else -> Unit
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
        })

        layoutPersistentBottomSheet.tvHeader.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        layoutPersistentBottomSheet.ivHeaderPlay.setOnClickListener {
        }

//        binding.layoutPersistentBottomSheet.tvSelectLanguage.setOnClickListener {
//            val ttsLanguageList = TTS_LANGUAGE_LIST.map { it.displayName }
//            requireContext().showListPopupMenu2(
//                anchorView = binding.layoutPersistentBottomSheet.tvSelectLanguage,
//                menuList = ttsLanguageList
//            ) { position: Int ->
//                binding.layoutPersistentBottomSheet.tvSelectLanguage.text = "Language: ${ttsLanguageList[position]}"
//            }
//        }

        layoutPersistentBottomSheet.layoutSliderPitch.apply {
            ibReduce.setOnClickListener {
                sliderCustom.progress -= 1
                tts?.setPitch(sliderCustom.progress.toFloat())
                tvValue.text = sliderCustom.progress.toString()
            }
            ibIncrease.setOnClickListener {
                sliderCustom.progress += 1
                tts?.setPitch(sliderCustom.progress.toFloat())
                tvValue.text = sliderCustom.progress.toString()
            }
            sliderCustom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    println("seekbar progress: $progress")
                    tvValue.text = progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }

        layoutPersistentBottomSheet.layoutSliderSpeed.apply {
            ibReduce.setOnClickListener {
                sliderCustom.progress -= 1
                tts?.setSpeechRate(sliderCustom.progress.toFloat())
                tvValue.text = sliderCustom.progress.toString()
            }
            ibIncrease.setOnClickListener {
                sliderCustom.progress += 1
                tts?.setSpeechRate(sliderCustom.progress.toFloat())
                tvValue.text = sliderCustom.progress.toString()
            }
            sliderCustom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    println("seekbar progress: $progress")
                    tvValue.text = progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }

        layoutPersistentBottomSheet.layoutSliderPlayback.apply {
            sliderCustom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    println("seekbar progress: $progress")
                    tvValue.text = "${progress}/${currentPlayingBook?.pageCount}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }

        layoutPersistentBottomSheet.ivPlay.setOnClickListener {
//            if (tts?.isSpeaking == true) {
//                tts?.stop()
//            } else {
//                speak(startIndex = 0, endIndex = currentPlayingBookData?.periodPositionsList?.firstOrNull() ?: 0)
//            }
            if (playBookForegroundService?.getTts()?.isSpeaking == true) {
                playBookForegroundService?.playPause(isPlay = false)
            } else {
                playBookForegroundService?.playPause(isPlay = true)
            }
            setPlaybackViewState()
        }

        layoutPersistentBottomSheet.ivHeaderPlay.setOnClickListener {
            layoutPersistentBottomSheet.ivPlay.performClick()
        }

        layoutPersistentBottomSheet.ibNextSentence.setOnClickListener {
            playBookForegroundService?.nextSentence()
            tts?.speak("text to speak", TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        layoutPersistentBottomSheet.ibPreviousSentence.setOnClickListener {
            playBookForegroundService?.previousSentence()
            tts?.speak("text to speak", TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        layoutPersistentBottomSheet.ibNextPage.setOnClickListener {
            playBookForegroundService?.nextPage()
            tts?.speak("text to speak", TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        layoutPersistentBottomSheet.ibPreviousPage.setOnClickListener {
            playBookForegroundService?.previousPage()
            tts?.speak("text to speak", TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        layoutPersistentBottomSheet.ivHeaderMore.setOnClickListener { view: View? ->
            val ttsLanguageList = availableLanguages.map { Pair(it.displayName, R.drawable.round_check_24) }
            val optionsList = listOf(
                Pair("Select Language", R.drawable.round_language_24),
                Pair("Save as audio file", R.drawable.outline_audio_file_24),
                Pair("Settings", R.drawable.outline_settings_24),
                Pair("Stop Playing", R.drawable.outline_cancel_24),
            )
            requireContext().showPopupMenuWithIcons(
                view = layoutPersistentBottomSheet.ivHeaderMore,
                menuList = optionsList,
                customColor = R.color.md_red_700,
                customColorItemText = optionsList.last().first
            ) { it: MenuItem? ->
                when (it?.title?.toString()?.trim()) {
                    optionsList[0].first -> {
                        requireContext().showSingleSelectionPopupMenu(
                            view = layoutPersistentBottomSheet.ivHeaderMore,
                            title = "Select Language",
                            selectedOption = selectedTtsLanguage,
                            menuList = ttsLanguageList,
                        ) { menuItem: MenuItem? ->
                            selectedTtsLanguage = menuItem?.title?.toString()?.trim() ?: ""
                            tts?.setLanguage(Locale(selectedTtsLanguage))
                        }
                    }

                    optionsList[1].first -> {
//                        saveAsAudioFile()
                    }

                    optionsList[2].first -> {
                        activity?.showTtsSettings()
                    }

                    optionsList[3].first -> {
                        playBookForegroundService?.stopForegroundService()
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        layoutPersistentBottomSheet.root.isVisible = false
                    }
                }
            }
        }
    }

    private fun FragmentMainBinding.setPlaybackViewState() {
        if (playBookForegroundService?.getTts()?.isSpeaking == true) {
            layoutPersistentBottomSheet.ivPlay.setImageDrawable(context?.drawable(R.drawable.round_play_arrow_24))
            layoutPersistentBottomSheet.ivHeaderPlay.setImageDrawable(context?.drawable(R.drawable.round_play_arrow_24))
        } else {
            layoutPersistentBottomSheet.ivPlay.setImageDrawable(context?.drawable(R.drawable.round_pause_24))
            layoutPersistentBottomSheet.ivHeaderPlay.setImageDrawable(context?.drawable(R.drawable.round_pause_24))
        }
    }

    private fun speak(startIndex: Int, endIndex: Int) {
        val text = if (endIndex - startIndex > TextToSpeech.getMaxSpeechInputLength()) {
            "Sentence is too long."
        } else {
            currentPlayingBookData?.text?.subSequence(startIndex, endIndex)
        }
        tts?.speak(
            /* text = */ text,
            /* queueMode = */ TextToSpeech.QUEUE_FLUSH,
            /* params = */ ttsParams,
            /* utteranceId = */ TtsTag.UID_SPEAK
        )
    }

    @SuppressLint("InlinedApi")
    private fun askNotificationPermission() {
        notificationPermissionResult.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeForData() {
        (activity as? MainActivity)?.collectLatestLifecycleFlow(flow = bookViewModel.getAllBookItemsFlow()) { booksList: List<Book?> ->
//            pdfList.add(it.absolutePath)
//          requireActivity().openFile(it)
            this.booksList = booksList
            booksAdapter.bookList = booksList
            booksAdapter.notifyDataSetChanged()
//            booksAdapter.notifyItemInserted(currentBookPosition)
//            currentBookPosition++
//            binding.nestedScrollView.scrollTo(0, 0)
//            binding.rvDownloads.runLayoutAnimation(globalLayoutAnimation)
        }
    }

    private fun startForegroundService(bookId: String) {
        if (playBookForegroundService != null) return

        val intent = Intent(context, PlayBookForegroundService::class.java).apply {
            putExtra(IntentExtraKey.BOOK_ID, bookId)
        }
        activity?.application?.startForegroundService(intent)
        // bind to the service to update UI
        activity?.bindService(intent, ttsConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopForegroundService() {
        playBookForegroundService?.stopForegroundService()
    }

    private fun stopPlayer() {
        binding.layoutPersistentBottomSheet.root.isVisible = false
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadPdfs() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity?.hasNotificationsPermission()?.not() == true) {
                setPermissionView(
                    isShow = true,
                    icon = R.drawable.round_notifications_active_24,
                    title = getString(R.string.grant_notification_permission),
                    isShowButton = true
                )
                return
            }
        }

        if (activity?.hasStoragePermission() == true) {
            setPermissionView(
                isShow = hasPdfs().not(),
                icon = R.drawable.round_menu_book_24,
                title = "No books found",
                isShowButton = false
            )
            if (hasPdfs().not()) return

            if (booksAdapter.bookList.isEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
                    bookLoadingSnackBar = Snackbar.make(binding.root, "Books are loading, please wait.", Snackbar.LENGTH_INDEFINITE).apply {
                        this.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
                        this.anchorView = binding.layoutPersistentBottomSheet.root
                        setAction("OK") {}
                        this.show()
                    }
//                    binding.root.showSnackBar(
//                        message = "Books are loading, please wait.",
//                        anchorView = binding.layoutPersistentBottomSheet.root,
//                        duration = Snackbar.LENGTH_INDEFINITE,
//                        isAnimated = false,
//                        actionBtnText = "OK"
//                    )
                }
            }
            setPermissionView(isShow = false)
            binding.rvDownloads.isVisible = true
            convertPdfToTextInWorker()
        } else {
            setPermissionView(isShow = true)
            binding.rvDownloads.isVisible = false
        }
    }

    private fun checkTtsExists() {
        val intent = Intent().apply {
            action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        }
        ttsLauncher.launch(intent)
    }

    /** Any call to speak() for the same string content as wakeUpText will result in the playback of destFileName.
     * This is done to avoid synthesizing text in tts again and save resources.
     * You can provide custom audio file as path as well */
    private fun playSavedAudioFile() {
        val wakeUpText = "Are you up yet?"
        val destFile = File("/sdcard/myAppCache/wakeUp.wav")
        tts?.addSpeech(wakeUpText, destFile)
    }

    private fun setUpPersistentBottomSheet() {
        binding.layoutPersistentBottomSheet.cardCurrentlyReading.layoutParams.height = deviceHeight() / 3
        binding.layoutPersistentBottomSheet.layoutSliderPlayback.apply {
            ibReduce.isVisible = false
            ibIncrease.isVisible = false
        }
        binding.layoutPersistentBottomSheet.layoutSliderPlayback.llSliderCustom.setMargins(
            start = 8.dpToPx().toInt(),
            top = 0.dpToPx().toInt(),
            end = 8.dpToPx().toInt(),
            bottom = 0.dpToPx().toInt(),
        )
//        binding.layoutPersistentBottomSheet.tvSelectLanguage.text = "Language: ${Locale.getDefault().displayName}"
    }

    private fun convertPdfToTextInWorker() {
        val data = Data.Builder().apply {
            putString(WorkerData.PDF_PATH, "")
        }.build()
        val workRequest = OneTimeWorkRequestBuilder<PdfToTextWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(requireContext()).enqueueUniqueWork(WorkerTag.PDF_TO_TEXT_CONVERTER, ExistingWorkPolicy.REPLACE, workRequest)
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(workRequest.id).observe(viewLifecycleOwner) { workInfo: WorkInfo? ->
            if (workInfo != null) {
                val progress = workInfo.progress.getInt(WorkerData.KEY_PROGRESS, 0)
                binding.tvProgress.text = progress.toString()
            }
            when (workInfo?.state) {
                WorkInfo.State.RUNNING -> showProgressBar(true)
                WorkInfo.State.ENQUEUED -> showProgressBar(true)
                WorkInfo.State.SUCCEEDED -> {
                    // TODO show manual rss url field
                    showProgressBar(false)
                    dismissBookLoadingSnackbar()
                }

                WorkInfo.State.FAILED -> showProgressBar(false)
                WorkInfo.State.BLOCKED -> showProgressBar(true)
                WorkInfo.State.CANCELLED -> showProgressBar(false)
                else -> Unit
            }
        }
    }

    private fun dismissBookLoadingSnackbar() {
        if (bookLoadingSnackBar?.isShownOrQueued == true) {
            bookLoadingSnackBar?.dismiss()
        }
    }

    private fun showProgressBar(isShow: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.progressCircular.isVisible = isShow
            binding.ivHeaderMore.isVisible = isShow.not()
            binding.tvProgress.isVisible = isShow
        }
    }

    private fun setPermissionView(
        isShow: Boolean,
        @DrawableRes icon: Int = R.drawable.outline_security_24,
        title: String = getString(R.string.grant_storage_permission),
        isShowButton: Boolean = true
    ) {
        binding.layoutGrantPermission.apply {
            root.isVisible = isShow
            ivIcon.load(icon)
            tvTitle.text = title
            btnGivePermission.isVisible = isShowButton
        }
    }
}