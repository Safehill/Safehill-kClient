package com.safehill.kclient.models

data class SHMessageOutputDTO(
    val interactionId: String,
    val senderUserIdentifier: String,
    val inReplyToAssetGlobalIdentifier: String?,
    val inReplyToInteractionId: String?,
    val encryptedMessage: SHReactionType,
    val createdAt: String, // ISO8601 formatted datetime
)