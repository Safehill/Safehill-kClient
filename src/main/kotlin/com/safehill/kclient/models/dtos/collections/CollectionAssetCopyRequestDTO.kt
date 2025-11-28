package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.Serializable

@Serializable
data class CollectionAssetCopyRequestDTO(
    val sourceCollectionId: String,
    val targetCollectionId: String,
    val assetGlobalIdentifiers: List<String>
)

@Serializable
data class CollectionAssetCopyResultDTO(
    val success: Boolean,
    val message: String,
    val copiedCount: Int,
    val skippedCount: Int,
    val errors: List<String>? = null
)
