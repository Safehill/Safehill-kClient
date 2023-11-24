package com.safehill.kclient.models

interface SHMessageInput {
    val interactionId: String?
    val senderUserIdentifier: String?
    val senderPublicSignature: String?
    val inReplyToAssetGlobalIdentifier: String?
    val inReplyToInteractionId: String?
    val encryptedMessage: String // base64EncodedData with the cipher
    val createdAt: String? // ISO8601 formatted datetime
}
