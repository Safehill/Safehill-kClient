package com.safehill.kclient.network

import com.safehill.kclient.api.SafehillApi
import com.safehill.kclient.api.SafehillApiImpl
import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.models.user.SHLocalUserInterface
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

class ServerProxy(
    user: SHLocalUserInterface,
    var localServer: LocalServerInterface
) : ServerProxyInterface {

    var remoteServer: SafehillApi = SafehillApiImpl(requestor = user)

    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        return try {
            val remoteResult = remoteServer.listThreads()
            remoteResult
        } catch (error: Exception) {
            println("failed to fetch threads from server. Returning local version. ${error.localizedMessage}")
            localServer.listThreads()
        }
    }

    override suspend fun getUsers(userIdentifiersToFetch: List<String>): List<SHServerUser> {
        if (userIdentifiersToFetch.isEmpty()) {
            return emptyList()
        }

        return try {
            val remoteUsers = remoteServer.getUsers(userIdentifiersToFetch)
            val updateResult = updateLocalUserDB(serverUsers = remoteUsers)
            updateResult
        } catch (exception: Exception) {
            val localUsers = localServer.getUsers(userIdentifiersToFetch)
            if (localUsers.size == userIdentifiersToFetch.size) {
                localUsers
            } else {
                throw exception
            }
        }
    }

    private suspend fun updateLocalUserDB(serverUsers: List<SHServerUser>): List<SHServerUser> {
        serverUsers.forEach { serverUser ->
            try {
                localServer.createOrUpdateUser(
                    identifier = serverUser.identifier,
                    name = serverUser.name,
                    publicKeyData = serverUser.publicKeyData,
                    publicSignatureData = serverUser.publicSignatureData
                )
            } catch (exception: Exception) {
                println("failed to create server user in local server: ${exception.message}")
            }
        }

        return localServer.getUsers(serverUsers.map { it.identifier })
    }

    override suspend fun getAllLocalUsers(): List<SHServerUser> {
        TODO("Not yet implemented")
    }

}
