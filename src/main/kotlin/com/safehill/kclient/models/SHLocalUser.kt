package com.safehill.kclient.models

import com.safehill.kcrypto.models.SHCryptoUser
import com.safehill.kcrypto.models.SHLocalCryptoUser
import com.safehill.kcrypto.models.SHShareablePayload
import com.safehill.kcrypto.models.SHUserContext
import java.security.PublicKey

class SHLocalUser(
    var shUser: SHLocalCryptoUser
) : SHServerUser {
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

    private fun updateUserDetails(given: SHServerUser?) {
        given?.let {
            this.name = it.name
        } ?: run {
            this.name = ""
        }
    }

    fun authenticate(user: SHServerUser, bearerToken: String) {
        this.updateUserDetails(user)
        this.authToken = bearerToken
    }

    fun deauthenticate() {
        this.authToken = null
    }

    fun shareable(
        data: ByteArray,
        with: SHCryptoUser,
        protocolSalt: ByteArray,
        iv: ByteArray
    ): SHShareablePayload {
        return SHUserContext(this.shUser).shareable(data, with, protocolSalt, iv)
    }

    fun decrypted(
        data: ByteArray,
        encryptedSecret: SHShareablePayload,
        protocolSalt: ByteArray,
        iv: ByteArray,
        receivedFrom: SHCryptoUser
    ): ByteArray {
        return SHUserContext(this.shUser).decrypt(
            data,
            encryptedSecret,
            protocolSalt,
            iv,
            receivedFrom
        )
    }

    fun regenerateKeys() {
        this.deauthenticate()
        this.shUser = SHLocalCryptoUser()
    }
}