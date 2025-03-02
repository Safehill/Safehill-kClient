package com.safehill.kclient.models.dtos

import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.base64.decodeBase64
import com.safehill.kclient.models.SafehillPublicKey
import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kcrypto.models.ShareablePayload
import kotlinx.serialization.Serializable

@Serializable
data class RecipientEncryptionDetailsDTO(
    val recipientUserIdentifier: String,
    val ephemeralPublicKey: String, // base64EncodedData with the ephemeral public part of the key used for the encryption
    val encryptedSecret: String, // base64EncodedData with the secret to decrypt the encrypted content in this group for this user
    val secretPublicSignature: String, // base64EncodedData with the public signature used for the encryption of the secret
    val senderPublicSignature: String // base64EncodedData with the public signature of the user sending it
) {
    private fun toShareablePayload() = ShareablePayload(
        ephemeralPublicKeyData = this.ephemeralPublicKey.toByteArray().decodeBase64(),
        ciphertext = this.encryptedSecret.toByteArray().decodeBase64(),
        signature = this.secretPublicSignature.toByteArray().decodeBase64(),
        recipient = null,
    )

    fun getSymmetricKey(currentUser: LocalUser) = try {
        SymmetricKey(
            SafehillCypher.decrypt(
                sealedMessage = this.toShareablePayload(),
                encryptionKey = currentUser.shUser.key,
                protocolSalt = currentUser.encryptionSalt,
                signedBy = SafehillPublicKey.from(
                    this.senderPublicSignature.toByteArray().decodeBase64()
                )
            )
        )
    } catch (e: Exception) {
        null
    }

}
