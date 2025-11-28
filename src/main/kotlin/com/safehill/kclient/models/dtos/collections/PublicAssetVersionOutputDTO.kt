package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.Serializable

@Serializable
data class PublicAssetVersionOutputDTO(
    val versionName: String,
    val timeUploaded: String? = null,
    /// Public URL for this asset version. During asset creation, this is a presigned upload URL.
    /// After upload, this is the public access URL for downloading.
    val publicURL: String
)
