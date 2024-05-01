package com.safehill.kclient.models.user

import com.safehill.kclient.models.SHServerUser
import com.safehill.kcrypto.models.SHLocalCryptoUser

interface SHLocalUserInterface : SHServerUser {
    val authToken: String?
    override val publicSignatureData: ByteArray
    override val publicKeyData: ByteArray
    val shUser: SHLocalCryptoUser
    val maybeEncryptionProtocolSalt: ByteArray?
    val keychainPrefix: String
}
