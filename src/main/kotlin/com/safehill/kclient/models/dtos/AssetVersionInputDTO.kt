package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
class AssetVersionInputDTO(
    val versionName: String,
    val senderEncryptedSecret: String,
    val ephemeralPublicKey: String,
    val publicSignature: String,
)
