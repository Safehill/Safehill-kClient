package com.safehill

//
//class SafehillClient(
//    val serverProxy: ServerProxy,
//    val webSocketApi: WebSocketApi,
//    val userProvider: UserProvider
//) {
//
//    init {
//        setupBouncyCastle()
//    }
//
//    val encryptionDetailsController by lazy {
//        EncryptionDetailsController(
//            userProvider = userProvider,
//            serverProxy = serverProxy
//        )
//    }
//
//    val interactionController by lazy {
//        UserInteractionController(
//            serverProxy = serverProxy,
//            userProvider = userProvider,
//            encryptionDetailsController = encryptionDetailsController
//        )
//    }
//
//    val conversationThreadController by lazy {
//        ConversationThreadController(
//            serverProxy = serverProxy,
//            userInteractionController = interactionController,
//            encryptionDetailsController = encryptionDetailsController
//        )
//    }
//
//    val userController by lazy {
//        UserController(
//            serverProxy = serverProxy
//        )
//    }
//
//    val assetDescriptorCache by lazy {
//        AssetDescriptorsCache(
//            userProvider = userProvider
//        )
//    }
//
//    suspend fun connectToSocket(deviceId: String, currentUser: LocalUser) {
//        webSocketApi.connectToSocket(deviceId = deviceId, currentUser = currentUser)
//    }
//
//}