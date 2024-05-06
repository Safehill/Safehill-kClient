package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHInteractionsGroupDTO(
    val messages: List<SHMessageOutputDTO>,
    val reactions: List<SHReactionOutputDTO>,

    val ephemeralPublicKey: String, // base64EncodedData with the ephemeral public part of the key used for the encryption
    val encryptedSecret: String, // base64EncodedData with the secret to decrypt the encrypted content in this group for this user
    val secretPublicSignature: String, // base64EncodedData with the public signature of the user sending it
    val senderPublicSignature: String
)