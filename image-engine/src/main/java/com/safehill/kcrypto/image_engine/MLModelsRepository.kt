package com.safehill.kcrypto.image_engine

import android.content.Context
import com.safehill.kcrypto.image_engine.internal.MLModelsRepositoryImpl
import com.safehill.kcrypto.image_engine.model.DownloadModelState
import com.safehill.kcrypto.image_engine.model.MLModel
import kotlinx.coroutines.flow.Flow

interface MLModelsRepository {

    /**
     * Retrieve model from local storage or download it if it's not cached
     */
    fun getOrDownloadModel(model: MLModel): Flow<DownloadModelState>
}

fun MLModelsRepository(context: Context): MLModelsRepository =
    MLModelsRepositoryImpl(context)