package com.safehill.kcrypto.image_engine.internal

import android.net.Uri
import androidx.work.Data
import androidx.work.workDataOf
import com.safehill.kcrypto.image_engine.model.CDNEnvironment
import com.safehill.kcrypto.image_engine.model.ImageFilterArgs

internal object ImageFilterWorkerInputUtils {

    private const val INPUT_IMAGE_URI = "input_image_uri"
    private const val FILTER_TYPE = "filter_type"
    private const val CDN_ENVIRONMENT_URL = "cdn_environment_url"

    fun toWorkData(
        config: ImageFilterArgs,
        cdnEnvironment: CDNEnvironment
    ) = workDataOf(
        INPUT_IMAGE_URI to config.inputImage.toString(),
        FILTER_TYPE to config.type.id,
        CDN_ENVIRONMENT_URL to cdnEnvironment.hostName
    )

    fun fromWorkData(
        workData: Data
    ): Pair<ImageFilterArgs, CDNEnvironment> = ImageFilterArgs(
        requireNotNull(Uri.parse(workData.getString(INPUT_IMAGE_URI))),
        requireNotNull(
            workData.getString(FILTER_TYPE)?.let { providedId ->
                ImageFilterArgs.Type.entries.firstOrNull { entry -> entry.id == providedId }
            }
        )
    ) to CDNEnvironment(requireNotNull(workData.getString(CDN_ENVIRONMENT_URL)))
}