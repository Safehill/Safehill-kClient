package com.safehill.kclient.models.dtos

import com.safehill.kclient.models.serde.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ReactionOutputDTO(
    val interactionId: String,
    val senderUserIdentifier: String,
    val inReplyToAssetGlobalIdentifier: String?,
    val inReplyToInteractionId: String?,
    val reactionType: Int,
    @Serializable(with = InstantSerializer::class) val addedAt: Instant,
    val senderPublicIdentifier: String,
)