package com.safehill.kcrypto.image_engine.model

enum class MLModel(val id: String) {
    Cuphead(
        "cuphead"
    ),
    Mosaic(
        "mosaic"
    ),
    StarryNight(
        "starry_night"
    );

    val url: String get() = "$MODELS_DOWNLOAD_URL_PREFIX$id"

    private companion object {

        const val MODELS_DOWNLOAD_URL_PREFIX =
            "https://s3.us-east-1.wasabisys.com/safehill-stage/models/style-transfer/"
    }
}