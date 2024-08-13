package com.safehill.kcrypto.image_engine.internal

import android.net.Uri
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.workDataOf
import com.safehill.kcrypto.image_engine.model.ImageFilterWorkState

internal object ImageFilterWorkStateUtils {

    private const val PROGRESS_KEY = "progress"
    private const val STEP_KEY = "step"
    private const val DOWNLOAD_RESOURCES_STEP_VALUE = "download_resources"
    private const val PROCESS_IMAGE_STEP_VALUE = "process_image"
    private const val EXECUTION_TIME_KEY = "execution_time"
    private const val OUTPUT_IMAGE_URI_KEY = "output_image_uri"

    fun fromProgressData(workData: Data) = ImageFilterWorkState.Running(
        workData.getFloat(PROGRESS_KEY, ImageFilterWorkState.Running.INDETERMINATE_PROGRESS_VALUE),
        when (val it = workData.getString(STEP_KEY)) {
            DOWNLOAD_RESOURCES_STEP_VALUE -> ImageFilterWorkState.Running.Step.DownloadResources
            PROCESS_IMAGE_STEP_VALUE -> ImageFilterWorkState.Running.Step.ProcessImage
            else -> throw IllegalArgumentException("Unknown step $it")
        }
    )

    fun toProgressData(progress: ImageFilterWorkState.Running) = workDataOf(
        PROGRESS_KEY to progress.progress,
        STEP_KEY to when (progress.step) {
            ImageFilterWorkState.Running.Step.DownloadResources -> DOWNLOAD_RESOURCES_STEP_VALUE
            ImageFilterWorkState.Running.Step.ProcessImage -> PROCESS_IMAGE_STEP_VALUE
        }
    )

    fun toSuccessResultData(outputImageUri: Uri, executionTime: Long) = workDataOf(
        OUTPUT_IMAGE_URI_KEY to outputImageUri.toString(),
        EXECUTION_TIME_KEY to executionTime
    )

    fun fromSuccessResultData(workData: Data): ImageFilterWorkState {
        val outputImageUri =
            workData.getString(OUTPUT_IMAGE_URI_KEY)?.toUri() ?: return ImageFilterWorkState.Error
        val executionTime =
            (workData.keyValueMap[EXECUTION_TIME_KEY] as? Long) ?: return ImageFilterWorkState.Error

        return ImageFilterWorkState.Success(outputImageUri, executionTime)
    }
}