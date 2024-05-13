package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.interactions.ReactionType

data class ReactionInputDTO(
    val inReplyToAssetGlobalIdentifier: String?,
    val inReplyToInteractionId: String?,
    val reactionType: ReactionType,
)