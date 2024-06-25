package com.safehill.kclient.models

import kotlinx.serialization.Serializable

@Serializable
data class GenericFailureResponse(
    val error: Boolean,
    val reason: String?
)