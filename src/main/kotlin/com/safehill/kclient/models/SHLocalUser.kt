package com.safehill.kclient.models

import com.safehill.kcrypto.models.SHCryptoUser
import com.safehill.kcrypto.models.SHLocalCryptoUser
import com.safehill.kcrypto.models.SHShareablePayload
import com.safehill.kcrypto.models.SHUserContext

class SHLocalUser(
    var shUser: SHLocalCryptoUser
) : SHServerUser {
    override val identifier: String
        get() = this.shUser.identifier
    override var name: String = ""

    val publicKeyData: ByteArray
        get() = this.shUser.publicKeyData

    val publicSignatureData: ByteArray
        get() = this.shUser.publicSignatureData

    var authToken: String? = null

    private fun updateUserDetails(given: SHServerUser?) {
        val user = given?.let {
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

    fun shareable(data: ByteArray, with: SHCryptoUser, protocolSalt: ByteArray, iv: ByteArray): SHShareablePayload {
        return SHUserContext(this.shUser).shareable(data, with, protocolSalt, iv)
    }

    fun decrypted(data: ByteArray, encryptedSecret: SHShareablePayload, protocolSalt: ByteArray, iv: ByteArray, receivedFrom: SHCryptoUser): ByteArray {
        return SHUserContext(this.shUser).decrypt(data, encryptedSecret, protocolSalt, iv, receivedFrom)
    }

    fun regenerateKeys() {
        this.deauthenticate()
        this.shUser = SHLocalCryptoUser()
    }
}