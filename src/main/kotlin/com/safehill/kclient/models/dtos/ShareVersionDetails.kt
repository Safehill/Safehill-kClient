package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ShareVersionDetails(
    val versionName: String,
    val recipientUserIdentifier: String,
    val recipientEncryptedSecret: String,
    val ephemeralPublicKey: String,
    val publicSignature: String
)
