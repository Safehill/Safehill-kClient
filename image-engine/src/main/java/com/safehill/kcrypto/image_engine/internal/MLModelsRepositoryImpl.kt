package com.safehill.kcrypto.image_engine.internal

import android.content.Context
import com.safehill.kcrypto.image_engine.model.DownloadModelState
import com.safehill.kcrypto.image_engine.model.MLModel
import com.safehill.kcrypto.image_engine.MLModelsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

internal class MLModelsRepositoryImpl(context: Context): MLModelsRepository {

    private val modelsStorageDir = context.cacheDir.resolve("image-models")

    private fun downloadModel(model: MLModel): Flow<DownloadModelState.Loading> = channelFlow {
        HttpClient(CIO).prepareGet(model.url) {
            onDownload { bytesSentTotal, contentLength ->
                trySend(DownloadModelState.Loading(bytesSentTotal, contentLength))
            }
        }.execute { response ->
            val channel = response.bodyAsChannel()

            try {
                model.file.outputStream().use { channel.copyTo(it) }
            } catch (e: Exception) {
                channel.cancel()
                throw e
            }
        }
    }

    override fun getOrDownloadModel(model: MLModel) = flow {
        val modelFile = model.file

        // Model already downloaded
        if (modelFile.isFile) {
            emit(DownloadModelState.Success(modelFile))
            return@flow
        }

        try {
            modelsStorageDir.createDir()
            modelFile.createNewFile()

            downloadModel(model).collect(::emit)

            emit(DownloadModelState.Success(modelFile))
        } catch (e: Exception) {
            emit(DownloadModelState.Error)
        }
    }.flowOn(Dispatchers.IO)

    private val MLModel.file get() = modelsStorageDir.resolve("$id.mnn")

    private companion object {

        /**
         * Create directory if it doesn't exist
         *
         * NOTE: THIS WILL OVERRIDE THE EXISTING FILE WITH THE SAME NAME IF FOUND
         */
        fun File.createDir(): Boolean {
            when {
                isDirectory -> return true
                isFile -> delete()
            }

            return mkdir()
        }
    }
}