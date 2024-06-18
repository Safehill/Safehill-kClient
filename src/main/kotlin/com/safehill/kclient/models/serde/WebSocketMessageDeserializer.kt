package com.safehill.kclient.models.serde

import com.safehill.kclient.models.dtos.websockets.ConnectionAck
import com.safehill.kclient.models.dtos.websockets.NewConnectionRequest
import com.safehill.kclient.models.dtos.websockets.ReactionChange
import com.safehill.kclient.models.dtos.websockets.TextMessage
import com.safehill.kclient.models.dtos.websockets.ThreadAssets
import com.safehill.kclient.models.dtos.websockets.ThreadCreated
import com.safehill.kclient.models.dtos.websockets.UnknownMessage
import com.safehill.kclient.models.dtos.websockets.WebSocketMessage
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object WebSocketMessageDeserializer : DeserializationStrategy<WebSocketMessage> {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        explicitNulls = false
    }
    override val descriptor: SerialDescriptor
        get() = WebSocketDataSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): WebSocketMessage {
        val socketData = decoder.decodeSerializableValue(
            WebSocketDataSurrogate.serializer()
        )
        return socketData.obtainWebSocketMessage()
    }

    private fun WebSocketDataSurrogate.obtainWebSocketMessage(): WebSocketMessage {
        val messageType = MessageType.entries.find {
            it.key == this.type
        } ?: return UnknownMessage

        val content = this.getContentData(messageType)
        return json.decodeFromString(messageType.deserializer, content)
    }

    private fun WebSocketDataSurrogate.getContentData(messageType: MessageType): String {
        return when (messageType) {
            MessageType.CONNECTION_ACK,
            MessageType.MESSAGE,
            MessageType.ASSETS_SHARE,
            MessageType.CONNECTION_REQUEST -> {
                this.content
            }

            MessageType.REACTION_ADD, MessageType.REACTION_REMOVE -> {
                val reaction = parseToJsonElement(this.content)
                reaction.parseReactionToRequiredFormat(messageType).toString()
            }

            MessageType.THREAD_ADD -> {
                val thread = parseToJsonElement(this.content)
                thread.parseThreadToRequiredFormat().toString()
            }
        }
    }

    private fun JsonElement.parseReactionToRequiredFormat(messageType: MessageType): JsonObject {
        return JsonObject(
            mapOf(
                "isAdded" to JsonPrimitive((messageType == MessageType.REACTION_ADD)),
                "reaction" to this
            )
        )
    }

    private fun JsonElement.parseThreadToRequiredFormat(): JsonObject {
        return JsonObject(
            mapOf(
                "thread" to this
            )
        )
    }
}

enum class MessageType(
    val key: String,
    val deserializer: DeserializationStrategy<WebSocketMessage>
) {
    CONNECTION_ACK("connection-ack", ConnectionAck.serializer()),
    MESSAGE("message", TextMessage.serializer()),
    REACTION_ADD("reaction-add", ReactionChange.serializer()),
    REACTION_REMOVE("reaction-remove", ReactionChange.serializer()),
    THREAD_ADD("thread-add", ThreadCreated.serializer()),
    ASSETS_SHARE("assets-share", ThreadAssets.serializer()),
    CONNECTION_REQUEST("connection-request", NewConnectionRequest.serializer())

}

@Serializable
private data class WebSocketDataSurrogate(
    val type: String,
    val content: String
)
