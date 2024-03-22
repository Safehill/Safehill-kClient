package com.safehill.mock

import com.safehill.kclient.models.SHServerUser
import com.safehill.kclient.network.ServerProxyInterface
import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO

class ServerProxySpy: ServerProxyInterface {

    var listTheadsCalled = 0
    var listThreadResponse: List<ConversationThreadOutputDTO> = emptyList()
    override suspend fun listThreads(): List<ConversationThreadOutputDTO> {
        listTheadsCalled++
        return listThreadResponse
    }

    var getUsersWithIdentifierCalled = 0
    var getUsersWithIdentifierParam: List<String>? = null
    var getUsersWithIdentifierResposne: List<SHServerUser> = emptyList()
    override suspend fun getUsers(userIdentifiersToFetch: List<String>): List<SHServerUser> {
        getUsersWithIdentifierCalled++
        getUsersWithIdentifierParam = userIdentifiersToFetch
        return getUsersWithIdentifierResposne
    }

    var getAllLocalUsersCalled = 0
    var getAllLocalUsersResponse: List<SHServerUser> = emptyList()
    override suspend fun getAllLocalUsers(): List<SHServerUser> {
        getAllLocalUsersCalled++
        return getAllLocalUsersResponse
    }

    fun reset() {
        listTheadsCalled = 0
        listThreadResponse = emptyList()
        getUsersWithIdentifierCalled = 0
        getUsersWithIdentifierParam = null
        getUsersWithIdentifierResposne = emptyList()
        getAllLocalUsersCalled = 0
        getAllLocalUsersResponse = emptyList()
    }
}
