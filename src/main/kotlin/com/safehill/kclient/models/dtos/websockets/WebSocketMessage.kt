package com.safehill.kclient.models.dtos.websockets

sealed interface WebSocketMessage

sealed interface InteractionSocketMessage : WebSocketMessage

data object UnknownMessage : WebSocketMessage
