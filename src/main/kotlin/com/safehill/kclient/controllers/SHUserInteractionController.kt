package com.safehill.kclient.controllers

import com.safehill.kclient.network.ServerProxyInterface
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

class SHUserInteractionController(
    private var serverProxy: ServerProxyInterface
) {
    @Throws
    suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return serverProxy.listThreads()
    }
}
