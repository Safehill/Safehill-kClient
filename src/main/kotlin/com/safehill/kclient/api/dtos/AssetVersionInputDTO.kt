package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
class AssetVersionInputDTO(
    val versionName: String,
    val senderEncryptedSecret: String,
    val ephemeralPublicKey: String,
    val publicSignature: String,
)
