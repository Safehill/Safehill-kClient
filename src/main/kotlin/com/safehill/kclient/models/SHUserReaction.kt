package com.safehill.kclient.models

interface SHUserReaction {
    val interactionId: String?
    val senderUserIdentifier: String?
    val inReplyToAssetGlobalIdentifier: String?
    val inReplyToInteractionId: String?
    val reactionType: SHReactionType
    val addedAt: String? // ISO8601 formatted datetime
}
