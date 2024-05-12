package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class MessageInputDTO(
    val encryptedMessage: String, // base64EncodedData with the cipher
    val senderPublicSignature: String,
    val inReplyToAssetGlobalIdentifier: String?,
    val inReplyToInteractionId: String?
)
