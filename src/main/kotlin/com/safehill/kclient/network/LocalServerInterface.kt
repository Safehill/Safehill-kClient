package com.safehill.kclient.network

import com.safehill.kclient.api.SafehillApi
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

interface LocalServerInterface : SafehillApi {
    suspend fun createOrUpdateUser(
        identifier: String,
        name: String,
        publicKeyData: ByteArray,
        publicSignatureData: ByteArray
    )

    suspend fun insertThreads(threads: List<ConversationThreadOutputDTO>)
}
