package com.safehill.kclient.controllers

import com.safehill.kclient.models.user.SHLocalUserInterface
import com.safehill.kclient.network.ServerProxyInterface
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

class SHUserInteractionController(
    private val user: SHLocalUserInterface,
    private var serverProxy: ServerProxyInterface = user.serverProxy
) {
    @Throws
    suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return serverProxy.listThreads()
    }
}
