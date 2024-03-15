package com.safehill.kclient.controllers

import com.safehill.kclient.network.dtos.ConversationThreadOutputDTO
import com.safehill.kclient.network.dtos.RecipientEncryptionDetailsDTO
import com.safehill.mock.ServerProxySpy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Test

class SHUserInteractionControllerTest {

    private val serverProxySpy = ServerProxySpy()

    private val sut = SHUserInteractionController(
        serverProxy = serverProxySpy
    )

    @BeforeEach
    fun setup() {
        serverProxySpy.reset()
    }

    @Test
    fun listThreads() = runBlocking {
        val mockedResponse = listOf(
            ConversationThreadOutputDTO(
                threadId = "mockedThreadId",
                membersPublicIdentifier = emptyList(),
                name = null,
                lastUpdatedAt = null,
                encryptionDetails = RecipientEncryptionDetailsDTO("", "", "", "", "")
            )
        )
        serverProxySpy.listThreadResponse = mockedResponse
        val result = sut.listThreads()
        assert(serverProxySpy.listTheadsCalled == 1)
        assert(mockedResponse.first().threadId == result.firstOrNull()?.threadId)
    }
}
