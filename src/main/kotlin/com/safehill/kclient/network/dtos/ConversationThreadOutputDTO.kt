package com.safehill.kclient.network.dtos

import com.safehill.kclient.models.users.LocalUser
import com.safehill.kcrypto.SHCypher
import com.safehill.kcrypto.base64.decodeBase64
import com.safehill.kcrypto.models.SHPublicKey
import com.safehill.kcrypto.models.SHSymmetricKey
import kotlinx.serialization.Serializable

@Serializable
data class ConversationThreadOutputDTO(
    val threadId: String,
    val name: String?,
    val membersPublicIdentifier: List<String>,
    val lastUpdatedAt: String,
    val creatorPublicIdentifier: String,
    val createdAt: String,
    val encryptionDetails: RecipientEncryptionDetailsDTO // for the user making the request
) {
    fun getSymmetricKey(currentUser: LocalUser) = SHSymmetricKey(
        SHCypher.decrypt(
            sealedMessage = this.encryptionDetails.toShareablePayload(),
            encryptionKey = currentUser.shUser.key,
            protocolSalt = currentUser.encryptionSalt,
            signedBy = SHPublicKey.from(
                this.encryptionDetails.senderPublicSignature.toByteArray().decodeBase64()
            )
        )
    )
}
