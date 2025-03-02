package com.safehill.kclient.controllers

import com.safehill.kclient.models.SymmetricKey
import com.safehill.kclient.models.dtos.RecipientEncryptionDetailsDTO
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.network.ServerProxy
import java.util.Base64

class EncryptionDetailsController(
    private val userProvider: UserProvider,
    val serverProxy: ServerProxy
) {
    fun getRecipientEncryptionDetails(
        users: List<ServerUser>,
        secretKey: SymmetricKey
    ): List<RecipientEncryptionDetailsDTO> {
        return users.map { user ->
            val currentUser = userProvider.get()
            val shareable = currentUser.shareable(
                data = secretKey.secretKeySpec.encoded,
                with = user,
                protocolSalt = currentUser.encryptionSalt
            )

            RecipientEncryptionDetailsDTO(
                recipientUserIdentifier = user.identifier,
                ephemeralPublicKey = Base64.getEncoder()
                    .encodeToString(shareable.ephemeralPublicKeyData),
                encryptedSecret = Base64.getEncoder().encodeToString(shareable.ciphertext),
                secretPublicSignature = Base64.getEncoder().encodeToString(shareable.signature),
                senderPublicSignature = Base64.getEncoder()
                    .encodeToString(currentUser.publicSignatureData)
            )
        }
    }
}