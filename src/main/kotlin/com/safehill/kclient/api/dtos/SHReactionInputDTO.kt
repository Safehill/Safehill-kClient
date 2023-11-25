package com.safehill.kclient.api.dtos

import com.safehill.kclient.models.SHReactionType

data class SHReactionInputDTO(
    val inReplyToAssetGlobalIdentifier: String?,
    val inReplyToInteractionId: String?,
    val reactionType: SHReactionType,
)