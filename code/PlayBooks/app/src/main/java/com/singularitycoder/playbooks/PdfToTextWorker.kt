package com.singularitycoder.playbooks

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.singularitycoder.playbooks.helpers.WorkerData
import com.singularitycoder.playbooks.helpers.db.PlayBooksDatabase
import com.singularitycoder.playbooks.helpers.extension
import com.singularitycoder.playbooks.helpers.getAppropriateSize
import com.singularitycoder.playbooks.helpers.getBookCoversFileDir
import com.singularitycoder.playbooks.helpers.getBookId
import com.singularitycoder.playbooks.helpers.getDownloadDirectory
import com.singularitycoder.playbooks.helpers.getFilesListFrom
import com.singularitycoder.playbooks.helpers.saveToStorage
import com.singularitycoder.playbooks.helpers.sizeInBytes
import com.singularitycoder.playbooks.helpers.toPdfFirstPageBitmap
import com.singularitycoder.playbooks.helpers.toUpCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.File

/** For setting progress - https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/observe */
class PdfToTextWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private var pageCount = 0

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ThisEntryPoint {
        fun db(): PlayBooksDatabase
    }

    override suspend fun doWork(): Result {
        return withContext(IO) {
            val appContext = context.applicationContext ?: throw IllegalStateException()
            val dbEntryPoint = EntryPointAccessors.fromApplication(appContext, ThisEntryPoint::class.java)
            val bookDao = dbEntryPoint.db().bookDao()
            val bookDataDao = dbEntryPoint.db().bookDataDao()

            try {
                val filesList = getFilesListFrom(folder = getDownloadDirectory()).sortedBy { it.sizeInBytes() } // sorted in asc to convert smaller books first
                findPdf(filesList, bookDao, bookDataDao)

                Result.success()
            } catch (_: Exception) {
                Result.failure()
            }
        }
    }

    private suspend fun findPdf(
        filesList: List<File>,
        bookDao: BookDao,
        bookDataDao: BookDataDao,
    ) {
        for (it in filesList) {
            if (it.isFile) {
                if (it.extension().contains(other = "pdf", ignoreCase = true)) {
                    if (bookDao.isItemPresent(id = it.getBookId()).not()) {
                        val bookData = it.toBookData() ?: continue
                        val book = it.toBook() ?: continue
                        it.toPdfFirstPageBitmap().saveToStorage(
                            fileName = "${it.getBookId()}.jpg",
                            fileDir = context.getBookCoversFileDir()
                        )
                        bookDataDao.insert(bookData = bookData)
                        bookDao.insert(book = book)
                    }
                }
            } else {
//                val innerFilesList = getFilesListFrom(folder = it).toMutableList()
//                findPdf(innerFilesList)
            }
        }
    }

    private fun File.toBook(): Book? {
        if (this.exists().not()) return null

        return Book(
            id = this.getBookId(),
            path = this.absolutePath,
            title = this.nameWithoutExtension,
            time = this.lastModified(),
            size = this.getAppropriateSize(),
            link = "",
            extension = this.extension.toUpCase(),
            isDirectory = this.isDirectory,
            pageCount = pageCount
        )
    }

    private suspend fun File.toBookData(): BookData? {
        if (this.exists().not()) return null

        val pdfBook = this.getTextFromPdf { progress: Int, totalPages: Int ->
            setProgress(workDataOf(WorkerData.KEY_PROGRESS to ((progress * 100) / totalPages)))
        }

        pdfBook ?: return null
        if (pdfBook.text.isNullOrBlank()) return null
        if (pdfBook.periodCountPerPageList.sum() == 0) return null
        if (pdfBook.periodPositionsList.sum() == 0) return null

        pageCount = pdfBook.pageCount
        return BookData(
            id = this.getBookId(),
            path = this.absolutePath,
            text = pdfBook.text,
            pageCount = pdfBook.pageCount,
            periodCountPerPageList = pdfBook.periodCountPerPageList,
            periodPositionsList = pdfBook.periodPositionsList,
            periodPosToPageNumMap = pdfBook.periodPosToPageNumMap,
            pageNumToPeriodLengthMap = pdfBook.pageNumToPeriodLengthMap,
            periodLengthToPageNumMap = pdfBook.periodLengthToPageNumMap
        )
    }

    // https://stackoverflow.com/questions/58750885/how-can-i-convert-pdf-file-to-text
    private suspend fun File.getTextFromPdf(
        callback: suspend (progress: Int, totalPages: Int) -> Unit
    ): PdfBook? = try {
        var parsedText = ""
        val periodCountPerPageList = mutableListOf<Int>()
        val periodPositionsList = mutableListOf<Int>()
        val periodPosToPageNumMap = HashMap<String, Int>()
        val pageNumToPeriodLengthMap = HashMap<String, Int>()
        val periodLengthToPageNumMap = HashMap<String, Int>()
        val reader = PdfReader(this.absolutePath)
        var totalPeriodCount = 0
        for (i in 0 until reader.numberOfPages) {
            val pageString = PdfTextExtractor.getTextFromPage(reader, i + 1)
            var periodCountPerPage = 0
            pageString.forEachIndexed { index, char ->
                if (char == '.') {
                    periodCountPerPage++
                    totalPeriodCount++
                    periodPositionsList.add(parsedText.length + index)
                    /** Map period position to page */
                    periodPosToPageNumMap[(parsedText.length + index).toString()] = i
                    /** Map the page to only last period position. Since map it will store only last iteration value */
                    pageNumToPeriodLengthMap[i.toString()] = totalPeriodCount
                    /** Map period length to page to advance to next page properly */
                    periodLengthToPageNumMap[totalPeriodCount.toString()] = i
                }
            }
            // Extracting the content from different pages - add new lines if necessary - "$parsedText${pageString.trim { it <= ' ' }}\n\n\n\n"
            parsedText = "$parsedText${pageString.trim { it <= ' ' }}"
            periodCountPerPageList.add(periodCountPerPage) // option 1: This will the position of next page. Take last period position of each page
            callback.invoke(i, reader.numberOfPages)
        }
        reader.close()
        PdfBook(
            pageCount = reader.numberOfPages,
            text = parsedText.replace("\n", " "),
            periodCountPerPageList = periodCountPerPageList,
            periodPositionsList = periodPositionsList,
            periodPosToPageNumMap = periodPosToPageNumMap,
            pageNumToPeriodLengthMap = pageNumToPeriodLengthMap,
            periodLengthToPageNumMap = periodLengthToPageNumMap
        )
    } catch (_: Exception) {
        null
    }
}