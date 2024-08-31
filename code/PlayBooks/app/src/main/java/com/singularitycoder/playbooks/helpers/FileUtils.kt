package com.singularitycoder.playbooks.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val KB = 1024.0
const val MB = 1024.0 * KB
const val GB = 1024.0 * MB
const val TB = 1024.0 * GB

fun File.sizeInBytes(): Int {
    if (!this.exists()) return 0
    return this.length().toInt()
}

/** metric is KB, MB, GB, TB constants which is 1024.0, etc */
fun File.showSizeIn(metric: Double): Double {
    if (!this.exists()) return 0.0
    return this.sizeInBytes().div(metric)
}

fun File.extension(): String {
    if (!this.exists()) return ""
    return this.absolutePath.substringAfterLast(delimiter = ".").lowercase().trim()
}

fun File.nameWithExtension(): String {
    if (!this.exists()) return ""
    return this.absolutePath.substringAfterLast(delimiter = "/")
}

fun File.name(): String {
    if (!this.exists()) return ""
    return this.nameWithExtension().substringBeforeLast(".")
}

// https://stackoverflow.com/questions/15662258/how-to-save-a-bitmap-on-internal-storage
suspend fun Bitmap?.saveToStorage(
    fileName: String,
    fileDir: String,
) {
    suspendCoroutine<Unit> { cont: Continuation<Unit> ->
//    val root: String = Environment.getExternalStorageDirectory().absolutePath
        val directory = File(fileDir).also {
            if (it.exists().not()) it.mkdirs()
        }
        val file = File(/* parent = */ directory, /* child = */ fileName).also {
            if (it.exists().not()) it.createNewFile() else return@suspendCoroutine
        }
        try {
            val out = FileOutputStream(file)
            this?.compress(Bitmap.CompressFormat.JPEG, 50, out)
            cont.resume(Unit)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun File?.toBitmap(): Bitmap? {
    return BitmapFactory.decodeFile(this?.absolutePath)
}

fun Context.getBookCoversFileDir(): String {
    return "${filesDir.absolutePath}/book_covers"
}

/** Checks if a volume containing external storage is available for read and write. */
fun isExternalStorageWritable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

/**
 * The idea is to replace all special characters with underscores
 * 48 to 57 are ASCII characters of numbers from 0 to 1
 * 97 to 122 are ASCII characters of lowercase alphabets from a to z
 * https://www.w3schools.com/charsets/ref_html_ascii.asp
 * */
fun String?.sanitize(): String {
    if (this.isNullOrBlank()) return ""
    var sanitizedString = ""
    val range0to9 = '0'.code..'9'.code
    val rangeLowerCaseAtoZ = 'a'.code..'z'.code
    val rangeUpperCaseAtoZ = 'A'.code..'Z'.code
    this.forEachIndexed { index: Int, char: Char ->
        if (char.code !in range0to9 && char.code !in rangeLowerCaseAtoZ && char.code !in rangeUpperCaseAtoZ) {
            if (sanitizedString.lastOrNull() != '_' && this.lastIndex != index) {
                sanitizedString += "_"
            }
        } else {
            sanitizedString += char
        }
    }
    return sanitizedString
}

// https://github.com/android/storage-samples/tree/main/ActionOpenDocumentTree
fun getFilesListFrom(folder: File): List<File> {
    val rawFilesList = folder.listFiles()?.filter { !it.isHidden }

    return if (folder == Environment.getExternalStorageDirectory()) {
        rawFilesList?.toList() ?: listOf()
    } else {
        listOf(folder.parentFile) + (rawFilesList?.toList() ?: listOf())
    }
}

fun File.getAppropriateSize(): String {
    return when {
        this.showSizeIn(MB) < 1 -> {
            "${String.format("%1.2f", this.showSizeIn(KB))} KB"
        }

        this.showSizeIn(MB) >= GB -> {
            "${String.format("%1.2f", this.showSizeIn(GB))} GB"
        }

        else -> {
            "${String.format("%1.2f", this.showSizeIn(MB))} MB"
        }
    }
}

/** /storage/emulated/0/Download/ */
fun getDownloadDirectory(): File {
    return File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DOWNLOADS}/").also {
        if (it.exists().not()) it.mkdirs()
    }
}

fun hasPdfs(): Boolean {
    return getFilesListFrom(folder = getDownloadDirectory()).any { it: File ->
        it.isFile && it.extension().contains(other = "pdf", ignoreCase = true)
    }
}

// https://developer.android.com/reference/android/graphics/pdf/PdfRenderer
// https://stackoverflow.com/questions/6715898/what-is-parcelfiledescriptor-in-android
// https://github.com/robolectric/robolectric/blob/master/robolectric/src/test/java/org/robolectric/shadows/ShadowParcelFileDescriptorTest.java
suspend fun File.toPdfFirstPageBitmap(): Bitmap? {
    return suspendCoroutine<Bitmap?> { cont: Continuation<Bitmap> ->
        var bitmap: Bitmap? = null
        try {
            val pfd = ParcelFileDescriptor.open(
                /* file = */ this,
                /* mode = */ ParcelFileDescriptor.MODE_READ_WRITE
            )
            val renderer = PdfRenderer(pfd) // create a new renderer
            // render all pages
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                val page: PdfRenderer.Page = renderer.openPage(i)
                bitmap = Bitmap.createBitmap(
                    /* width = */ page.width,
                    /* height = */ page.height,
                    /* config = */ Bitmap.Config.ARGB_8888
                )

                // say we render for showing on the screen
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                // do stuff with the bitmap
                cont.resume(bitmap)
                page.close()
                break // quiting loop since we only want first page
            }
            renderer.close()
        } catch (_: Exception) {
        }
    }
}

inline fun deleteAllFilesFrom(
    directory: File?,
    crossinline onDone: () -> Unit = {}
) {
    CoroutineScope(Dispatchers.Default).launch {
        directory?.listFiles()?.forEach files@{ it: File? ->
            it ?: return@files
            if (it.exists().not()) return@files
            it.delete()
        }

        withContext(Dispatchers.Main) { onDone.invoke() }
    }
}

fun deleteFileFrom(path: String) {
    CoroutineScope(Dispatchers.Default).launch {
        val file = File(path)
        if (file.exists()) file.delete()
    }
}