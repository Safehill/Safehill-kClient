package com.safehill.clip_embeddings

import com.safehill.kclient.logging.SafehillLogger

class ModelFileManager internal constructor(
    val modelFileProvider: ModelFileProvider,
    private val modelFileValidator: ModelFileValidator,
    private val downloadDirectory: DownloadDirectory,
    private val logger: SafehillLogger,
    private val modelFileDownloadScheduler: ModelFileDownloadScheduler
) {

    suspend fun ensureModelReady() {
        val file = downloadDirectory.getOnnxFile
        if (modelFileValidator.isFileValid(file)) {
            modelFileProvider.setModelPath(file)
        } else {
            modelFileDownloadScheduler.scheduleDownload()
        }
    }

    val modelFileDownloader
        get() = ModelFileDownloader(
            modelFileProvider = modelFileProvider,
            modelFileValidator = modelFileValidator,
            downloadDirectory = downloadDirectory,
            logger = logger
        )

    class Factory(
        val downloadDirectory: DownloadDirectory,
        val modelFileDownloadScheduler: ModelFileDownloadScheduler,
        val logger: SafehillLogger
    ) {

        fun build(): ModelFileManager {
            val modelFileValidator = ModelFileValidator(
                downloadDirectory = downloadDirectory
            )
            val modelFileProvider = ModelFileProvider(
                downloadDirectory = downloadDirectory,
                logger = logger
            )
            return ModelFileManager(
                modelFileProvider = modelFileProvider,
                modelFileValidator = modelFileValidator,
                downloadDirectory = downloadDirectory,
                modelFileDownloadScheduler = modelFileDownloadScheduler,
                logger = logger
            )
        }
    }
}

