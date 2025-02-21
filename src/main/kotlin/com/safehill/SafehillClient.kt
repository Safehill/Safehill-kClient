package com.safehill

import com.safehill.kclient.controllers.ConversationThreadController
import com.safehill.kclient.controllers.EncryptionDetailsController
import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.controllers.UserInteractionController
import com.safehill.kclient.logging.DefaultSafehillLogger
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.ServerProxyImpl
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.network.local.LocalServerInterface
import com.safehill.kclient.network.remote.RemoteServer
import io.ktor.client.HttpClient
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class SafehillClient private constructor(
    val serverProxy: ServerProxy,
    val webSocketApi: WebSocketApi,
    val currentUser: LocalUser
) {
    val encryptionDetailsController by lazy {
        EncryptionDetailsController(
            currentUser = currentUser,
            serverProxy = serverProxy
        )
    }

    val interactionController by lazy {
        UserInteractionController(
            serverProxy = serverProxy,
            currentUser = currentUser,
            encryptionDetailsController = encryptionDetailsController
        )
    }

    val conversationThreadController by lazy {
        ConversationThreadController(
            serverProxy = serverProxy,
            userInteractionController = interactionController,
            encryptionDetailsController = encryptionDetailsController
        )
    }

    val userController by lazy {
        UserController(
            serverProxy = serverProxy
        )
    }

    val assetDescriptorCache by lazy {
        AssetDescriptorsCache(
            currentUser = currentUser
        )
    }

    suspend fun connectToSocket(deviceId: String) {
        webSocketApi.connectToSocket(deviceId = deviceId, currentUser = currentUser)
    }

    class Builder(
        private val localServer: LocalServerInterface,
        private val currentUser: LocalUser,
        private val webSocketApi: WebSocketApi,
        private val httpClient: HttpClient,
        private val safehillLogger: SafehillLogger = DefaultSafehillLogger()
    ) {

        private fun setupBouncyCastle() {
            // Android registers its own BC provider. As it might be outdated and might not include
            // all needed ciphers, we substitute it with a known BC bundled in the app.
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            }
            Security.addProvider(BouncyCastleProvider())
        }

        fun build(): SafehillClient {
            setupBouncyCastle()
            logger = safehillLogger
            return SafehillClient(
                serverProxy = ServerProxyImpl(
                    localServer = localServer,
                    remoteServer = RemoteServer(
                        requestor = currentUser,
                        client = httpClient
                    ),
                    requestor = currentUser
                ),
                webSocketApi = webSocketApi,
                currentUser = currentUser
            )
        }
    }

    companion object {

        // Do we want a singleton logger or each safehill client should be responsible for its own logger?
        var logger: SafehillLogger = DefaultSafehillLogger()
            private set
    }
}