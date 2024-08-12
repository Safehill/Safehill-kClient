package com.safehill.kclient.models.dtos

import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.base64.decodeBase64
import com.safehill.kclient.models.SafehillPublicKey
import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.serde.InstantSerializer
import com.safehill.kclient.models.users.LocalUser
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ConversationThreadOutputDTO(
    val threadId: String,
    val name: String?,
    val membersPublicIdentifier: List<String>,
    @Serializable(with = InstantSerializer::class) val lastUpdatedAt: Instant,
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
