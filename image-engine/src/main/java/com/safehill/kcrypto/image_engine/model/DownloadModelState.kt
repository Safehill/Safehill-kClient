package com.safehill.kcrypto.image_engine.model

import java.io.File

sealed interface DownloadModelState {
    
    @JvmInline
    value class Success(val file: File): DownloadModelState

    data class Loading(val downloadBytes: Long, val totalBytes: Long): DownloadModelState

    data object Error: DownloadModelState
}