package com.safehill.kclient.models.dtos.websockets

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.users.RemoteUser
import kotlinx.serialization.Serializable

sealed interface WebSocketMessage

sealed interface InteractionSocketMessage : WebSocketMessage

@Serializable
data class ConnectionAck(
    val userPublicIdentifier: String,
    val deviceId: String
) : WebSocketMessage

@Serializable
data class NewConnectionRequest(
    val requestor: RemoteUser
) : WebSocketMessage


@Serializable
data class AssetDescriptorsChanged(
    val descriptors: List<AssetGlobalIdentifier>
) : WebSocketMessage

data object UnknownMessage : WebSocketMessage
