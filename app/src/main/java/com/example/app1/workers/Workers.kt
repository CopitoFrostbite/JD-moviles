package com.example.app1.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.app1.data.repository.JournalEntryRepository
import com.example.app1.data.repository.UserRepository
import com.example.app1.utils.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SyncWorker @Inject constructor(
    @ApplicationContext context: Context,
    workerParams: WorkerParameters,
    private val userRepository: UserRepository,
    private val journalEntryRepository: JournalEntryRepository
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return if (NetworkUtils.isNetworkAvailable(applicationContext)) {
            userRepository.syncData()
            Result.success()
        } else {
            Result.retry()
        }
    }
}