package com.safehill.kclient.network.api.web_client

import com.safehill.kclient.network.api.BaseApi
import com.safehill.kclient.network.api.postRequest
import io.ktor.util.encodeBase64

class WebClientApiImpl(baseApi: BaseApi) : WebClientApi, BaseApi by baseApi {
    override suspend fun sendEncryptedKeysToWebClient(
        sessionId: String,
        requestorIp: String,
        encryptedPrivateKeyData: ByteArray,
        encryptedPrivateKeyIvData: ByteArray,
        encryptedPrivateSignatureData: ByteArray,
        encryptedPrivateSignatureIvData: ByteArray
    ) {
        postRequest(
            "app-web-auth/$sessionId/send-keys",
            request = mapOf(
                "requestorIp" to requestorIp,
                "privateKey" to encryptedPrivateKeyData.encodeBase64(),
                "privateKeyIV" to encryptedPrivateKeyIvData.encodeBase64(),
                "privateSignature" to encryptedPrivateSignatureData.encodeBase64(),
                "privateSignatureIV" to encryptedPrivateSignatureIvData.encodeBase64(),
            )
        )
    }
}