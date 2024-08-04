package com.singularitycoder.playbooks

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.singularitycoder.playbooks.helpers.WorkerData
import com.singularitycoder.playbooks.helpers.db.PlayBookDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

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
            val dao = dbEntryPoint.db().bookDao()
//            val networkStatus = dbEntryPoint.networkStatus()
            val rssUrl = inputData.getString(WorkerData.PDF_PATH)

            try {

                Result.success()
            } catch (_: Exception) {
                Result.failure()
            }
        }
    }
}