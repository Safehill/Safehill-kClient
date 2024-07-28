package com.safehill.kcrypto.image_engine

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.safehill.kcrypto.image_engine.internal.ImageFilterConfigUtils
import com.safehill.kcrypto.image_engine.internal.ImageFilterWorker
import com.safehill.kcrypto.image_engine.model.ImageFilterConfig

object ImageFilterRepository {

    fun testFilter(context: Context, config: ImageFilterConfig) {
        val request = OneTimeWorkRequestBuilder<ImageFilterWorker>().setInputData(
            ImageFilterConfigUtils.toWorkData(config)
        ).build()

        WorkManager.getInstance(context).enqueue(request)
    }
}