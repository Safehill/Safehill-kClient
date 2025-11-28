package com.safehill.kclient.models.dtos.collections

import kotlinx.serialization.Serializable

@Serializable
data class CollectionChangeVisibilityRequestDTO(
    /// The new visibility for the collection ("confidential" or "public")
    val visibility: String,
    /// Decryption details for all assets in the collection, organized by asset
    /// Required when transitioning from not-shared to confidential or public
    /// Required when transitioning from confidential to public
    val assetDecryptionDetails: List<CollectionAssetDecryptionDTO>,
    /// Whether to delete asset versions that don't have decryption details provided
    val deleteOrphanedVersions: Boolean? = null
)

@Serializable
data class CollectionChangeVisibilityResultDTO(
    /// The collection ID that was processed
    val collectionId: String,
    /// The new visibility status of the collection
    val newVisibility: String,
    /// Whether link sharing is enabled (only applicable for confidential collections)
    val linkSharingEnabled: Boolean,
    /// Number of assets found in the collection
    val totalAssets: Int,
    /// Number of assets that were already in the target state
    val alreadyInTargetStateAssets: Int,
    /// Number of assets queued for publicization (only for public transitions)
    val queuedAssets: Int? = null,
    /// Number of asset versions that had server keys stored (only for confidential transitions)
    val processedVersions: Int? = null,
    /// Asset global identifiers that were processed
    val processedAssetIds: List<String>
)
