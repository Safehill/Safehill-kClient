package com.safehill.kclient.models.users

import com.safehill.kclient.api.dtos.AuthResponseDTO
import com.safehill.kcrypto.models.SHCryptoUser
import com.safehill.kcrypto.models.SHLocalCryptoUser
import com.safehill.kcrypto.models.SHShareablePayload
import com.safehill.kcrypto.models.SHUserContext
import java.security.PublicKey
import java.util.Base64

class LocalUser(
    var shUser: SHLocalCryptoUser,
) : ServerUser {

    override val identifier: String
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

    var authToken: String? = null
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
        with: SHCryptoUser,
        protocolSalt: ByteArray
    ): SHShareablePayload {
        return SHUserContext(this.shUser).shareable(data, with, protocolSalt)
    }

    fun decrypted(
        data: ByteArray,
        encryptedSecret: SHShareablePayload,
        protocolSalt: ByteArray,
        receivedFrom: SHCryptoUser
    ): ByteArray {
        return SHUserContext(this.shUser).decrypt(
            data,
            encryptedSecret,
            protocolSalt,
            receivedFrom
        )
    }

    fun regenerateKeys() {
        this.deauthenticate()
        this.shUser = SHLocalCryptoUser()
    }
}