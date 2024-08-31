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

    internal fun getDownloadUrl(env: CDNEnvironment) =
        "https://${env.hostName}/models/style-transfer/$id.mnn"
}