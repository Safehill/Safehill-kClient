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
import io.ktor.client.statement.discardRemaining
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

internal class MLModelsRepositoryImpl(context: Context): MLModelsRepository {

    private val httpClient by lazy { HttpClient(CIO) }
    private val modelsStorageDir = context.cacheDir.resolve("image-models")

    private fun downloadModel(model: MLModel): Flow<DownloadModelState> = channelFlow {
        httpClient.prepareGet(model.url) {
            onDownload { bytesSentTotal, contentLength ->
                trySend(DownloadModelState.Loading(bytesSentTotal, contentLength))
            }
        }.execute { response ->
            if (!response.status.isSuccess()) {
                response.cancel()
                trySend(DownloadModelState.Error)
                return@execute
            }

            val modelFile = model.file

            try {
                val channel = response.bodyAsChannel()

                modelFile.createNewFile()
                modelFile.outputStream().use { channel.copyTo(it) }
                trySend(DownloadModelState.Success(modelFile))
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }
                trySend(DownloadModelState.Error)
            } finally {
                modelFile.delete()
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

        modelsStorageDir.createDir()
        downloadModel(model).collect(::emit)
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