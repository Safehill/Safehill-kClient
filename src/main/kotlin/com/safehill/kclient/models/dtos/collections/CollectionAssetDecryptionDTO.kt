package com.safehill.kclient.models.dtos.collections

import com.safehill.kclient.models.dtos.AssetVersionInputDTO
import kotlinx.serialization.Serializable

@Serializable
data class CollectionAssetDecryptionDTO(
    /// The asset global identifier
    val assetGlobalIdentifier: String,
    /// Decryption details for each version of this asset
    val versionDecryptionDetails: List<AssetVersionInputDTO>
)
