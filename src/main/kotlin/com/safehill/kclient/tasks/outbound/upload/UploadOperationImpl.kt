package com.safehill.kclient.tasks.outbound.upload

import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.models.users.getOrNull
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.tasks.outbound.AssetEncrypter
import com.safehill.kclient.tasks.outbound.OutboundQueueItemManagerInterface
import com.safehill.kclient.tasks.outbound.UploadListenersRegistry
import com.safehill.kclient.tasks.outbound.UploadOperationListener
import com.safehill.kclient.tasks.outbound.model.UploadRequest
import com.safehill.kclient.tasks.outbound.sharing.AssetSharingProcessor
import com.safehill.kclient.tasks.outbound.sharing.DefaultSharingExecutor
import com.safehill.kclient.tasks.outbound.sharing.SharingStates
import com.safehill.kclient.tasks.upload.DefaultRetryManager
import com.safehill.kclient.tasks.upload.ExponentialBackoffRetryPolicy
import com.safehill.kclient.tasks.upload.RobustUploadManager
import com.safehill.kclient.tasks.upload.queue.ChannelQueue
import com.safehill.safehillclient.ClientScope
import com.safehill.safehillclient.module.platform.UserModule
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID

class UploadOperationImpl(
    private val serverProxy: ServerProxy,
    private val encrypter: AssetEncrypter,
    private val userModule: UserModule,
    private val userProvider: UserProvider,
    private val userController: UserController,
    private val localAssetsStoreController: LocalAssetsStoreController,
    private val clientScope: ClientScope,
    private val safehillLogger: SafehillLogger
) : UploadOperation {

    override val listeners: MutableList<UploadOperationListener> =
        Collections.synchronizedList(mutableListOf())

    private val listenerRegistry = UploadListenersRegistry(listeners)

    private val uploadStates = UploadStates()
    private val sharingStates = SharingStates()

    private val uploadQueue = ChannelQueue(
        processor = UploadProcessor(
            uploadExecutor = RobustUploadManager(
                serverProxy = serverProxy,
                encrypter = encrypter,
                localAssetsStoreController = localAssetsStoreController,
                safehillLogger = safehillLogger,
                retryManager = DefaultRetryManager(ExponentialBackoffRetryPolicy()),
                uploadListenersRegistry = listenerRegistry
            ),
            uploadStates = uploadStates,
            uploadOperation = this
        ),
        scope = clientScope,
        retryPolicy = ExponentialBackoffRetryPolicy()
    )

    private val shareQueue = ChannelQueue(
        processor = AssetSharingProcessor(
            sharingExecutor = DefaultSharingExecutor(
                serverProxy = serverProxy,
                localAssetsStoreController = localAssetsStoreController,
                userController = userController,
                retryManager = DefaultRetryManager(ExponentialBackoffRetryPolicy())
            ),
            sharingStates = sharingStates
        ),
        scope = clientScope,
        retryPolicy = ExponentialBackoffRetryPolicy()
    )
    private val outboundQueueItemManager: OutboundQueueItemManagerInterface?
        get() {
            val currentUser = userProvider.getOrNull() ?: return null
            return userModule.getOutboundQueueItemManager(currentUser)
        }

    override val user: LocalUser
        get() = serverProxy.requestor

    val uploadItems = uploadStates.uploadItems

    val uploadStatesByGlobalId = uploadStates.statesByGlobalId
    val uploadStatesByLocalId = uploadStates.statesByLocalId

    override fun enqueueUpload(
        localIdentifier: AssetLocalIdentifier,
        assetQualities: List<AssetQuality>,
        groupId: GroupId,
        recipientIds: List<UserIdentifier>,
        threadId: String?
    ) {
        clientScope.launch {
            val globalIdentifier = UUID.randomUUID().toString()
            val uploadRequest = UploadRequest(
                localIdentifier = localIdentifier,
                globalIdentifier = globalIdentifier,
                qualities = assetQualities,
                groupId = groupId,
                recipients = recipientIds,
                threadId = threadId
            )
            uploadQueue.enqueue(uploadRequest)
        }
    }

    override fun enqueueShare(
        assetQualities: List<AssetQuality>,
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        recipientIds: List<UserIdentifier>,
        threadId: String?
    ) {
        clientScope.launch {
            val shareRequest = UploadRequest(
                localIdentifier = localIdentifier,
                globalIdentifier = globalIdentifier,
                qualities = assetQualities,
                groupId = groupId,
                recipients = recipientIds,
                threadId = threadId
            )
            uploadQueue.enqueue(shareRequest)
        }
    }


    override suspend fun run() {
    }
}