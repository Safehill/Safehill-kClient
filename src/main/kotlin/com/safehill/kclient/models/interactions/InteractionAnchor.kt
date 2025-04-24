package com.safehill.kclient.models.interactions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InteractionAnchor(val value: String) {
    @SerialName("user-threads")
    THREAD("user-threads"),

    @SerialName("assets-groups")
    GROUP("assets-groups");

    companion object {
        fun from(value: String): InteractionAnchor {
            return entries.first { it.value == value }
        }
    }
}