package com.safehill.kcrypto.image_engine.model

import android.net.Uri

data class ImageFilterConfig(
    val inputImage: Uri,
    val type: Type
) {

    enum class Type(val id: String) {
        Cuphead("cuphead"),
        Mosaic("mosaic"),
        StarryNight("starry-night"),
    }
}