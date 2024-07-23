package com.safehill.kclient.models.assets

enum class AssetQuality(
    val value: String,
    val dimension: Int
) {
    LowResolution("low", 480), MidResolution("mid", 1440), HighResolution("hi", 4800);
}
