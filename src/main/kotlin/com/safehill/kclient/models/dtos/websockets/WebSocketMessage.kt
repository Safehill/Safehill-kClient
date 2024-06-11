package com.safehill.kclient.models.dtos.websockets

import com.safehill.kclient.models.dtos.ConversationThreadAssetDTO
import com.safehill.kclient.models.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.models.interactions.InteractionAnchor
import com.safehill.kclient.models.serde.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

sealed interface WebSocketMessage

@Serializable
data class ConnectionAck(
    val userPublicIdentifier: String,
    val deviceId: String
) : WebSocketMessage


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
) : WebSocketMessage

@Serializable
sealed class ReactionChange(
    val isAdded: Boolean,
    val reaction: Reaction
) : WebSocketMessage

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
) : WebSocketMessage

@Serializable
data class ThreadAssets(
    val threadId: String,
    val assets: List<ConversationThreadAssetDTO>
) : WebSocketMessage

data object UnknownMessage : WebSocketMessage
