package com.safehill.kclient.api.dtos

import com.safehill.kclient.models.SHUserMessage
import kotlinx.serialization.Serializable

@Serializable

data class SHMessageOutputDTO(
    override val interactionId: String,
    override val senderUserIdentifier: String,
    override val inReplyToAssetGlobalIdentifier: String?,
    override val inReplyToInteractionId: String?,
    override val encryptedMessage: String, // base64EncodedData with the cipher
    override val createdAt: String, // ISO8601 formatted datetime
) : SHUserMessage