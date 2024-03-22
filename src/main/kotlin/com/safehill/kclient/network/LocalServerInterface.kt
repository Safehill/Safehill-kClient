package com.safehill.kclient.network

import com.safehill.kclient.api.SafehillApi

interface LocalServerInterface: SafehillApi {
    suspend fun createOrUpdateUser(
        identifier: String,
        name: String,
        publicKeyData: ByteArray,
        publicSignatureData: ByteArray
    )
}
