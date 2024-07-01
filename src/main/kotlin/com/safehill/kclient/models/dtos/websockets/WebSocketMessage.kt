package com.safehill.kclient.models.dtos.websockets

import kotlinx.serialization.Serializable

sealed interface WebSocketMessage

sealed interface InteractionSocketMessage : WebSocketMessage

@Serializable
data class ConnectionAck(
    val userPublicIdentifier: String,
    val deviceId: String
) : WebSocketMessage

data object UnknownMessage : WebSocketMessage
