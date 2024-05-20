package com.safehill.kclient.models.assets

enum class AssetQuality {
    LowResolution, MidResolution, HighResolution;

    override fun toString(): String {
        return when (this) {
            LowResolution -> "low"
            MidResolution -> "mid"
            HighResolution -> "hi"
        }
    }
}
