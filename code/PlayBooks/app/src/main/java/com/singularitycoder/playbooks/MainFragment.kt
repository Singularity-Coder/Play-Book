package com.singularitycoder.playbooks

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.text.Editable
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
import com.singularitycoder.playbooks.helpers.TTS_LANGUAGE_LIST
import com.singularitycoder.playbooks.helpers.WorkerData
import com.singularitycoder.playbooks.helpers.WorkerTag
import com.singularitycoder.playbooks.helpers.collectLatestLifecycleFlow
import com.singularitycoder.playbooks.helpers.deviceHeight
import com.singularitycoder.playbooks.helpers.dpToPx
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
import com.singularitycoder.playbooks.helpers.showSnackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale


// Run a background worker converting all pdf to text and store books in db
// Before storing text in db, trim all new line characters and unreadable ASCII code

// DB - Book - position of periods, chapters, sentences
// Use foreground service for player
// First get files from file man, then in worker convert pdfs to text n insert text to db, from db listen to db inserts and in observer load list in view
// Show instructions on how to convert ebook formats to pdf online


/**
 * https://stackoverflow.com/questions/58425372/android-room-database-size#:~:text=The%20maximum%20size%20of%20a,140%2C000%20gigabytes%20or%20128%2C000%20gibibytes).
 *
 * Maximum length of a string or BLOB Default size is 1 GB Max size is 2.147483647
 * Maximum Number Of Columns Default size is 2000 Max size is 32767
 * Maximum Length Of An SQL Statement Default size is 1 MB Max size is 1.073741824
 * Maximum Number Of Tables In A Join Default is 64 tables
 * Maximum Number Of Attached Databases Default is 10 Max size is 125
 * Maximum Number Of Rows In A Table Max Size is 18446744073.709552765
 * Maximum Database Size 140 tb but it will depends on your device disk size.
 *
 * https://www.sqlite.org/limits.html
 * */

/**
 * https://android-developers.googleblog.com/2009/09/introduction-to-text-to-speech-in.html
 *
 * String myText1 = "Did you sleep well?";
 * String myText2 = "I hope so, because it's time to wake up.";
 * mTts.speak(myText1, TextToSpeech.QUEUE_FLUSH, null);
 * mTts.speak(myText2, TextToSpeech.QUEUE_ADD, null);
 *
 * the first speak() request would interrupt whatever was currently being synthesized: the queue is flushed and the new utterance is queued, which places it at the head of the queue. The second utterance is queued and will be played after myText1 has completed.
 * */
const val ARG_PARAM_SCREEN_TYPE = "ARG_PARAM_SCREEN_TYPE"

@AndroidEntryPoint
class MainFragment : Fragment(), OnInitListener {

    companion object {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        // tts?.shutdown()
    }

    override fun onInit(p0: Int) {
        binding.root.showSnackBar("Text-To-Speech engine is ready.")
        TTS_LANGUAGE_LIST.forEach { locale: Locale ->
            if (tts?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                availableLanguages.add(locale)
            }
        }
        tts?.setLanguage(Locale.US)
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
        layoutPersistentBottomSheet.layoutSliderPlayback.tvSliderTitle.text = "Book Progress"
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun FragmentMainBinding.setupUserActionListeners() {
        root.setOnClickListener { }

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

                withContext(Dispatchers.Main) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    layoutPersistentBottomSheet.root.isVisible = true
                    layoutPersistentBottomSheet.tvHeader.text = book?.title
                    layoutPersistentBottomSheet.tvCurrentlyReading.text = bookData.text
                    tts?.speak(bookData.text, TextToSpeech.QUEUE_FLUSH, ttsParams, "")
                    startForegroundService()
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
                    optionsList[1].first -> {
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
                tts?.setPitch(0F)
                sliderCustom.progress -= 1
                tvValue.text = sliderCustom.progress.toString()
            }
            ibIncrease.setOnClickListener {
                tts?.setPitch(0F)
                sliderCustom.progress += 1
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
                tts?.setSpeechRate(0F)
                sliderCustom.progress -= 1
                tvValue.text = sliderCustom.progress.toString()
            }
            ibIncrease.setOnClickListener {
                tts?.setSpeechRate(0F)
                sliderCustom.progress += 1
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
                    tvValue.text = progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }

        layoutPersistentBottomSheet.ivPlay.setOnClickListener {
            tts?.speak("text to speak", TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        layoutPersistentBottomSheet.ivForward.setOnClickListener {
            tts?.speak("text to speak", TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        layoutPersistentBottomSheet.ivBackward.setOnClickListener {
            tts?.speak("text to speak", TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        layoutPersistentBottomSheet.ivNext.setOnClickListener {
            tts?.speak("text to speak", TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        layoutPersistentBottomSheet.ivPrevious.setOnClickListener {
            tts?.speak("text to speak", TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) = Unit

            override fun onDone(uttId: String?) {
                if (uttId == "end of wakeup message ID") {
                    // do something
                }
            }

            override fun onError(uttId: String?) = Unit
        })

        val ttsLanguageList = TTS_LANGUAGE_LIST.map { Pair(it.displayName, R.drawable.round_check_24) }
        layoutPersistentBottomSheet.ivHeaderMore.setOnClickListener { view: View? ->
            val optionsList = listOf(
                Pair("Select Language", R.drawable.round_language_24),
                Pair("Save as audio file", R.drawable.outline_audio_file_24),
            )
            requireContext().showPopupMenuWithIcons(
                view = layoutPersistentBottomSheet.ivHeaderMore,
                menuList = optionsList
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
                            tts?.language = Locale(selectedTtsLanguage)
                        }
                    }

                    optionsList[1].first -> {
                        saveAsAudioFile()
                    }
                }
            }
        }
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

    private fun startForegroundService() {
        val intent = Intent()
        context?.applicationContext?.startForegroundService(intent)
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