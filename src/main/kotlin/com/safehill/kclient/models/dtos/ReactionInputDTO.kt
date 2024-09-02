package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ReactionInputDTO(
    val inReplyToAssetGlobalIdentifier: String?,
    val inReplyToInteractionId: String?,
    val reactionType: Int,
)