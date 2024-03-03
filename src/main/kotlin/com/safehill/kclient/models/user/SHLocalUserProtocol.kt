package com.safehill.kclient.models.user

import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.network.SHServerProxyProtocol
import com.safehill.kcrypto.models.SHLocalCryptoUser

interface SHLocalUserProtocol : SHServerUser {
    val authToken: String?
    val publicSignatureData: ByteArray?
    val publicKeyData: ByteArray?
    val shUser: SHLocalCryptoUser
//    val maybeEncryptionProtocolSalt: ByteArray?
    val serverProxy: SHServerProxyProtocol
//
//    val keychainPrefix: String
//
//    fun authKeychainLabel(keychainPrefix: String): String
//    fun identityTokenKeychainLabel(keychainPrefix: String): String
//    fun authTokenKeychainLabel(keychainPrefix: String): String
//
//    @Throws(Exception::class)
//    fun deauthenticate(): SHLocalUser
//    @Throws(Exception::class)
//    fun destroy(): SHLocalUser
//
//    @Throws(Exception::class)
//    fun createShareablePayload(
//        data: ByteArray,
//        user: SHCryptoUser
//    ): SHShareablePayload
//
//    @Throws(Exception::class)
//    fun decrypt(
//        data: ByteArray,
//        encryptedSecret: SHShareablePayload,
//        user: SHCryptoUser
//    ): ByteArray
}
