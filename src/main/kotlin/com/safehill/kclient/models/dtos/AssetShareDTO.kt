package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AssetShareDTO(
    val globalAssetIdentifier: String,
    val versionSharingDetails: List<ShareVersionDetails>,
    val groupId: String,
    val asPhotoMessageInThreadId: String?
)
