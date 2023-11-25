package com.safehill.kclient.api.dtos

import com.safehill.kclient.api.AssetGlobalIdentifier
import kotlinx.serialization.Serializable

@Serializable
data class SHAssetShareDTO(
    val globalIdentifier: AssetGlobalIdentifier,
    val versionSharingDetails: List<SHAssetVersionUserShareDTO>,
    val groupId: String?
) {}