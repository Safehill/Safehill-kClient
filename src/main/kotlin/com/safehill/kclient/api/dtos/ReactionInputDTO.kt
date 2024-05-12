package com.safehill.kclient.api.dtos

import com.safehill.kclient.models.interactions.ReactionType

data class ReactionInputDTO(
    val inReplyToAssetGlobalIdentifier: String?,
    val inReplyToInteractionId: String?,
    val reactionType: ReactionType,
)