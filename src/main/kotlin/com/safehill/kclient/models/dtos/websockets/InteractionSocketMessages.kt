package com.safehill.kclient.models.dtos.websockets

import com.safehill.kclient.models.dtos.ConversationThreadAssetDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.serde.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant


@Serializable
data class TextMessage(
    val interactionId: String,
    val anchorType: InteractionAnchor,
    val anchorId: String,
    val inReplyToAssetGlobalIdentifier: String?,
    val inReplyToInteractionId: String?,
    val senderPublicIdentifier: String,
    val senderPublicSignature: String, // base64Encoded signature
    val encryptedMessage: String,
    @Serializable(with = InstantSerializer::class) val sentAt: Instant // ISO8601 formatted datetime
) : InteractionSocketMessage

@Serializable
sealed class ReactionChange(
    val isAdded: Boolean,
    val reaction: Reaction
) : InteractionSocketMessage

@Serializable
data class Reaction(
    val interactionId: String?,
    val anchorType: String,
    val anchorId: String,
    val inReplyToAssetGlobalIdentifier: String?,
    val inReplyToInteractionId: String?,
    val senderPublicIdentifier: String,
    val reactionType: Int,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant// ISO8601 formatted datetime
)

@Serializable
data class ThreadCreated(
    val thread: ConversationThreadOutputDTO
) : InteractionSocketMessage


@Serializable
data class ThreadUpdatedDTO(
    val invitedUsersPhoneNumbers: Map<String, @Serializable(with = InstantSerializer::class) Instant>,
    @Serializable(with = InstantSerializer::class) val lastUpdatedAt: Instant,
    val membersPublicIdentifier: List<String>,
    val name: String,
    val threadId: String
) : InteractionSocketMessage

@Serializable
data class ThreadAssets(
    val threadId: String,
    val assets: List<ConversationThreadAssetDTO>
) : InteractionSocketMessage
