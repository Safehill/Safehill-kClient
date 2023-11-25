package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHAssetVersionUserShareDTO(
    val versionName: String, // the asset version name
    val recipientUserIdentifier: String, // the user to share it with
    val recipientEncryptedSecret: String, // base64EncodedData with the private key for the asset encrypted using the ephemeral public key and signature, in turn generated from the public key of the user it's been shared with
    val ephemeralPublicKey: String, // base64EncodedData with the ephemeral public key
    val publicSignature: String, // base64EncodedData with the ephemeral public signature
) {}
