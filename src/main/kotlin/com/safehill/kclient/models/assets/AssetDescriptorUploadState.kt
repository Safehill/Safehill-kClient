package com.safehill.kclient.models.assets

enum class AssetDescriptorUploadState {
    NotStarted, Partial, Completed, Failed;

    override fun toString(): String {
        return when (this) {
            NotStarted -> "not_started"
            Partial -> "partial"
            Completed -> "completed"
            Failed -> "failed"
        }
    }
}
