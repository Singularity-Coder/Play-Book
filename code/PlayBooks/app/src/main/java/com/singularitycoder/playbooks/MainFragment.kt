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
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.card.MaterialCardView
import com.singularitycoder.playbooks.databinding.FragmentMainBinding
import com.singularitycoder.playbooks.helpers.AndroidVersions
import com.singularitycoder.playbooks.helpers.AppPreferences
import com.singularitycoder.playbooks.helpers.FILE_PROVIDER
import com.singularitycoder.playbooks.helpers.IntentExtraKey
import com.singularitycoder.playbooks.helpers.IntentExtraValue
import com.singularitycoder.playbooks.helpers.IntentKey
import com.singularitycoder.playbooks.helpers.TtsConstants
import com.singularitycoder.playbooks.helpers.WakeLockKey
import com.singularitycoder.playbooks.helpers.WorkerData
import com.singularitycoder.playbooks.helpers.WorkerTag
import com.singularitycoder.playbooks.helpers.clipboard
import com.singularitycoder.playbooks.helpers.collectLatestLifecycleFlow
import com.singularitycoder.playbooks.helpers.deleteAllFilesFrom
import com.singularitycoder.playbooks.helpers.deleteFileFrom
import com.singularitycoder.playbooks.helpers.deviceHeight
import com.singularitycoder.playbooks.helpers.dpToPx
import com.singularitycoder.playbooks.helpers.drawable
import com.singularitycoder.playbooks.helpers.getBookCoversFileDir
import com.singularitycoder.playbooks.helpers.getDownloadDirectory
import com.singularitycoder.playbooks.helpers.globalLayoutAnimation
import com.singularitycoder.playbooks.helpers.hasNotificationsPermission
import com.singularitycoder.playbooks.helpers.hasPdfs
import com.singularitycoder.playbooks.helpers.hasStoragePermissionApi30
import com.singularitycoder.playbooks.helpers.hideKeyboard
import com.singularitycoder.playbooks.helpers.layoutAnimationController
import com.singularitycoder.playbooks.helpers.onImeClick
import com.singularitycoder.playbooks.helpers.onSafeClick
import com.singularitycoder.playbooks.helpers.requestStoragePermissionApi30
import com.singularitycoder.playbooks.helpers.setMargins
import com.singularitycoder.playbooks.helpers.setNavigationBarColor
import com.singularitycoder.playbooks.helpers.shouldShowRationaleFor
import com.singularitycoder.playbooks.helpers.showAlertDialog
import com.singularitycoder.playbooks.helpers.showAppSettings
import com.singularitycoder.playbooks.helpers.showPopupMenu
import com.singularitycoder.playbooks.helpers.showPopupMenuWithIcons
import com.singularitycoder.playbooks.helpers.showSingleSelectionPopupMenu
import com.singularitycoder.playbooks.helpers.showSnackBar
import com.singularitycoder.playbooks.helpers.showToast
import com.singularitycoder.playbooks.helpers.showTtsSettings
import com.singularitycoder.playbooks.helpers.showWebPage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@AndroidEntryPoint
class MainFragment : Fragment() {

    companion object {
        private val TAG = this::class.java.simpleName

        @JvmStatic
        fun newInstance() = MainFragment()
    }

    private var previousConfig: Configuration? = null

    private val booksAdapter = BooksAdapter()

    private var booksList = listOf<Book?>()

    private lateinit var binding: FragmentMainBinding

    private var isTtsPresent = false

    private var selectedTtsLanguage: String? = Locale.getDefault().displayName

    private val bookViewModel by viewModels<BookViewModel>()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<MaterialCardView>

    private var playBookForegroundService: PlayBookForegroundService? = null

    private var isServiceBound = false

    private var wakeLock: PowerManager.WakeLock? = null

    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val msg = intent.getStringExtra(IntentExtraKey.MESSAGE)
            when (msg) {
                IntentExtraValue.TTS_READY -> {
                    Log.d(TAG, "TTS_READY broadcast received")
                }

                IntentExtraValue.FOREGROUND_SERVICE_READY -> {
                    Log.d(TAG, "FOREGROUND_SERVICE_READY broadcast received")
                    binding.doWhenForegroundServiceIsReady()
                }

                IntentExtraValue.TTS_PLAYING -> {
                    doWhenTtsIsPlaying(context)
                }

                IntentExtraValue.SET_PAGE_PROGRESS -> {
                    binding.layoutPersistentBottomSheet.layoutSliderPlayback.sliderCustom.progress = playBookForegroundService?.getCurrentPagePosition() ?: 1
                }

                IntentExtraValue.TTS_PAUSED -> {
                    binding.layoutPersistentBottomSheet.ivPlay.setImageDrawable(context.drawable(R.drawable.round_play_arrow_24))
                    binding.layoutPersistentBottomSheet.ivHeaderPlay.setImageDrawable(context.drawable(R.drawable.round_play_arrow_24))
                }

                IntentExtraValue.READING_COMPLETE -> {
                    /** This will work for all other languages when highlighting fails */
                    binding.layoutPersistentBottomSheet.tvCurrentlyReading.text = playBookForegroundService?.getCurrentlyPlayingText()?.trim()
                }

                IntentExtraValue.TTS_WORD_HIGHLIGHT -> {
                    /** This works for English languages only */
                    try {
                        val start = intent.getIntExtra(IntentExtraKey.TTS_WORD_HIGHLIGHT_START, 0) - 1
                        val end = intent.getIntExtra(IntentExtraKey.TTS_WORD_HIGHLIGHT_END, 0) - 1 // -1 as we set start + 1 in speak method in service
                        val utterance = playBookForegroundService?.getCurrentlyPlayingText()?.trim()
                        val textWithHighlights: Spannable = SpannableString(utterance).apply {
                            setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                            setSpan(BackgroundColorSpan(Color.YELLOW), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                        }
                        binding.layoutPersistentBottomSheet.tvCurrentlyReading.text = textWithHighlights
                    } catch (e: Exception) {
                        println(e)
                    }
                }

                else -> Unit
            }
        }
    }

    /** needed to communicate with the service. */
    private val playerConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            /** we've bound to PlayBookForegroundService, cast the IBinder and get PlayBookForegroundService instance. */
            Log.d(TAG, "onServiceConnected")
            val binder = service as PlayBookForegroundService.LocalBinder
            playBookForegroundService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            /** This is called when the connection with the service has been disconnected. Clean up. */
            Log.d(TAG, "onServiceDisconnected")
            isServiceBound = false
            playBookForegroundService = null
        }
    }

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

    /** Check if TTS exists */
    private val ttsLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            /** success, create the TTS instance */
            isTtsPresent = true
        } else {
            isTtsPresent = false
            /** missing data, install it */
            val intent = Intent().apply {
                action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            }
            startActivity(intent)
        }
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
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            /* receiver = */ messageReceiver,
            /* filter = */ IntentFilter(IntentKey.MAIN_BROADCAST_FROM_SERVICE)
        )
    }

    /** Since onDestroy is not a guarenteed call when app destroyed */
    override fun onPause() {
        super.onPause()
        playBookForegroundService?.updateCompletedPagePositionToDb()
        if (isServiceBound) {
            activity?.unbindService(playerConnection)
            isServiceBound = false
        }

        releaseWakeLock()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(messageReceiver)
    }

    // https://stackoverflow.com/questions/59694023/listening-on-dark-theme-in-notification-area-toggle-and-be-notified-of-a-chang
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        fun isOnDarkMode(configuration: Configuration): Boolean {
            return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        fun isNightConfigChanged(newConfig: Configuration): Boolean {
            return (newConfig.diff(previousConfig) and ActivityInfo.CONFIG_UI_MODE) != 0 && isOnDarkMode(newConfig) != isOnDarkMode(previousConfig!!)
        }

        if (isNightConfigChanged(newConfig)) {
            playBookForegroundService?.doOnConfigChange()
        }
    }

    private fun FragmentMainBinding.setupUI() {
        previousConfig = Configuration(resources.configuration)
        bottomSheetBehavior = BottomSheetBehavior.from(binding.layoutPersistentBottomSheet.root)

        /** Only after setting this peekHeight will work for persistent bottomsheet */
        bottomSheetBehavior.isGestureInsetBottomIgnored = true

        requireActivity().setNavigationBarColor(R.color.white)
        rvBooks.apply {
            layoutAnimation = rvBooks.context.layoutAnimationController(globalLayoutAnimation)
            layoutManager = LinearLayoutManager(context)
            adapter = booksAdapter
        }
        layoutSearch.etSearch.hint = "Search in ${getDownloadDirectory().name}"
        setUpPersistentBottomSheet()
        checkTtsExists()
        selectedTtsLanguage = AppPreferences.getInstance().ttsLanguage
        layoutPersistentBottomSheet.layoutSliderPlayback.apply {
            tvSliderTitle.text = "Page"
            sliderCustom.min = 1
        }
        layoutPersistentBottomSheet.layoutSliderSpeed.apply {
            tvSliderTitle.text = "Speed"
            tvValue.text = AppPreferences.getInstance().ttsSpeechRate.toString()
            sliderCustom.min = TtsConstants.MIN
            sliderCustom.max = TtsConstants.MAX
            sliderCustom.progress = AppPreferences.getInstance().ttsSpeechRate
        }
        layoutPersistentBottomSheet.layoutSliderPitch.apply {
            tvSliderTitle.text = "Pitch"
            tvValue.text = AppPreferences.getInstance().ttsPitch.toString()
            sliderCustom.min = TtsConstants.MIN
            sliderCustom.max = TtsConstants.MAX
            sliderCustom.progress = AppPreferences.getInstance().ttsPitch
        }

        /** This will make sure that when u kill the app & relaunch it u will
         * still see the player view reading the right book */
        if (PlayBookForegroundService.playBookForegroundService != null &&
            PlayBookForegroundService.playBookForegroundService?.isTtsSpeaking() == true
        ) {
            playBookForegroundService = PlayBookForegroundService.playBookForegroundService
            showPlayerView(isShow = true)
            doWhenTtsIsPlaying(requireContext())
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun FragmentMainBinding.setupUserActionListeners() {
        root.setOnClickListener {}

        layoutPersistentBottomSheet.root.setOnClickListener {}

        binding.layoutLoading.btnStop.onSafeClick {
            WorkManager.getInstance(requireContext()).cancelAllWork() // cancelAllWorkByTag is not working
        }

        progressCircular.viewTreeObserver.addOnGlobalLayoutListener {
            binding.layoutLoading.root.isVisible = progressCircular.isVisible
        }

        ivHeaderMore.onSafeClick { view: Pair<View?, Boolean> ->
            val optionsList = listOf(
                Pair("Refresh", R.drawable.round_refresh_24),
                Pair("Download E-Books", R.drawable.outline_file_download_24),
                Pair("Convert to PDF", R.drawable.outline_picture_as_pdf_24),
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
                        context?.showWebPage(url = "https://www.google.com/search?q=download+free+ebooks")
                    }

                    optionsList[2].first -> {
                        val menuList = listOf(
                            "epub to pdf",
                            "txt to pdf",
                            "djvu to pdf",
                            "word to pdf",
                            "rich text to pdf",
                            "kindle (azw) to pdf"
                        )
                        val urlList = listOf(
                            "https://www.google.com/search?q=convert+epub+to+pdf+online",
                            "https://www.google.com/search?q=convert+txt+to+pdf+online",
                            "https://www.google.com/search?q=convert+djvu+to+pdf+online",
                            "https://www.google.com/search?q=convert+word+to+pdf+online",
                            "https://www.google.com/search?q=convert+rich+text+to+pdf+online",
                            "https://www.google.com/search?q=convert+kindle+to+pdf+online"
                        )
                        context?.showPopupMenu(
                            view = view.first,
                            title = "to PDF Converters",
                            menuList = menuList
                        ) { menuPosition: Int ->
                            urlList.forEachIndexed { index, s ->
                                if (index == menuPosition) {
                                    context?.showWebPage(url = urlList[menuPosition])
                                }
                            }
                        }
                    }

                    optionsList[3].first -> {
                        requireContext().showAlertDialog(
                            title = "Delete all books",
                            message = "Don't worry. The files on your device won't be deleted.",
                            positiveBtnText = "Delete All",
                            negativeBtnText = "Cancel",
                            positiveBtnColor = R.color.md_red_700,
                            positiveAction = {
                                showPlayerView(false)
                                bookViewModel.deleteAllBookDataItems()
                                bookViewModel.deleteAllBookItems()
                                deleteAllFilesFrom(directory = File(requireContext().getBookCoversFileDir()))
                                booksAdapter.notifyDataSetChanged()
                            }
                        )
                    }
                }
            }
        }

        booksAdapter.setOnItemClickListener { book, position ->
            if (isTtsPresent.not()) {
                binding.root.showSnackBar(message = "You don't have \"Text-to-Speech\" feature on your device.")
                return@setOnItemClickListener
            }

            if (AndroidVersions.isTiramisu()) {
                if (activity?.hasNotificationsPermission()?.not() == true) {
                    askNotificationPermission()
                    return@setOnItemClickListener
                }
            }

            startForegroundService(book)
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
                        if (file.exists().not()) {
                            context?.showToast("Could not find the book in ${getDownloadDirectory().name} folder.")
                            return@showPopupMenuWithIcons
                        }
                        val uri = FileProvider.getUriForFile(requireContext(), FILE_PROVIDER, file)
                        val intent = Intent().apply {
                            action = Intent.ACTION_VIEW
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            setDataAndType(uri, "application/pdf")
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
                                deleteFileFrom(path = "${requireContext().getBookCoversFileDir()}/${book?.id}.jpg")
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
                    requireActivity().requestStoragePermissionApi30()
                }

                getString(R.string.no_books_found) -> {
                    loadPdfs()
                }

                else -> Unit
            }
        }

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
            ibReduce.onSafeClick {
                sliderCustom.progress -= 1
                playBookForegroundService?.setTtsPitch(sliderCustom.progress.toFloat())
                tvValue.text = sliderCustom.progress.toString()
                playBookForegroundService?.stopAndPlayTts()
                AppPreferences.getInstance().ttsPitch = sliderCustom.progress
            }
            ibIncrease.onSafeClick {
                sliderCustom.progress += 1
                playBookForegroundService?.setTtsPitch(sliderCustom.progress.toFloat())
                tvValue.text = sliderCustom.progress.toString()
                playBookForegroundService?.stopAndPlayTts()
                AppPreferences.getInstance().ttsPitch = sliderCustom.progress
            }
            sliderCustom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    tvValue.text = seekBar.progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    println("seekbar progress: ${seekBar.progress}")
                    tvValue.text = seekBar.progress.toString()
                    playBookForegroundService?.setTtsPitch(seekBar.progress.toFloat())
                    playBookForegroundService?.stopAndPlayTts()
                    AppPreferences.getInstance().ttsPitch = seekBar.progress
                }
            })
        }

        layoutPersistentBottomSheet.layoutSliderSpeed.apply {
            ibReduce.onSafeClick {
                sliderCustom.progress -= 1
                playBookForegroundService?.setTtsSpeechRate(sliderCustom.progress.toFloat())
                tvValue.text = sliderCustom.progress.toString()
                playBookForegroundService?.stopAndPlayTts()
                AppPreferences.getInstance().ttsSpeechRate = sliderCustom.progress
            }
            ibIncrease.onSafeClick {
                sliderCustom.progress += 1
                playBookForegroundService?.setTtsSpeechRate(sliderCustom.progress.toFloat())
                tvValue.text = sliderCustom.progress.toString()
                playBookForegroundService?.stopAndPlayTts()
                AppPreferences.getInstance().ttsSpeechRate = sliderCustom.progress
            }
            sliderCustom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    tvValue.text = seekBar.progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    println("seekbar progress: ${seekBar.progress}")
                    tvValue.text = seekBar.progress.toString()
                    playBookForegroundService?.setTtsSpeechRate(seekBar.progress.toFloat())
                    playBookForegroundService?.stopAndPlayTts()
                    AppPreferences.getInstance().ttsSpeechRate = seekBar.progress
                }
            })
        }

        layoutPersistentBottomSheet.layoutSliderPlayback.apply {
            var oldProgress = 0
            sliderCustom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    tvValue.text = "${seekBar.progress}/${playBookForegroundService?.getCurrentPlayingBook()?.pageCount}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    println("seekbar progress: ${seekBar.progress}")
                    tvValue.text = "${seekBar.progress}/${playBookForegroundService?.getCurrentPlayingBook()?.pageCount}"
                    if (seekBar.progress > oldProgress) {
                        playBookForegroundService?.nextPage(pagePosition = seekBar.progress)
                    } else {
                        playBookForegroundService?.previousPage(pagePosition = seekBar.progress)
                    }
                    oldProgress = seekBar.progress
                    Log.d(TAG, seekBar.progress.toString())
                    Log.d(TAG, oldProgress.toString())
                }
            })
        }

        layoutPersistentBottomSheet.ivPlay.onSafeClick {
            playBookForegroundService?.playPauseTts()
        }

        layoutPersistentBottomSheet.ivHeaderPlay.onSafeClick {
            layoutPersistentBottomSheet.ivPlay.performClick()
        }

        layoutPersistentBottomSheet.ibNextSentence.onSafeClick {
            playBookForegroundService?.nextSentence()
        }

        /** Not using delayed click here as TTS does not switch fast enough to the previous sentence */
        layoutPersistentBottomSheet.ibPreviousSentence.setOnClickListener {
            playBookForegroundService?.previousSentence()
        }

        layoutPersistentBottomSheet.ibNextPage.onSafeClick {
            playBookForegroundService?.nextPage()
            layoutPersistentBottomSheet.layoutSliderPlayback.sliderCustom.progress += 1
        }

        layoutPersistentBottomSheet.ibPreviousPage.onSafeClick {
            playBookForegroundService?.previousPage()
            layoutPersistentBottomSheet.layoutSliderPlayback.sliderCustom.progress -= 1
        }

        layoutPersistentBottomSheet.ivHeaderMore.setOnClickListener { view: View? ->
            val optionsList = listOf(
                Pair("Copy Sentence", R.drawable.baseline_content_copy_24),
                Pair("Select Language", R.drawable.round_language_24),
                Pair("Reset Speech Settings", R.drawable.round_settings_backup_restore_24),
//                Pair("Save as audio file", R.drawable.outline_audio_file_24),
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
                        context.clipboard()?.text = playBookForegroundService?.getCurrentlyPlayingText()
                    }

                    /** Locale() takes input as short form language codes like en, fr, etc. */
                    optionsList[1].first -> {
                        val ttsLanguages = playBookForegroundService?.getAvailableTtsLanguages()?.toList()
                        val ttsLangMenuList = ttsLanguages?.map {
                            Pair(it.displayName, R.drawable.round_check_24)
                        } ?: emptyList()
                        requireContext().showSingleSelectionPopupMenu(
                            view = layoutPersistentBottomSheet.ivHeaderMore,
                            title = "Select Language",
                            selectedOption = selectedTtsLanguage,
                            menuList = ttsLangMenuList,
                        ) { menuItem: MenuItem? ->
                            val selectedLangIndex = ttsLanguages?.indexOfFirst { it.displayName == (menuItem?.title?.toString()?.trim() ?: "") } ?: 0
                            selectedTtsLanguage = ttsLanguages?.get(selectedLangIndex)?.displayName ?: Locale.getDefault().displayName
                            playBookForegroundService?.setTtsLanguage(ttsLanguages?.get(selectedLangIndex) ?: Locale.getDefault())
                            playBookForegroundService?.stopAndPlayTts()
                            AppPreferences.getInstance().ttsLanguage = selectedTtsLanguage ?: Locale.getDefault().displayName
                        }
                    }

//                    optionsList[1].first -> {
//                        playBookForegroundService?.saveAsAudioFile()
//                    }

                    optionsList[2].first -> {
                        binding.setTtsDefaultSettings()
                    }

                    optionsList[3].first -> {
                        activity?.showTtsSettings()
                    }

                    optionsList[4].first -> {
                        // stopForegroundService()
                        playBookForegroundService?.stopTts()
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        showPlayerView(false)
                    }
                }
            }
        }
    }

    private fun doWhenTtsIsPlaying(context: Context) {
        binding.layoutPersistentBottomSheet.ivPlay.setImageDrawable(context.drawable(R.drawable.round_pause_24))
        binding.layoutPersistentBottomSheet.ivHeaderPlay.setImageDrawable(context.drawable(R.drawable.round_pause_24))
        binding.layoutPersistentBottomSheet.tvHeader.text = playBookForegroundService?.getCurrentPlayingBook()?.title
        binding.layoutPersistentBottomSheet.layoutSliderPlayback.apply {
            tvValue.text = "${sliderCustom.progress}/${playBookForegroundService?.getCurrentPlayingBook()?.pageCount}"
            sliderCustom.min = 1
            sliderCustom.max = playBookForegroundService?.getCurrentPlayingBook()?.pageCount ?: 0
        }
        val bookCover = File(
            /* parent = */ context.getBookCoversFileDir(),
            /* child = */ "${playBookForegroundService?.getCurrentPlayingBook()?.id}.jpg"
        )
        binding.layoutPersistentBottomSheet.ivHeaderImage.load(bookCover)
    }

    private fun FragmentMainBinding.setTtsDefaultSettings() {
        layoutPersistentBottomSheet.layoutSliderSpeed.apply {
            tvValue.text = TtsConstants.DEFAULT.toString()
            sliderCustom.progress = TtsConstants.DEFAULT
        }
        layoutPersistentBottomSheet.layoutSliderPitch.apply {
            tvValue.text = TtsConstants.DEFAULT.toString()
            sliderCustom.progress = TtsConstants.DEFAULT
        }
        playBookForegroundService?.setTtsPitch(TtsConstants.DEFAULT.toFloat())
        playBookForegroundService?.setTtsSpeechRate(TtsConstants.DEFAULT.toFloat())
        playBookForegroundService?.setTtsLanguage(Locale.getDefault())
        selectedTtsLanguage = Locale.getDefault().displayName
        playBookForegroundService?.stopAndPlayTts()
        context?.showToast("Speech settings reset")
    }

    private fun FragmentMainBinding.doWhenForegroundServiceIsReady() {
        /** This must be set after player is ready with book data */
        layoutPersistentBottomSheet.layoutSliderPlayback.apply {
            sliderCustom.max = playBookForegroundService?.getCurrentPlayingBook()?.pageCount ?: 0
            tvValue.text = "${sliderCustom.progress}/${playBookForegroundService?.getCurrentPlayingBook()?.pageCount}"
        }
        showPlayerView(true)
        layoutPersistentBottomSheet.tvHeader.text = playBookForegroundService?.getCurrentPlayingBook()?.title

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    @SuppressLint("InlinedApi")
    private fun askNotificationPermission() {
        notificationPermissionResult.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeForData() {
        (activity as? MainActivity)?.collectLatestLifecycleFlow(flow = bookViewModel.getAllBookItemsFlow()) { booksList: List<Book?> ->
            if (this.booksList.isNotEmpty() && this.booksList == booksList) {
                return@collectLatestLifecycleFlow
            }
            this.booksList = booksList
            booksAdapter.bookList = booksList
            booksAdapter.notifyDataSetChanged()
//            booksAdapter.notifyItemInserted(currentBookPosition)
//            currentBookPosition++
//            binding.nestedScrollView.scrollTo(0, 0)
//            binding.rvBooks.runLayoutAnimation(globalLayoutAnimation)
        }
    }

    private fun startForegroundService(book: Book?) {
        fun startService() {
            val intent = Intent(context, PlayBookForegroundService::class.java).apply {
                putExtra(IntentExtraKey.BOOK_ID, book?.id ?: "")
            }
            activity?.application?.startForegroundService(intent)
            /** bind to the service to update UI */
            activity?.bindService(intent, playerConnection, Context.BIND_AUTO_CREATE)
        }

        if (playBookForegroundService != null) {
            playBookForegroundService?.stopTts()
            playBookForegroundService?.loadData(book?.id ?: "")
            return
        }

        startService()
    }

    private fun stopForegroundService() {
        playBookForegroundService?.stopForegroundService()
    }

    private fun showPlayerView(isShow: Boolean) {
        requireActivity().window.navigationBarColor = if (isShow) {
            requireContext().getColor(R.color.purple_700)
        } else {
            requireContext().getColor(R.color.white)
        }
        binding.layoutPersistentBottomSheet.root.isVisible = isShow
    }

    private fun loadPdfs() {
        if (AndroidVersions.isTiramisu()) {
            if (activity?.hasNotificationsPermission()?.not() == true) {
                setPermissionView(
                    isShow = true,
                    icon = R.drawable.round_notifications_active_24,
                    title = R.string.grant_notification_permission,
                    isShowButton = true
                )
                return
            }
        }

        if (activity?.hasStoragePermissionApi30() == true) {
            var hasBooksInDb = false
            CoroutineScope(Dispatchers.IO).launch {
                hasBooksInDb = bookViewModel.hasBooks()

                withContext(Dispatchers.Main) {
                    setPermissionView(
                        isShow = hasPdfs().not() && hasBooksInDb.not(),
                        icon = R.drawable.round_menu_book_24,
                        title = R.string.no_books_found,
                        isShowButton = true,
                        btnText = R.string.refresh
                    )
                }
            }
            if (hasPdfs().not() && hasBooksInDb.not()) return
            /** This will continue the existing work instead of starting new if app is not killed */
            if (binding.progressCircular.isVisible) return

            setPermissionView(isShow = false)
            binding.rvBooks.isVisible = true
            convertPdfToTextInWorker()
        } else {
            setPermissionView(isShow = true)
            binding.rvBooks.isVisible = false
        }
    }

    private fun checkTtsExists() {
        val intent = Intent().apply {
            action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        }
        ttsLauncher.launch(intent)
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
    }

    @SuppressLint("WakelockTimeout")
    private fun convertPdfToTextInWorker() {
        /** This is to make sure books are loading even if screen is turned off. Keeps CPU awake. */
        wakeLock = (requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WakeLockKey.LOADING_BOOKS).apply {
                acquire()
            }
        }

        val workRequest = OneTimeWorkRequestBuilder<PdfToTextWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(requireContext()).enqueueUniqueWork(WorkerTag.PDF_TO_TEXT_CONVERTER, ExistingWorkPolicy.REPLACE, workRequest)
        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(workRequest.id).observe(viewLifecycleOwner) { workInfo: WorkInfo? ->
            if (workInfo != null) {
                val progress = workInfo.progress.getInt(WorkerData.KEY_PROGRESS, 0)
                binding.tvProgress.text = progress.toString()
                binding.progressCircular.progress = progress
            }
            when (workInfo?.state) {
                WorkInfo.State.RUNNING -> showProgressBar(true)
                WorkInfo.State.ENQUEUED -> showProgressBar(true)
                WorkInfo.State.SUCCEEDED -> {
                    showProgressBar(false)
                    releaseWakeLock()
                }

                WorkInfo.State.FAILED -> {
                    showProgressBar(false)
                    releaseWakeLock()
                }

                WorkInfo.State.BLOCKED -> showProgressBar(true)
                WorkInfo.State.CANCELLED -> {
                    showProgressBar(false)
                    releaseWakeLock()
                }

                else -> Unit
            }
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
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
        @StringRes title: Int = R.string.grant_storage_permission,
        isShowButton: Boolean = true,
        @StringRes btnText: Int = R.string.give_permission
    ) {
        binding.layoutGrantPermission.apply {
            root.isVisible = isShow
            ivIcon.load(icon)
            tvTitle.text = getString(title)
            btnGivePermission.isVisible = isShowButton
            btnGivePermission.text = getString(btnText)
        }
    }
}