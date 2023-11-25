package com.safehill.kclient.api.dtos

import com.safehill.kclient.api.AssetGlobalIdentifier
import kotlinx.serialization.Serializable

@Serializable
data class SHAssetIdentifiersDTO(
    val globalIdentifiers: List<AssetGlobalIdentifier>
)