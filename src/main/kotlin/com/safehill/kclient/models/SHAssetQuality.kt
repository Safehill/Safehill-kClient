package com.safehill.kclient.models

enum class SHAssetQuality {
    LowResolution, MidResolution, HighResolution;

    override fun toString(): String {
        return when (this) {
            LowResolution -> "low"
            MidResolution -> "mid"
            HighResolution -> "hi"
        }
    }
}