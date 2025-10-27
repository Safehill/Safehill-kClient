package com.safehill.clip_embeddings

import com.safehill.kclient.logging.SafehillLogger
import java.io.File

class ModelFileDownloader(
    private val modelFileValidator: ModelFileValidator,
    private val modelFileProvider: ModelFileProvider,
    private val logger: SafehillLogger,
    private val downloadDirectory: DownloadDirectory
) {

    suspend fun downloadAndSetModel() {
        val modelFile = downloadDirectory.getOnnxFile

        downloadModel(finalFile = modelFile)
            .takeIf { modelFileValidator.isFileValid(modelFile) }
            ?: throw IllegalStateException("Downloaded model file failed validation")

        modelFileProvider.setModelPath(modelFile)
    }

    private suspend fun downloadModel(finalFile: File) {
        createDownloadDirectoryIfItDoesNotExist()
        val tempFile = File(downloadDirectory.file, "TinyCLIP.onnx.zip.part")
        tempFile.delete()
        downloadFileFromUrl(url = CLIP_URL, destinationFile = tempFile)
        unzipFile(tempFile, finalFile)
        tempFile.delete()
    }

    private fun unzipFile(
        sourceFile: File,
        destinationFile: File
    ) {
        destinationFile.delete()
        sourceFile.inputStream().unZippedStreamFirstEntry().use {
            it.copyTo(destinationFile.outputStream())
        }
    }

    private fun createDownloadDirectoryIfItDoesNotExist() {
        if (!downloadDirectory.file.exists()) {
            downloadDirectory.file.mkdirs()
        }
    }
}


const val CLIP_URL =
    "https://s3.us-east-2.wasabisys.com/safehill-ml-prod/latest/TinyCLIP.onnx.zip"

const val CLIP_HASH_URL =
    "https://s3.us-east-2.wasabisys.com/safehill-ml-prod/latest/TinyCLIP.onnx.sha256"
