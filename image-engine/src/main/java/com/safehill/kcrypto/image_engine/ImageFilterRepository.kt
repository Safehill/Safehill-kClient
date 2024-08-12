package com.safehill.kcrypto.image_engine

import android.content.Context
import com.safehill.kcrypto.image_engine.internal.ImageFilterRepositoryImpl
import com.safehill.kcrypto.image_engine.model.ImageFilterArgs
import com.safehill.kcrypto.image_engine.model.ImageFilterWorkState
import kotlinx.coroutines.flow.Flow

interface ImageFilterRepository {

    fun applyFilterAsync(config: ImageFilterArgs): Flow<ImageFilterWorkState>
}

fun ImageFilterRepository(context: Context): ImageFilterRepository =
    ImageFilterRepositoryImpl(context)