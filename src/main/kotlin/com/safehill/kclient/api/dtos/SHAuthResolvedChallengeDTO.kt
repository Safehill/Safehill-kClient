package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHAuthResolvedChallengeDTO(
    val userIdentifier: String,
    val signedChallenge: String,
    val digest: String,
    val signedDigest: String
) {}