package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHMessageInputDTO(
    val inReplyToAssetGlobalIdentifier: String,
    val inReplyToInteractionId: String,
    val encryptedMessage: String, // base64EncodedData with the cipher
    val createdAt: String, // ISO8601 formatted datetime
)
