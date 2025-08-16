package com.safehill.clip_embeddings

import com.safehill.kclient.logging.DefaultSafehillLogger
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.util.runCatchingSafe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File


class ModelFileProvider(
    private val downloadDirectory: File,
    private val logger: SafehillLogger = DefaultSafehillLogger()
) {


    private val modelPath = MutableStateFlow<File?>(null)

    suspend fun getModelFile(): File {
        logger.info("[ModelFileProvider] Getting model file from path")
        return modelPath.filterNotNull().first()
    }

    fun setModelPath(file: File) {
        logger.info("[ModelFileProvider] Setting model path to: ${file.absolutePath}")
        modelPath.update { file }
    }

    suspend fun downloadModelIfNeeded() {
        logger.info("[ModelFileProvider] Starting model download process")
        return withContext(Dispatchers.IO) {
            val client = HttpClient(CIO) {
                engine {
                    requestTimeout = 0

                }
            }
            val finalFile = File(downloadDirectory, "TinyCLIP.onnx")
            try {
                createDownloadDirectoryIfItDoesNotExist()
                logger.info("[ModelFileProvider] Checking if final file is valid")
                val isValid = client.isFinalFileValid(finalFile)
                if (isValid) {
                    logger.info("[ModelFileProvider] Final file is valid, setting model path")
                    setModelPath(finalFile)
                } else {
                    logger.info("[ModelFileProvider] Final file is not valid, downloading model")
                    client.downloadModel(finalFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("[ModelFileProvider] Failed to download CLIP model")
            } finally {
                logger.info("[ModelFileProvider] Closing HTTP client")
                client.close()
            }
        }
    }

    private suspend fun HttpClient.downloadModel(
        finalFile: File
    ) {
        logger.info("[ModelFileProvider] Starting model download to: ${finalFile.absolutePath}")
        val tempFile = File(downloadDirectory, "TinyCLIP.onnx.zip.part").apply {
            if (exists()) {
                delete()
            }
        }
        try {
            logger.info("[ModelFileProvider] Downloading model from: $CLIP_URL")
            prepareGet(CLIP_URL).execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                channel.copyTo(tempFile.writeChannel())
            }
            logger.info("[ModelFileProvider] Download completed, unzipping file")
            unzipFile(tempFile, finalFile)
            logger.info("[ModelFileProvider] Model file ready, setting model path")
            setModelPath(finalFile)
        } finally {
            logger.info("[ModelFileProvider] Cleaning up temporary file")
            tempFile.delete()
        }
    }


    private fun unzipFile(
        tempFile: File,
        finalFile: File
    ) {
        logger.info("[ModelFileProvider] Unzipping file from ${tempFile.absolutePath} to ${finalFile.absolutePath}")
        if (finalFile.exists()) {
            logger.info("[ModelFileProvider] Deleting existing final file before unzipping")
            finalFile.delete()
        }
        tempFile.inputStream().unZippedStreamFirstEntry().use {
            it.copyTo(finalFile.outputStream())
        }
        logger.info("[ModelFileProvider] File unzipped successfully")
    }

    private suspend fun HttpClient.isFinalFileValid(finalFile: File): Boolean {
        if (!finalFile.exists() || finalFile.length() == 0L) {
            return false
        }
        val hash = finalFile.sha256()
        val hashFile = File(downloadDirectory, "TinyCLIP.onnx.sha256")
        val expectedHash = runCatchingSafe {
            val hashFromServer = get(CLIP_HASH_URL).body<String>()
            hashFile.writeBytes(hashFromServer.toByteArray())
            hashFromServer
        }.getOrNull() ?: String(hashFile.readBytes())
        val isValid = hash.trim() == expectedHash.trim()
        logger.info("[ModelFileProvider] File validation result: $isValid (local: $hash, expected: $expectedHash)")
        return isValid
    }

    private fun createDownloadDirectoryIfItDoesNotExist() {
        if (!downloadDirectory.exists()) {
            downloadDirectory.mkdirs()
        }
    }
}


private const val CLIP_URL =
    "https://s3.us-east-2.wasabisys.com/safehill-ml-prod/latest/TinyCLIP.onnx.zip"

private const val CLIP_HASH_URL =
    "https://s3.us-east-2.wasabisys.com/safehill-ml-prod/latest/TinyCLIP.onnx.sha256"