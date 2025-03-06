package com.safehill.kclient.controllers.module

import com.safehill.kclient.controllers.ConversationThreadController
import com.safehill.kclient.controllers.EncryptionDetailsController
import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.controllers.UserInteractionController
import com.safehill.kclient.models.users.UserProvider
import com.safehill.safehillclient.backgroundsync.NetworkModule
import com.safehill.safehillclient.module.asset.AssetModule

class ControllersModule(
    private val userProvider: UserProvider,
    private val networkModule: NetworkModule,
    private val assetModule: AssetModule
) {

    val encryptionDetailsController by lazy {
        EncryptionDetailsController(
            userProvider = userProvider,
            serverProxy = networkModule.serverProxy
        )
    }

    val interactionController by lazy {
        UserInteractionController(
            serverProxy = networkModule.serverProxy,
            userProvider = userProvider,
            encryptionDetailsController = encryptionDetailsController
        )
    }

    val conversationThreadController by lazy {
        ConversationThreadController(
            serverProxy = networkModule.serverProxy,
            userInteractionController = interactionController,
            encryptionDetailsController = encryptionDetailsController
        )
    }

    val userController by lazy {
        UserController(
            serverProxy = networkModule.serverProxy
        )
    }

    val localAssetsStoreController by lazy {
        LocalAssetsStoreController(
            serverProxy = networkModule.serverProxy,
            userController = userController,
            assetDescriptorsCache = assetModule.assetDescriptorCache,
            userProvider = userProvider
        )
    }
}