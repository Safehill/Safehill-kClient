package com.safehill.kclient.network.api.web_client

interface WebClientApi {

    suspend fun sendEncryptedKeysToWebClient(
        sessionId: String,
        requestorIp: String,
        encryptedPrivateKeyData: ByteArray,
        encryptedPrivateKeyIvData: ByteArray,
        encryptedPrivateSignatureData: ByteArray,
        encryptedPrivateSignatureIvData: ByteArray
    )
}