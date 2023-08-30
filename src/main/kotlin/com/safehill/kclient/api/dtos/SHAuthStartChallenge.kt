package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHAuthStartChallenge(
    val identifier: String,
    val name: String,
) {}