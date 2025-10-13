package com.safehill.safehillclient.data.message.model

import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.base64.decodeBase64
import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.dtos.MessageOutputDTO
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.tasks.outbound.UploadFailure
import java.time.Instant

data class Message(
    val id: String,
    val senderIdentifier: UserIdentifier,
    val userIdentifier: UserIdentifier,
    val createdDate: Instant,
    val status: MessageStatus,
    val messageType: MessageType
) {
    val isOwnMessage: Boolean = senderIdentifier == userIdentifier

    constructor(
        id: String,
        userIdentifier: UserIdentifier,
        senderIdentifier: UserIdentifier,
        createdDate: Instant,
        status: MessageStatus,
        text: String
    ) : this(
        id = id,
        userIdentifier = userIdentifier,
        senderIdentifier = senderIdentifier,
        createdDate = createdDate,
        status = status,
        messageType = MessageType.Text(text)
    )
}


sealed class MessageType {
    data class Text(val message: String) : MessageType()

    data class Images(
        val groupId: GroupId,
        val messageImageStates: List<MessageImageState>
    ) : MessageType() {

        val completedImages =
            messageImageStates.filterIsInstance<MessageImageState.Completed>()

        val failedImages =
            messageImageStates.filterIsInstance<MessageImageState.Failed>()

        val uploadingImages =
            messageImageStates.filterIsInstance<MessageImageState.Uploading>()

        val assetIdentifiers: List<AssetGlobalIdentifier>
            get() = messageImageStates.map { it.globalIdentifier }

        fun updateImageStatus(
            newImageState: MessageImageState
        ): Images {
            val updatedStates = messageImageStates
                .updateOrAdd(newItem = newImageState) {
                    it.globalIdentifier == newImageState.globalIdentifier
                }.distinctBy { it.globalIdentifier }
            return copy(messageImageStates = updatedStates)
        }

    }
}

fun <T> List<T>.updateOrAdd(newItem: T, predicate: (T) -> Boolean): List<T> {
    val index = indexOfFirst(predicate)
    return if (index == -1) {
        listOf(newItem) + this
    } else {
        mapIndexed { i, item -> if (i == index) newItem else item }
    }
}

sealed class MessageImageState {
    abstract val globalIdentifier: AssetGlobalIdentifier

    data class Uploading(
        val localIdentifier: AssetLocalIdentifier,
        override val globalIdentifier: AssetGlobalIdentifier
    ) : MessageImageState()

    data class Completed(
        override val globalIdentifier: AssetGlobalIdentifier
    ) : MessageImageState()

    data class Failed(
        val localIdentifier: AssetLocalIdentifier,
        val error: UploadFailure,
        val groupId: GroupId,
        override val globalIdentifier: AssetGlobalIdentifier
    ) : MessageImageState()
}

sealed class MessageStatus {
    data object Sending : MessageStatus()
    data object Sent : MessageStatus()
    data class Error(val errorMsg: String) : MessageStatus()

}

fun MessageOutputDTO.toMessage(
    decryptedMessage: String,
    userIdentifier: UserIdentifier,
    status: MessageStatus
): Message {
    return Message(
        id = this.interactionId,
        messageType = MessageType.Text(decryptedMessage),
        userIdentifier = userIdentifier,
        senderIdentifier = this.senderUserIdentifier,
        createdDate = this.createdAt,
        status = status
    )
}

fun MessageOutputDTO.toMessage(
    key: SymmetricKey,
    currentUser: LocalUser,
    status: MessageStatus
): Message {
    val decryptedMessage = SafehillCypher.decrypt(
        cipherText = this.encryptedMessage.toByteArray().decodeBase64(),
        key = key
    )
    return toMessage(
        decryptedMessage = String(decryptedMessage),
        userIdentifier = currentUser.identifier,
        status = status
    )
}