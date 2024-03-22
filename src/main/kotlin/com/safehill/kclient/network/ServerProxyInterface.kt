package com.safehill.kclient.network

import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

interface ServerProxyInterface {
    suspend fun listThreads(): List<ConversationThreadOutputDTO>
    suspend fun getUsers(userIdentifiersToFetch: List<String>): List<SHServerUser>
    suspend fun getAllLocalUsers(): List<SHServerUser>
}

