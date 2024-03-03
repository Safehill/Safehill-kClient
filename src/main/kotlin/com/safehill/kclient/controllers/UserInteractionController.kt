package com.safehill.kclient.controllers

import com.safehill.kclient.api.SHSafehillAPI
import com.safehill.kclient.models.user.SHLocalUserProtocol
import com.safehill.kclient.network.SHServerProxyProtocol
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

class SHUserInteractionController(
    private val user: SHLocalUserProtocol,
    private var serverProxy: SHServerProxyProtocol = user.serverProxy
) {
    @Throws
    suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return serverProxy.listThreads()
    }
}

