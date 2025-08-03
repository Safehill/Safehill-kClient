package com.safehill.safehillclient.backgroundsync

import com.safehill.kclient.controllers.module.ControllersModule
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.network.remote.RemoteServerEnvironment
import com.safehill.kclient.tasks.inbound.LocalDownloadOperation
import com.safehill.kclient.tasks.inbound.RemoteDownloadOperation
import com.safehill.kclient.tasks.outbound.UploadOperation
import com.safehill.kclient.tasks.outbound.UploadOperationImpl
import com.safehill.kclient.tasks.syncing.InteractionSync
import com.safehill.safehillclient.module.asset.AssetModule
import com.safehill.safehillclient.module.config.ClientOptions
import com.safehill.safehillclient.module.platform.UserModule

class NetworkModule(
    val serverProxy: ServerProxy,
    val webSocketApi: WebSocketApi,
    val remoteServerEnvironment: RemoteServerEnvironment
)

class SafehillBackgroundTasksRegistryFactory(
    private val assetModule: AssetModule,
    private val userModule: UserModule,
    private val clientOptions: ClientOptions,
    private val networkModule: NetworkModule,
    private val userProvider: UserProvider,
    private val controllersModule: ControllersModule
) : BackgroundTasksRegistryFactory {


    private fun createRemoteDownloadOperation(): RemoteDownloadOperation {
        return RemoteDownloadOperation(
            serverProxy = networkModule.serverProxy,
            assetDescriptorsCache = assetModule.assetDescriptorCache
        )
    }

    private fun createLocalDownloadOperation(): LocalDownloadOperation {
        return LocalDownloadOperation(
            serverProxy = networkModule.serverProxy,
            assetDescriptorsCache = assetModule.assetDescriptorCache,
        )
    }

    private fun createInteractionSync(): InteractionSync {
        return InteractionSync(
            serverProxy = networkModule.serverProxy,
            webSocketApi = networkModule.webSocketApi
        )
    }

    private fun uploadOperation(): UploadOperation {
        return UploadOperationImpl(
            serverProxy = networkModule.serverProxy,
            encrypter = assetModule.assetEncrypter,
            userModule = userModule,
            userProvider = userProvider,
            userController = controllersModule.userController,
            localAssetsStoreController = controllersModule.localAssetsStoreController,
            clientScope = clientOptions.clientScope,
            safehillLogger = clientOptions.safehillLogger
        ).also {
            it.listeners.add(assetModule.assetsUploadPipelineStateHolder)
        }
    }

    override fun create(): BackgroundTasksRegistry {
        return BackgroundTasksRegistry(
            remoteDownloadOperation = createRemoteDownloadOperation(),
            interactionSync = createInteractionSync(),
            localDownloadOperation = createLocalDownloadOperation(),
            uploadOperation = uploadOperation()
        )
    }
}