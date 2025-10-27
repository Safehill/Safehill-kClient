package com.safehill.clip_embeddings

import com.safehill.kclient.logging.DefaultSafehillLogger
import com.safehill.kclient.logging.SafehillLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.io.File

class ModelFileProvider(
    private val downloadDirectory: DownloadDirectory,
    private val logger: SafehillLogger = DefaultSafehillLogger()
) {

    private val modelPath = MutableStateFlow<File?>(null)

    suspend fun getModelFile(): File {
        return modelPath.filterNotNull().first()
    }

    fun setModelPath(file: File) {
        modelPath.update { file }
    }
}
