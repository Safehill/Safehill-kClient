package com.safehill.kclient.api.dtos

import com.safehill.kclient.api.AssetGlobalIdentifier
import kotlinx.serialization.Serializable

@Serializable
data class SHAssetSearchDTO(
    val globalIdentifiers: List<AssetGlobalIdentifier>,
    val versionNames: List<String>
) {}