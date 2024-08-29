package com.safehill.kcrypto.image_engine.internal

import android.net.Uri
import androidx.work.Data
import androidx.work.workDataOf
import com.safehill.kcrypto.image_engine.model.ImageFilterArgs

internal object ImageFilterConfigUtils {

    private const val INPUT_IMAGE_URI = "input_image_uri"
    private const val FILTER_TYPE = "filter_type"

    fun toWorkData(config: ImageFilterArgs) = workDataOf(
        INPUT_IMAGE_URI to config.inputImage.toString(),
        FILTER_TYPE to config.type.id
    )

    fun fromWorkData(workData: Data) = ImageFilterArgs(
        requireNotNull(Uri.parse(workData.getString(INPUT_IMAGE_URI))),
        requireNotNull(
            workData.getString(FILTER_TYPE)?.let { providedId ->
                ImageFilterArgs.Type.entries.firstOrNull { entry -> entry.id == providedId }
            }
        )
    )
}