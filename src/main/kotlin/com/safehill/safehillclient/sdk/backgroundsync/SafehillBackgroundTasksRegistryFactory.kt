package com.safehill.safehillclient.sdk.backgroundsync

import com.safehill.kclient.logging.DefaultSafehillLogger
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.network.remote.RemoteServerEnvironment
import com.safehill.kclient.tasks.inbound.LocalDownloadOperation
import com.safehill.kclient.tasks.inbound.RemoteDownloadOperation
import com.safehill.kclient.tasks.outbound.UploadOperation
import com.safehill.kclient.tasks.outbound.UploadOperationImpl
import com.safehill.kclient.tasks.syncing.InteractionSync
import com.safehill.safehillclient.sdk.ClientScope
import com.safehill.safehillclient.sdk.module.asset.AssetModule
import com.safehill.safehillclient.sdk.platform.UserModule
import com.safehill.safehillclient.sdk.utils.api.dispatchers.SdkDispatchers
import com.safehill.safehillclient.sdk.utils.extensions.createChildScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ClientOptions(
    val safehillLogger: SafehillLogger = DefaultSafehillLogger(),
    val sdkDispatchers: SdkDispatchers = SdkDispatchers(
        io = Dispatchers.IO,
        default = Dispatchers.Default
    ),
    val clientScope: ClientScope = CoroutineScope(
        SupervisorJob() + sdkDispatchers.io + CoroutineExceptionHandler { coroutineContext, throwable ->
            safehillLogger.error("Exception in coroutine scope: coroutineContext=$coroutineContext, throwable=$throwable")
        }
    ),
    val userScope: CoroutineScope = clientScope.createChildScope { SupervisorJob(it) }
)

class NetworkModule(
    val serverProxy: ServerProxy,
    val webSocketApi: WebSocketApi,
    val remoteServerEnvironment: RemoteServerEnvironment
)

class SafehillBackgroundTasksRegistryFactory(
    private val assetModule: AssetModule,
    private val userModule: UserModule,
    private val networkModule: NetworkModule,
    private val userProvider: UserProvider
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
            listeners = mutableListOf(assetModule.assetsUploadPipelineStateHolder),
            encrypter = assetModule.assetEncrypter,
            userModule = userModule,
            userProvider = userProvider
        )
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