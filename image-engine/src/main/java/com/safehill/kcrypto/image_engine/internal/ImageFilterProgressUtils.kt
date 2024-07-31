package com.safehill.kcrypto.image_engine.internal

import androidx.work.Data
import androidx.work.workDataOf
import com.safehill.kcrypto.image_engine.model.ImageFilterProgress

internal object ImageFilterProgressUtils {

    private const val PROGRESS_KEY = "progress"
    private const val STEP_KEY = "step"
    private const val DOWNLOAD_RESOURCES_STEP_VALUE = "download_resources"
    private const val PROCESS_IMAGE_STEP_VALUE = "process_image"

    fun fromWorkData(workData: Data) = ImageFilterProgress(
        workData.getFloat(PROGRESS_KEY, ImageFilterProgress.INDETERMINATE_PROGRESS_VALUE),
        when (val it = workData.getString(STEP_KEY)) {
            DOWNLOAD_RESOURCES_STEP_VALUE -> ImageFilterProgress.Step.DownloadResources
            PROCESS_IMAGE_STEP_VALUE -> ImageFilterProgress.Step.ProcessImage
            else -> throw IllegalArgumentException("Unknown step $it")
        }
    )

    fun toWorkData(progress: ImageFilterProgress) = workDataOf(
        PROGRESS_KEY to progress.progress,
        STEP_KEY to when (progress.step) {
            ImageFilterProgress.Step.DownloadResources -> DOWNLOAD_RESOURCES_STEP_VALUE
            ImageFilterProgress.Step.ProcessImage -> PROCESS_IMAGE_STEP_VALUE
        }
    )
}