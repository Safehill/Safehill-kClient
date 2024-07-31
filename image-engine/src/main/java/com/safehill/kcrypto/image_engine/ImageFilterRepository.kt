package com.safehill.kcrypto.image_engine

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.safehill.kcrypto.image_engine.internal.ImageFilterConfigUtils
import com.safehill.kcrypto.image_engine.internal.ImageFilterWorkStateUtils
import com.safehill.kcrypto.image_engine.internal.ImageFilterWorker
import com.safehill.kcrypto.image_engine.model.ImageFilterConfig
import com.safehill.kcrypto.image_engine.model.ImageFilterWorkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ImageFilterRepository {

    private const val IMAGE_FILTER_WORKER_TAG = "image_worker"

    fun testFilter(
        context: Context,
        config: ImageFilterConfig
    ): Flow<ImageFilterWorkState?> {
        val request = OneTimeWorkRequestBuilder<ImageFilterWorker>()
            .setInputData(ImageFilterConfigUtils.toWorkData(config))
            .build()
        val workManager = WorkManager
            .getInstance(context)

        workManager.enqueueUniqueWork(IMAGE_FILTER_WORKER_TAG, ExistingWorkPolicy.KEEP, request)

        return workManager.getWorkInfosForUniqueWorkFlow(IMAGE_FILTER_WORKER_TAG).map { workInfos ->
            val workInfoOfRequest = workInfos.firstOrNull { it.id == request.id }

            when (workInfoOfRequest?.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> ImageFilterWorkState.Queued
                WorkInfo.State.RUNNING -> if (workInfoOfRequest.progress.keyValueMap.isEmpty()) {
                    ImageFilterWorkState.Queued
                } else {
                    ImageFilterWorkStateUtils.fromProgressData(workInfoOfRequest.progress)
                }
                WorkInfo.State.SUCCEEDED -> ImageFilterWorkState.Success
                WorkInfo.State.FAILED -> ImageFilterWorkState.Error
                WorkInfo.State.CANCELLED, null -> null
            }
        }
    }
}