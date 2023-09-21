package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHDeleteAssetRequest(
    val globalIdentifiers: List<String>
)