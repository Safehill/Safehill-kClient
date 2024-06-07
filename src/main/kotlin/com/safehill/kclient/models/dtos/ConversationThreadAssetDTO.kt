package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ConversationThreadAssetDTO(
    val globalIdentifier: String,
    val addedByUserIdentifier: String,
    val addedAt: String,
    val groupId: String
)