package com.safehill.kclient.models

import com.safehill.kclient.models.user.SHLocalUserProtocol
import com.safehill.kclient.network.SHServerProxy
import com.safehill.kclient.network.SHServerProxyProtocol
import com.safehill.kcrypto.models.SHCryptoUser
import com.safehill.kcrypto.models.SHLocalCryptoUser
import com.safehill.kcrypto.models.SHShareablePayload
import com.safehill.kcrypto.models.SHUserContext
import org.jetbrains.annotations.TestOnly

class SHLocalUser(
    override var shUser: SHLocalCryptoUser
) : SHServerUser, SHLocalUserProtocol {
    override val serverProxy: SHServerProxyProtocol = SHServerProxy(user = this)
    override val identifier: String
        get() = this.shUser.identifier
    override var name: String = ""

    override val publicKeyData: ByteArray
        get() = this.shUser.publicKeyData

    override val publicSignatureData: ByteArray
        get() = this.shUser.publicSignatureData

    override var authToken: String? = null

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