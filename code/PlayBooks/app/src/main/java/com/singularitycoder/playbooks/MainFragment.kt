package com.singularitycoder.playbooks

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioManager
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
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.card.MaterialCardView
import com.singularitycoder.playbooks.databinding.FragmentMainBinding
import com.singularitycoder.playbooks.helpers.TTS_LANGUAGE_LIST
import com.singularitycoder.playbooks.helpers.checkStoragePermission
import com.singularitycoder.playbooks.helpers.deviceHeight
import com.singularitycoder.playbooks.helpers.dpToPx
import com.singularitycoder.playbooks.helpers.extension
import com.singularitycoder.playbooks.helpers.getAppropriateSize
import com.singularitycoder.playbooks.helpers.getDownloadDirectory
import com.singularitycoder.playbooks.helpers.getFilesListFrom
import com.singularitycoder.playbooks.helpers.getTextFromPdf
import com.singularitycoder.playbooks.helpers.globalLayoutAnimation
import com.singularitycoder.playbooks.helpers.hideKeyboard
import com.singularitycoder.playbooks.helpers.layoutAnimationController
import com.singularitycoder.playbooks.helpers.onImeClick
import com.singularitycoder.playbooks.helpers.onSafeClick
import com.singularitycoder.playbooks.helpers.requestStoragePermission
import com.singularitycoder.playbooks.helpers.runLayoutAnimation
import com.singularitycoder.playbooks.helpers.setMargins
import com.singularitycoder.playbooks.helpers.setNavigationBarColor
import com.singularitycoder.playbooks.helpers.showListPopupMenu2
import com.singularitycoder.playbooks.helpers.showPopupMenu
import com.singularitycoder.playbooks.helpers.showPopupMenuWithIcons
import com.singularitycoder.playbooks.helpers.showSingleSelectionPopupMenu
import com.singularitycoder.playbooks.helpers.showSnackBar
import com.singularitycoder.playbooks.helpers.toUpCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

// Run a background worker converting all pdf to text and store books in db
// Before storing text in db, trim all new line characters and unreadable ASCII code

// DB - Book - position of periods, chapters, sentences

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

    private val pdfList = mutableListOf<String>()

    private val downloadsAdapter = DownloadsAdapter()
    private val downloadsList = mutableListOf<Download>()

    private lateinit var binding: FragmentMainBinding

    private var tts: TextToSpeech? = null

    private val availableLanguages = mutableListOf<Locale>()

    private val ttsParams = Bundle()

    private var selectedTtsLanguage = Locale.getDefault().displayName

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<MaterialCardView>

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

    @SuppressLint("NotifyDataSetChanged")
    private fun loadPdfs() {
        CoroutineScope(IO).launch {
            val hasPermission = requireActivity().checkStoragePermission()
            if (hasPermission) {
                binding.llStoragePermissionRationaleView.isVisible = false
                binding.rvDownloads.isVisible = true
                val filesList = getFilesListFrom(folder = getDownloadDirectory()).toMutableList()
//          requireActivity().openFile(it)
                findPdf(filesList)
            } else {
                binding.llStoragePermissionRationaleView.isVisible = true
                binding.rvDownloads.isVisible = false
            }

            withContext(Main) {
                downloadsAdapter.downloadsList = downloadsList
                downloadsAdapter.notifyDataSetChanged()
                binding.nestedScrollView.scrollTo(0, 0)
                binding.rvDownloads.runLayoutAnimation(globalLayoutAnimation)
            }
        }
    }

    private fun FragmentMainBinding.setupUI() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.layoutPersistentBottomSheet.root)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        requireActivity().setNavigationBarColor(R.color.white)
        rvDownloads.apply {
            layoutAnimation = rvDownloads.context.layoutAnimationController(globalLayoutAnimation)
            layoutManager = LinearLayoutManager(context)
            adapter = downloadsAdapter
        }
        ivShield.setMargins(top = (deviceHeight() / 2) - 200.dpToPx().toInt())
        layoutSearch.etSearch.hint = "Search in ${getDownloadDirectory().name}"
        setUpPersistentBottomSheet()

        ttsParams.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM.toString())
        checkTtsExists()

        layoutPersistentBottomSheet.layoutSliderPitch.tvSliderTitle.text = "Pitch"
        layoutPersistentBottomSheet.layoutSliderSpeed.tvSliderTitle.text = "Speed"
        layoutPersistentBottomSheet.layoutSliderPlayback.tvSliderTitle.text = "Book Progress"
    }

    private fun checkTtsExists() {
        val intent = Intent().apply {
            action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
        }
        ttsLauncher.launch(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun FragmentMainBinding.setupUserActionListeners() {
        root.setOnClickListener { }

        downloadsAdapter.setOnItemClickListener { download, position ->
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            val file = File(download?.path ?: "")
            val text = file.getTextFromPdf()
            layoutPersistentBottomSheet.tvCurrentlyReading.text = text
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, ttsParams, "")
        }

        btnGivePermission.onSafeClick {
            requireActivity().requestStoragePermission()
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
                downloadsAdapter.downloadsList = downloadsList
                downloadsAdapter.notifyDataSetChanged()
                return@doAfterTextChanged
            }
            downloadsAdapter.downloadsList = downloadsList.filter { it.title.contains(other = query, ignoreCase = true) }
            downloadsAdapter.notifyDataSetChanged()
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

    private fun observeForData() {
    }

    private fun findPdf(filesList: MutableList<File>) {
        for (it in filesList) {
            if (it.isFile) {
                if (it.extension().contains("pdf", true)) {
                    pdfList.add(it.absolutePath)
                    downloadsList.add(it.toDownload() ?: continue)
                }
            } else {
//                val innerFilesList = getFilesListFrom(folder = it).toMutableList()
//                findPdf(innerFilesList)
            }
        }
    }

    private fun File.toDownload(): Download? {
        if (this.exists().not()) return null
        val size = if (this.isDirectory) {
            "${getFilesListFrom(this).size} items"
        } else {
            if (this.extension.isBlank()) {
                this.getAppropriateSize()
            } else {
                "${this.extension.toUpCase()}  •  ${this.getAppropriateSize()}"
            }
        }
        return Download(
            path = this.absolutePath,
            title = this.nameWithoutExtension,
            time = this.lastModified(),
            size = size,
            link = "",
            extension = this.extension,
            isDirectory = this.isDirectory
        )
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

    override fun onInit(p0: Int) {
        binding.root.showSnackBar("Text-To-Speech engine is ready.")
        TTS_LANGUAGE_LIST.forEach { locale: Locale ->
            if (tts?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                availableLanguages.add(locale)
            }
        }
        tts?.setLanguage(Locale.US)
    }
}