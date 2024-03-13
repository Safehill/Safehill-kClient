package com.safehill.kclient.controllers

import com.safehill.kclient.models.SHServerUser
import com.safehill.mock.SHLocalUserSpy
import com.safehill.mock.ServerProxySpy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.PublicKey

class UsersControllerTest {

    private val serverProxySpy = ServerProxySpy()

    private val sut = UsersController(
        localUser = SHLocalUserSpy(),
        serverProxy = serverProxySpy
    )

    private val mockedResponse = listOf(object: SHServerUser {
        override val identifier: String
            get() = "mockedIdentifier"
        override val name: String
            get() = TODO("Not yet implemented")
        override val publicKey: PublicKey
            get() = TODO("Not yet implemented")
        override val publicSignature: PublicKey
            get() = TODO("Not yet implemented")
        override val publicKeyData: ByteArray
            get() = TODO("Not yet implemented")
        override val publicSignatureData: ByteArray
            get() = TODO("Not yet implemented")

    })

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
