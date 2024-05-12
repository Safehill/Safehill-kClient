package com.safehill.kclient.api.dtos

import com.safehill.kclient.models.interactions.ReactionType
import com.safehill.kclient.models.interactions.UserReaction
import kotlinx.serialization.Serializable

@Serializable
data class ReactionOutputDTO(
    override val interactionId: String,
    override val senderUserIdentifier: String,
    override val inReplyToAssetGlobalIdentifier: String?,
    override val inReplyToInteractionId: String?,
    override val reactionType: ReactionType,
    override val addedAt: String, // ISO8601 formatted datetime
) : UserReaction