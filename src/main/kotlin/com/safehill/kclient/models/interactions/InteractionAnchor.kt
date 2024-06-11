package com.safehill.kclient.models.interactions

import kotlinx.serialization.SerialName

enum class InteractionAnchor {
    @SerialName("user-threads")
    THREAD,

    @SerialName("assets-groups")
    GROUP
}