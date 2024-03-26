package com.safehill.kclient.api.dtos

import com.safehill.kclient.models.SHReactionType
import com.safehill.kclient.models.SHUserReaction
import kotlinx.serialization.Serializable

@Serializable
data class SHReactionOutputDTO(
    override val interactionId: String,
    override val senderUserIdentifier: String,
    override val inReplyToAssetGlobalIdentifier: String?,
    override val inReplyToInteractionId: String?,
    override val reactionType: SHReactionType,
    override val addedAt: String, // ISO8601 formatted datetime
) : SHUserReaction