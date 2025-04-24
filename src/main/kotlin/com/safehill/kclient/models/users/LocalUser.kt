package com.safehill.kclient.models.users

import com.safehill.kclient.SafehillCypher
import com.safehill.kclient.models.CryptoUser
import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.SafehillKeyPair
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.BearerToken
import com.safehill.kcrypto.models.ShareablePayload
import java.security.PublicKey
import java.util.Base64

class LocalUser(
    var shUser: LocalCryptoUser
) : ServerUser {

    override val identifier: UserIdentifier
        get() = this.shUser.identifier

    override var name: String = ""

    override val publicKey: PublicKey
        get() = this.shUser.publicKey

    override val publicSignature: PublicKey
        get() = this.shUser.publicSignature

    override val publicKeyData: ByteArray
        get() = this.shUser.publicKeyData

    override val publicSignatureData: ByteArray
        get() = this.shUser.publicSignatureData

    var authToken: BearerToken? = null
    var encryptionSalt: ByteArray = byteArrayOf()

    private fun updateUserDetails(given: ServerUser?) {
        given?.let {
            this.name = it.name
        } ?: run {
            this.name = ""
        }
    }

    fun authenticate(user: ServerUser, authResponseDTO: AuthResponseDTO) {
        this.updateUserDetails(user)
        this.authToken = authResponseDTO.bearerToken
        this.encryptionSalt = Base64.getDecoder().decode(authResponseDTO.encryptionProtocolSalt)
    }

    fun deauthenticate() {
        this.authToken = null
    }

    fun shareable(
        data: ByteArray,
        with: CryptoUser,
        protocolSalt: ByteArray
    ): ShareablePayload {
        val ephemeralKey = SafehillKeyPair.generate()
        val encrypted = SafehillCypher.encrypt(
            message = data,
            receiverPublicKey = with.publicKey,
            ephemeralKey = ephemeralKey,
            protocolSalt = protocolSalt,
            senderSignatureKey = shUser.signature
        )
        return ShareablePayload(
            ephemeralKey.public.encoded,
            encrypted.ciphertext,
            encrypted.signature,
            with
        )
    }

    fun decrypted(
        data: ByteArray,
        encryptedSecret: ShareablePayload,
        protocolSalt: ByteArray,
        sender: CryptoUser
    ): ByteArray {
        val secretData = decryptSecret(
            sealedMessage = encryptedSecret,
            protocolSalt = protocolSalt,
            sender = sender
        )
        return SafehillCypher.decrypt(data, secretData)
    }

    fun decryptSecret(
        sealedMessage: ShareablePayload,
        protocolSalt: ByteArray,
        sender: CryptoUser
    ): ByteArray {
        return SafehillCypher.decrypt(
            sealedMessage = sealedMessage,
            encryptionKey = shUser.key,
            protocolSalt = protocolSalt,
            signedBy = sender.publicSignature
        )
    }

    fun regenerateKeys() {
        this.deauthenticate()
        this.shUser = LocalCryptoUser()
    }
}
