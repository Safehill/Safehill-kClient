package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.interactions.UserMessage
import kotlinx.serialization.Serializable

@Serializable

data class MessageOutputDTO(
    override val interactionId: String,
    override val senderUserIdentifier: String,
    override val inReplyToAssetGlobalIdentifier: String?,
    override val inReplyToInteractionId: String?,
    override val encryptedMessage: String, // base64EncodedData with the cipher
    override val createdAt: String, // ISO8601 formatted datetime
) : UserMessage