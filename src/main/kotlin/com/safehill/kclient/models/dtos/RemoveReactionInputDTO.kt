package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class RemoveReactionInputDTO(
    val reactionType: Int,
    val inReplyToInteractionId: String?,
    val inReplyToAssetGlobalIdentifier: String?
)