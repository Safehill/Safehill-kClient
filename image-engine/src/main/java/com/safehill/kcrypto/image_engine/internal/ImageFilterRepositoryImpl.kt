package com.safehill.kcrypto.image_engine.internal

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.safehill.kcrypto.image_engine.ImageFilterRepository
import com.safehill.kcrypto.image_engine.model.CDNEnvironment
import com.safehill.kcrypto.image_engine.model.ImageFilterArgs
import com.safehill.kcrypto.image_engine.model.ImageFilterWorkState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transform

internal class ImageFilterRepositoryImpl(
    context: Context,
    private val cdnEnvironment: CDNEnvironment
): ImageFilterRepository {

    private val workManager by lazy {
        WorkManager.getInstance(context)
    }

    private class WorkTerminatedCancellationException: CancellationException()

    override fun applyFilterAsync(config: ImageFilterArgs): Flow<ImageFilterWorkState> {
        val request = OneTimeWorkRequestBuilder<ImageFilterWorker>()
            .setInputData(ImageFilterWorkerInputUtils.toWorkData(config, cdnEnvironment))
            .build()

        workManager.enqueueUniqueWork(IMAGE_FILTER_WORKER_TAG, ExistingWorkPolicy.KEEP, request)

        return workManager.getWorkInfosForUniqueWorkFlow(IMAGE_FILTER_WORKER_TAG).transform { workInfos ->
            val workInfoOfRequest = workInfos.firstOrNull { it.id == request.id }

            when (workInfoOfRequest?.state) {
                null -> Unit
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> emit(ImageFilterWorkState.Queued)
                WorkInfo.State.RUNNING -> emit(
                    if (workInfoOfRequest.progress.keyValueMap.isEmpty()) {
                        ImageFilterWorkState.Queued
                    } else {
                        ImageFilterWorkStateUtils.fromProgressData(workInfoOfRequest.progress)
                    }
                )
                WorkInfo.State.SUCCEEDED -> {
                    emit(ImageFilterWorkStateUtils.fromSuccessResultData(workInfoOfRequest.outputData))
                    currentCoroutineContext().cancel(WorkTerminatedCancellationException())
                }
                WorkInfo.State.FAILED ->  {
                    emit(ImageFilterWorkState.Error)
                    currentCoroutineContext().cancel(WorkTerminatedCancellationException())
                }
                WorkInfo.State.CANCELLED ->
                    currentCoroutineContext().cancel(WorkTerminatedCancellationException())
            }
        }.onCompletion {
            when (it) {
                null, is WorkTerminatedCancellationException -> Unit
                else -> workManager.cancelWorkById(request.id)
            }
        }
    }

    private companion object {
        const val IMAGE_FILTER_WORKER_TAG = "image_worker"
    }
}