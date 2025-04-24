package com.safehill.kclient.models.assets

enum class AssetQuality(
    val versionName: String,
    val dimension: Int
) {
    LowResolution("low", 480), MidResolution("mid", 1440), HighResolution("hi", 4800);

    companion object {
        fun fromVersionName(serverKey: String): AssetQuality =
            entries.first { it.versionName == serverKey }
    }
}
