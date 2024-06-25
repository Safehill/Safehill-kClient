package com.safehill.kclient.models.interactions

import kotlinx.serialization.SerialName

enum class InteractionAnchor(val value: String) {
    @SerialName("user-threads")
    THREAD("user-threads"),

    @SerialName("assets-groups")
    GROUP("assets-groups")
}