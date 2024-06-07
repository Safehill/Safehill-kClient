package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.serde.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class MessageOutputDTO(
    override val interactionId: String,
    override val senderUserIdentifier: String,
    override val inReplyToAssetGlobalIdentifier: String?,
    override val inReplyToInteractionId: String?,
    override val encryptedMessage: String, // base64EncodedData with the cipher
    @Serializable(with = InstantSerializer::class) override val createdAt: Instant, // ISO8601 formatted datetime
) : UserMessageDTO