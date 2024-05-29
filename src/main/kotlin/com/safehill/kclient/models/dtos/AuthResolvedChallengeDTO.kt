package com.safehill.kclient.models.dtos

import kotlinx.serialization.Serializable

@Serializable
data class AuthResolvedChallengeDTO(
    val userIdentifier: String,
    val signedChallenge: String,
    val digest: String,
    val signedDigest: String
)
