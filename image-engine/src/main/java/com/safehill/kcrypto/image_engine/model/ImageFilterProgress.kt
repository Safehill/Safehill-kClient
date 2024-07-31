package com.safehill.kcrypto.image_engine.model

data class ImageFilterProgress(
    val progress: Float,
    val step: Step
) {

    internal companion object {
        const val INDETERMINATE_PROGRESS_VALUE = -1f
    }

    enum class Step { DownloadResources, ProcessImage }
}