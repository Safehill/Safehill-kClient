package com.safehill.kclient.controllers

import com.safehill.kclient.models.SHRemoteUser
import com.safehill.mock.ServerProxySpy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UsersControllerTest {

    private val serverProxySpy = ServerProxySpy()

    private val sut = UsersController(
        serverProxy = serverProxySpy
    )

    private val mockedResponse = listOf(
        SHRemoteUser(
            identifier = "mockedIdentifier",
            name = TODO("Not yet implemented"),
            publicSignatureData = TODO("Not yet implemented"),
            publicKeyData = TODO("Not yet implemented")
        )
    )

    @BeforeEach
    fun setup() {
        serverProxySpy.reset()
    }

    @Test
    fun getUsers() = runBlocking {
        val mockedParam = listOf("id1", "id2", "id3")
        serverProxySpy.getUsersWithIdentifierResposne = mockedResponse
        val result = sut.getUsers(mockedParam)
        assert(serverProxySpy.getUsersWithIdentifierCalled == 1)
        assert(serverProxySpy.getUsersWithIdentifierParam == mockedParam)
        assert(mockedResponse.first().identifier == result.firstOrNull()?.identifier)
    }

    @Test
    fun getAllLocalUsers() = runBlocking {
        serverProxySpy.getAllLocalUsersResponse = mockedResponse
        val result = sut.getAllLocalUsers()
        assert(serverProxySpy.getAllLocalUsersCalled == 1)
        assert(mockedResponse.first().identifier == result.firstOrNull()?.identifier)
    }
}
