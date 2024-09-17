package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AuthChallengeResponseDTO(
    val challenge: String,
    val ephemeralPublicKey: String, // base64EncodedData
    val ephemeralPublicSignature: String, // base64EncodedData
    val publicKey: String, // base64EncodedData
    val publicSignature: String, // base64EncodedData
    val protocolSalt: String, // base64EncodedData
    val iv: String? // base64EncodedData
)