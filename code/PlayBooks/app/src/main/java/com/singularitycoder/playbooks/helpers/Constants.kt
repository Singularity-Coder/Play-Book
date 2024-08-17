package com.singularitycoder.playbooks.helpers

import com.facebook.shimmer.BuildConfig
import com.singularitycoder.playbooks.MainFragment
import com.singularitycoder.playbooks.R

const val FILE_PROVIDER = "${BuildConfig.APPLICATION_ID}.fileprovider"

val globalLayoutAnimation = R.anim.layout_animation_fall_down
val globalSlideToBottomAnimation = R.anim.layout_animation_fall_down
val globalSlideToTopAnimation = R.anim.layout_animation_slide_from_bottom

val DUMMY_IMAGE_URLS = listOf(
    "https://images.pexels.com/photos/2850287/pexels-photo-2850287.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
)

object FragmentResultKey {
    const val RENAME_DOWNLOAD_FILE = "RENAME_DOWNLOAD_FILE"
}

object FragmentResultBundleKey {
    const val RENAME_DOWNLOAD_FILE = "RENAME_DOWNLOAD_FILE"
}

enum class NotificationAction {
    PREVIOUS_SENTENCE,
    NEXT_SENTENCE,
    PLAY_PAUSE,
    PREVIOUS_PAGE,
    NEXT_PAGE
}

object IntentKey {
    const val NOTIFICATION_BUTTON_CLICK_BROADCAST = "NOTIFICATION_BUTTON_CLICK_BROADCAST"
    const val MAIN_BROADCAST = "MAIN_BROADCAST"
}

object IntentExtraKey {
    const val MESSAGE = "MESSAGE"
    const val NOTIFICATION_BUTTON_CLICK_TYPE = "NOTIFICATION_BUTTON_CLICK_TYPE"
    const val BOOK_ID = "BOOK_ID"
}

object IntentExtraValue {
    const val UNBIND = "UNBIND"
    const val UPDATE_PROGRESS = "UPDATE_PROGRESS"
    const val READING_COMPLETE = "READING_COMPLETE"
    const val TTS_READY = "TTS_READY"
    const val SERVICE_DESTROYED = "SERVICE_DESTROYED"
    const val TTS_PAUSED = "TTS_PAUSED"
}

object Db {
    const val PLAY_BOOKS = "db_play_books"
}

object Table {
    const val BOOK = "table_book"
    const val BOOK_DATA = "table_book_data"
}

object BroadcastKey {
    const val LOCATION_TOGGLE_STATUS = "LOCATION_TOGGLE_STATUS"
}

object FragmentsTag {
    val MAIN: String = MainFragment::class.java.simpleName
}

object TtsTag {
    const val UID_SPEAK: String = "UID_SPEAK"
}

object BottomSheetTag {
    const val TAG_BOOK_READER_FILTERS = "TAG_BOOK_READER_FILTERS_BOTTOM_SHEET"
}

object WorkerData {
    const val PDF_PATH = "WORKER_DATA_RSS_URL"
    const val KEY_PROGRESS = "KEY_PROGRESS"
}

object WorkerTag {
    const val PDF_TO_TEXT_CONVERTER = "PDF_TO_TEXT_CONVERTER"
}

enum class MimeType(val value: String) {
    EPUB(value = "application/epub+zip"),
    PDF(value = "application/pdf"),
    TEXT(value = "text/plain"),
    ANY(value = "*/*"),
    FONT(value = "application/x-font-ttf")
}