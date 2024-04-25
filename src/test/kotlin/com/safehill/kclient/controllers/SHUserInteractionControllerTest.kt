package com.safehill.kclient.controllers

class SHUserInteractionControllerTest {

    // todo fix tests for implementation change
//    private val serverProxySpy = ServerProxySpy()
//
//    private val sut = SHUserInteractionController(
//        serverProxy = serverProxySpy
//    )
//
//    @BeforeEach
//    fun setup() {
//        serverProxySpy.reset()
//    }
//
//    @Test
//    fun listThreads() = runBlocking {
//        val mockedResponse = listOf(
//            ConversationThreadOutputDTO(
//                threadId = "mockedThreadId",
//                membersPublicIdentifier = emptyList(),
//                name = null,
//                lastUpdatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
//                encryptionDetails = RecipientEncryptionDetailsDTO("", "", "", "", ""),
//                creatorPublicIdentifier = UUID.randomUUID().toString(),
//                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
//            )
//        )
//        serverProxySpy.listThreadResponse = mockedResponse
//        val result = sut.listThreads()
//        assert(serverProxySpy.listTheadsCalled == 1)
//        assert(mockedResponse.first().threadId == result.firstOrNull()?.threadId)
//    }
}
