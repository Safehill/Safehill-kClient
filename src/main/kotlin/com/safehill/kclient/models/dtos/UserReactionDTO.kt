package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.interactions.ReactionType

interface UserReactionDTO {
    val interactionId: String?
    val senderUserIdentifier: String?
    val inReplyToAssetGlobalIdentifier: String?
    val inReplyToInteractionId: String?
    val reactionType: ReactionType
    val addedAt: String? // ISO8601 formatted datetime
}
