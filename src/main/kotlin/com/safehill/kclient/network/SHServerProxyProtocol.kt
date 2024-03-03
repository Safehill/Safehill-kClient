package com.safehill.kclient.network

import com.safehill.kclient.api.SHHTTPAPI
import com.safehill.kclient.api.SHSafehillAPI
import com.safehill.kclient.models.user.SHLocalUserProtocol
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

interface SHServerProxyProtocol {
    suspend fun listThreads(): List<ConversationThreadOutputDTO>
}

class SHServerProxy(private val user: SHLocalUserProtocol) : SHServerProxyProtocol {

    private val localServer: LocalServer = LocalServer(requestor = user)
    private val remoteServer: SHSafehillAPI = SHHTTPAPI(requestor = user)

    init {
        // Inizializzazione del server proxy
    }

    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        try {
            val remoteResult = remoteServer.listThreads()
            return remoteResult
        } catch (error: Exception) {
            println("failed to fetch threads from server. Returning local version. ${error.localizedMessage}")
            return localServer.listThreads()
        }
    }

}
