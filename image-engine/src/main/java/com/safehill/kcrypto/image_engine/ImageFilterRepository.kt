package com.safehill.kcrypto.image_engine

import android.content.Context
import com.safehill.kcrypto.image_engine.internal.ImageFilterRepositoryImpl
import com.safehill.kcrypto.image_engine.model.ImageFilterArgs
import com.safehill.kcrypto.image_engine.model.ImageFilterWorkState
import kotlinx.coroutines.flow.Flow

interface ImageFilterRepository {

    /**
     * Apply a filter on a image with the given [ImageFilterArgs] using a background worker
     *
     * @return a [Flow] emitting the state of the work. NOTE: the worker will be cancelled
     * if the flow collector is cancelled before it's completion
     */
    fun applyFilterAsync(config: ImageFilterArgs): Flow<ImageFilterWorkState>
}

fun ImageFilterRepository(context: Context): ImageFilterRepository =
    ImageFilterRepositoryImpl(context)