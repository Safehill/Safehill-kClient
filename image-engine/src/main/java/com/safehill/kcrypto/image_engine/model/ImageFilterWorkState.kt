package com.safehill.kcrypto.image_engine.model

import android.net.Uri

sealed interface ImageFilterWorkState {

    data class Running(
        val progress: Float,
        val step: Step
    ): ImageFilterWorkState {

        internal companion object {
            const val INDETERMINATE_PROGRESS_VALUE = -1f
        }

        enum class Step { DownloadResources, ProcessImage }
    }

    data object Queued: ImageFilterWorkState

    data class Success(val outputImageUri: Uri, val executionTime: Long): ImageFilterWorkState

    data object Error: ImageFilterWorkState
}