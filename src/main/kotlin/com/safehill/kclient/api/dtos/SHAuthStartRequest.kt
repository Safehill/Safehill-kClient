package com.safehill.kclient.api.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SHAuthStartRequest(
    val identifier: String,
    val name: String,
) {}