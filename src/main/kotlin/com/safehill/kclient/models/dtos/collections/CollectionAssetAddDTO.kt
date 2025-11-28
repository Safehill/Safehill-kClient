package com.safehill.kclient.models.dtos.collections

import com.safehill.kclient.models.dtos.AssetInputDTO
import com.safehill.kclient.models.dtos.AssetOutputDTO
import kotlinx.serialization.Serializable

// MARK: - Request DTOs

@Serializable
data class CollectionAssetAddRequestDTO(
    val assets: List<AssetInputDTO>,
    /// For confidential and public collections: server decryption keys for assets being added
    val serverDecryptionDetails: List<CollectionAssetDecryptionDTO>? = null
)

// MARK: - Response DTOs

@Serializable
data class CollectionAssetAddResultDTO(
    val success: Boolean,
    val message: String,
    val addedCount: Int,
    val skippedCount: Int,
    val assets: List<AssetOutputDTO>? = null,
    val errors: List<String>? = null
)
