package com.singularitycoder.playbooks.helpers

import com.facebook.shimmer.BuildConfig
import com.singularitycoder.playbooks.MainFragment
import com.singularitycoder.playbooks.R
import java.util.Locale

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

object IntentKey {
    const val LOCATION_TOGGLE_STATUS = "LOCATION_TOGGLE_STATUS"
}

object Db {
    const val CONNECT_ME = "db_connect_me"
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

val ebookReaderFormats = listOf(
    "pdf",
    "epub",
    "epub3",
    "mobi",
    "djvu",
    "fb2",
    "txt",
    "rtf",
    "azw",
    "azw3",
    "html",
    "cbz",
    "cbr",
    "doc",
    "docx",
    "opds"
)

val TTS_LANGUAGE_LIST = listOf(
    Locale.CANADA,
    Locale.CANADA_FRENCH,
    Locale.CHINA,
//    Locale.CHINESE,
//    Locale.ENGLISH,
    Locale.FRANCE,
//    Locale.FRENCH,
//    Locale.GERMAN,
    Locale.GERMANY,
//    Locale.ITALIAN,
    Locale.ITALY,
    Locale.JAPAN,
//    Locale.JAPANESE,
    Locale.KOREA,
//    Locale.KOREAN,
//    Locale.PRC,
//    Locale.SIMPLIFIED_CHINESE,
    Locale.TAIWAN,
//    Locale.TRADITIONAL_CHINESE,
    Locale.UK,
    Locale.US
).sortedBy { it.displayName }