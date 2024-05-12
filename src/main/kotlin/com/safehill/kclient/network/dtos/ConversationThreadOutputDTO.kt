package com.safehill.kclient.network.dtos

import com.safehill.kclient.models.users.LocalUser
import com.safehill.kcrypto.SafehillCypher
import com.safehill.kcrypto.base64.decodeBase64
import com.safehill.kcrypto.models.SafehillPublicKey
import com.safehill.kcrypto.models.SymmetricKey
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
    fun getSymmetricKey(currentUser: LocalUser) = SymmetricKey(
        SafehillCypher.decrypt(
            sealedMessage = this.encryptionDetails.toShareablePayload(),
            encryptionKey = currentUser.shUser.key,
            protocolSalt = currentUser.encryptionSalt,
            signedBy = SafehillPublicKey.from(
                this.encryptionDetails.senderPublicSignature.toByteArray().decodeBase64()
            )
        )
    )
}
