package com.safehill.kclient.models.dtos

import java.time.Instant


interface UserMessageDTO {
    val interactionId: String?
    val senderUserIdentifier: String?
    val inReplyToAssetGlobalIdentifier: String?
    val inReplyToInteractionId: String?
    val encryptedMessage: String // base64EncodedData with the cipher
    val createdAt: Instant? // ISO8601 formatted datetime
}
