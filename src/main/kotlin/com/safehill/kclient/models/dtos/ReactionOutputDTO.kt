package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.interactions.ReactionType
import kotlinx.serialization.Serializable

@Serializable
data class ReactionOutputDTO(
    override val interactionId: String,
    override val senderUserIdentifier: String,
    override val inReplyToAssetGlobalIdentifier: String?,
    override val inReplyToInteractionId: String?,
    override val reactionType: ReactionType,
    override val addedAt: String, // ISO8601 formatted datetime
) : UserReactionDTO