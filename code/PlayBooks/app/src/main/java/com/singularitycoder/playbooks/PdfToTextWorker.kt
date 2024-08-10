package com.singularitycoder.playbooks

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.singularitycoder.playbooks.helpers.WorkerData
import com.singularitycoder.playbooks.helpers.db.PlayBookDatabase
import com.singularitycoder.playbooks.helpers.extension
import com.singularitycoder.playbooks.helpers.getAppropriateSize
import com.singularitycoder.playbooks.helpers.getBookId
import com.singularitycoder.playbooks.helpers.getDownloadDirectory
import com.singularitycoder.playbooks.helpers.getFilesListFrom
import com.singularitycoder.playbooks.helpers.getTextFromPdf
import com.singularitycoder.playbooks.helpers.sizeInBytes
import com.singularitycoder.playbooks.helpers.toUpCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.File

class PdfToTextWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ThisEntryPoint {
        fun db(): PlayBookDatabase
//        fun networkStatus(): NetworkStatus
    }

    override suspend fun doWork(): Result {
        return withContext(IO) {
            val appContext = context.applicationContext ?: throw IllegalStateException()
            val dbEntryPoint = EntryPointAccessors.fromApplication(appContext, ThisEntryPoint::class.java)
            val bookDao = dbEntryPoint.db().bookDao()
            val bookDataDao = dbEntryPoint.db().bookDataDao()
//            val networkStatus = dbEntryPoint.networkStatus()
            val path = inputData.getString(WorkerData.PDF_PATH)

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
                        bookDataDao.insert(bookData = it.toBookData() ?: continue)
                        bookDao.insert(book = it.toBook() ?: continue)
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
//        val size = if (this.isDirectory) {
//            "${getFilesListFrom(this).size} items"
//        } else {
//            if (this.extension.isBlank()) {
//                this.getAppropriateSize()
//            } else {
//                "${this.extension.toUpCase()}  •  ${this.getAppropriateSize()}"
//            }
//        }

        return Book(
            id = this.getBookId(),
            path = this.absolutePath,
            title = this.nameWithoutExtension,
            time = this.lastModified(),
            size = this.getAppropriateSize(),
            link = "",
            extension = this.extension.toUpCase(),
            isDirectory = this.isDirectory,
        )
    }

    private fun File.toBookData(): BookData? {
        if (this.exists().not()) return null
        val text = this.getTextFromPdf() // this must be added to new table with foreign key
        return BookData(
            id = this.getBookId(),
            path = this.absolutePath,
            text = text
        )
    }
}