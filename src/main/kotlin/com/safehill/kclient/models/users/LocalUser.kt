package com.safehill.kclient.models.users

import com.safehill.kclient.models.CryptoUser
import com.safehill.kclient.models.LocalCryptoUser
import com.safehill.kclient.models.SHUserContext
import com.safehill.kclient.models.dtos.AuthResponseDTO
import com.safehill.kclient.models.dtos.BearerToken
import com.safehill.kcrypto.models.ShareablePayload
import java.security.PublicKey
import java.util.Base64

class LocalUser(
    val shUser: LocalCryptoUser,
    override val name: String
) : ServerUser {

    override val identifier: UserIdentifier
        get() = this.shUser.identifier

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

    fun authenticate(authResponseDTO: AuthResponseDTO) {
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
        return SHUserContext(this.shUser).shareable(data, with, protocolSalt)
    }

    fun decrypted(
        data: ByteArray,
        encryptedSecret: ShareablePayload,
        protocolSalt: ByteArray,
        receivedFrom: CryptoUser
    ): ByteArray {
        return SHUserContext(this.shUser).decrypt(
            data,
            encryptedSecret,
            protocolSalt,
            receivedFrom
        )
    }
    
}
