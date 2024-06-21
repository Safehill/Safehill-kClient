package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ConversationThreadAssetsDTO(
    val photoMessages: List<ConversationThreadAssetDTO>,
    val otherAssets: List<UserGroupAssetDTO>
)