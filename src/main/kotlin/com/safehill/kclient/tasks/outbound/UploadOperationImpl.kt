package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.controllers.UserController
import com.safehill.kclient.logging.SafehillLogger
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.AssetQuality
import com.safehill.kclient.models.assets.EncryptedAsset
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.assets.ShareableEncryptedAsset
import com.safehill.kclient.models.assets.ShareableEncryptedAssetVersion
import com.safehill.kclient.models.users.LocalUser
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.models.users.getOrNull
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.exceptions.SafehillError
import com.safehill.kcrypto.models.ShareablePayload
import com.safehill.safehillclient.ClientScope
import com.safehill.safehillclient.module.platform.UserModule
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID

class UploadOperationImpl(
    val serverProxy: ServerProxy,
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
    private val outboundQueueItemManager: OutboundQueueItemManagerInterface?
        get() {
            val currentUser = userProvider.getOrNull() ?: return null
            return userModule.getOutboundQueueItemManager(currentUser)
        }

    private val outboundQueueItems: Channel<OutboundQueueItem> = Channel(Channel.UNLIMITED)

    override val user: LocalUser
        get() = serverProxy.requestor

    override fun enqueueUpload(
        localIdentifier: AssetLocalIdentifier,
        assetQualities: List<AssetQuality>,
        groupId: GroupId,
        recipientIds: List<UserIdentifier>,
        threadId: String?
    ) {
        clientScope.launch {
            val globalIdentifier = UUID.randomUUID().toString()
            val item = OutboundQueueItem(
                operationType = OutboundQueueItem.OperationType.Upload,
                assetQualities = assetQualities,
                localIdentifier = localIdentifier,
                globalIdentifier = globalIdentifier,
                groupId = groupId,
                recipientIds = recipientIds,
                threadId = threadId,
                operationState = OutboundQueueItem.OperationState.Enqueued
            )
            sendToChannelAndNotifyListeners(item)
            outboundQueueItemManager?.addOutboundQueueItem(item)
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
            enqueueShareItem(
                globalIdentifier = globalIdentifier,
                groupId = groupId,
                recipientIds = recipientIds,
                threadId = threadId,
                assetQualities = assetQualities,
                localIdentifier = localIdentifier
            )
        }
    }

    private suspend fun sendToChannelAndNotifyListeners(outboundQueueItem: OutboundQueueItem) {
        outboundQueueItems.send(outboundQueueItem)
        listenerRegistry.notifyOfEnqueuing(outboundQueueItem)
    }

    private fun shareIfRecipientsExist(
        outboundQueueItem: OutboundQueueItem,
        globalIdentifier: String
    ) {
        if (outboundQueueItem.recipientIds.isNotEmpty()) {
            clientScope.launch {
                enqueueShareItem(
                    assetQualities = outboundQueueItem.assetQualities,
                    globalIdentifier = globalIdentifier,
                    groupId = outboundQueueItem.groupId,
                    recipientIds = outboundQueueItem.recipientIds,
                    threadId = outboundQueueItem.threadId,
                    localIdentifier = outboundQueueItem.localIdentifier
                )
            }
        }
    }

    private suspend fun enqueueShareItem(
        assetQualities: List<AssetQuality>,
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        recipientIds: List<UserIdentifier>,
        threadId: String?
    ) {
        val item = OutboundQueueItem(
            operationType = OutboundQueueItem.OperationType.Share,
            globalIdentifier = globalIdentifier,
            groupId = groupId,
            recipientIds = recipientIds,
            threadId = threadId,
            localIdentifier = localIdentifier,
            assetQualities = assetQualities,
            operationState = OutboundQueueItem.OperationState.Enqueued
        )
        sendToChannelAndNotifyListeners(item)
        outboundQueueItemManager?.addOutboundQueueItem(item)
    }

    private suspend fun upload(outboundQueueItem: OutboundQueueItem) {
        try {
            val globalIdentifier = outboundQueueItem.globalIdentifier

            // *******encrypting*******
            listenerRegistry.notifyListenersStartedEncrypting(outboundQueueItem)

            val encryptedAsset = encrypter.encryptedAsset(
                outboundQueueItem = outboundQueueItem,
                user = user
            )

            listenerRegistry.notifyListenersFinishedEncrypting(outboundQueueItem)
            // #######encrypting#######

            // *******uploading********
            listenerRegistry.notifyListenersStartedUploading(outboundQueueItem)

            uploadAsset(outboundQueueItem, encryptedAsset)

            listenerRegistry.notifyListenersFinishedUploading(outboundQueueItem)
            // #######uploading########

            shareIfRecipientsExist(outboundQueueItem, globalIdentifier)
            outboundQueueItemManager?.removeOutboundQueueItem(outboundQueueItem)
        } catch (exception: Throwable) {
            safehillLogger.error("Error while uploading $exception")
            reEnqueueItemOrElse(outboundQueueItem, exception) {
                outboundQueueItemManager?.addOutboundQueueItem(
                    queueItem = outboundQueueItem.copy(
                        operationState = OutboundQueueItem.OperationState.Failed(
                            UploadFailure.UPLOAD
                        )
                    )
                )
                listenerRegistry.notifyListenersFailedUploading(outboundQueueItem)
            }
        }
    }


    private suspend fun uploadAsset(
        outboundQueueItem: OutboundQueueItem,
        encryptedAsset: EncryptedAsset,
    ) {
        serverProxy.remoteServer.upload(
            listOf(encryptedAsset),
            outboundQueueItem.groupId
        )
    }

    private fun reEnqueueItemOrElse(
        outboundQueueItem: OutboundQueueItem,
        exception: Throwable,
        elseBlock: suspend () -> Unit
    ) {
        clientScope.launch {
            if (!exception.isFatal) {
                sendToChannelAndNotifyListeners(outboundQueueItem)
                outboundQueueItemManager?.addOutboundQueueItem(outboundQueueItem)
            } else {
                elseBlock()
            }
        }

    }

    private val Throwable.isFatal: Boolean
        get() = when (this) {
            is SafehillError.ClientError.Conflict,
            is SafehillError.ClientError.BadRequest,
            SafehillError.ClientError.MethodNotAllowed,
            SafehillError.ClientError.PaymentRequired -> true

            else -> false
        }


    private suspend fun share(outboundQueueItem: OutboundQueueItem) {
        val (sender, recipients) = getSenderAndRecipients(outboundQueueItem)
        try {
            listenerRegistry.notifyListenersStartedSharing(outboundQueueItem, recipients)
            val sharedVersions = getShareableEncryptedAssetVersions(
                outboundQueueItem = outboundQueueItem,
                sender = sender,
                recipients = recipients
            )
            serverProxy.share(
                asset = ShareableEncryptedAsset(
                    globalIdentifier = outboundQueueItem.globalIdentifier,
                    sharedVersions = sharedVersions,
                    groupId = outboundQueueItem.groupId
                ),
                threadId = outboundQueueItem.threadId!!
            )
            listenerRegistry.notifyListenersFinishedSharing(outboundQueueItem, recipients)
            outboundQueueItemManager?.removeOutboundQueueItem(outboundQueueItem)
        } catch (exception: Exception) {
            reEnqueueItemOrElse(
                outboundQueueItem = outboundQueueItem,
                exception = exception
            ) {
                outboundQueueItemManager?.addOutboundQueueItem(
                    queueItem = outboundQueueItem.copy(
                        operationState = OutboundQueueItem.OperationState.Failed(
                            UploadFailure.SHARING
                        )
                    )
                )
                listenerRegistry.notifyListenersFailedSharing(outboundQueueItem)
            }
        }
    }

    private suspend fun getSenderAndRecipients(outboundQueueItem: OutboundQueueItem): Pair<ServerUser, List<ServerUser>> {
        val assetDescriptor = localAssetsStoreController.getAssetDescriptor(
            globalIdentifier = outboundQueueItem.globalIdentifier
        )
        val recipientIds = outboundQueueItem.recipientIds
        val senderUserIdentifier = assetDescriptor.sharingInfo.sharedByUserIdentifier
        val toFetchUsers = recipientIds + senderUserIdentifier
        val usersMap = userController.getUsers(toFetchUsers).getOrThrow()
        val sender = usersMap[senderUserIdentifier] ?: error("Asset Owner not found.")
        val recipients = recipientIds.mapNotNull { usersMap[it] }
        return sender to recipients
    }

    private suspend fun getShareableEncryptedAssetVersions(
        outboundQueueItem: OutboundQueueItem,
        sender: ServerUser,
        recipients: List<ServerUser>
    ): List<ShareableEncryptedAssetVersion> {
        val encryptedAsset = serverProxy.getAsset(
            globalIdentifier = outboundQueueItem.globalIdentifier,
            qualities = outboundQueueItem.assetQualities,
            cacheAfterFetch = true
        )
        return recipients.flatMap { recipient ->
            outboundQueueItem.assetQualities.mapNotNull { quality ->
                val encryptedVersion =
                    encryptedAsset.encryptedVersions[quality] ?: return@mapNotNull null
                val sharedSecret = user.decryptSecret(
                    sealedMessage = ShareablePayload(
                        ephemeralPublicKeyData = encryptedVersion.publicKeyData,
                        ciphertext = encryptedVersion.encryptedSecret,
                        signature = encryptedVersion.publicSignatureData,
                        recipient = recipient
                    ),
                    protocolSalt = user.encryptionSalt,
                    sender = sender
                )
                val encryptedSharedSecretForRecipient = user.shareable(
                    data = sharedSecret,
                    with = recipient,
                    protocolSalt = user.encryptionSalt
                )
                ShareableEncryptedAssetVersion(
                    quality = quality,
                    userPublicIdentifier = recipient.identifier,
                    encryptedSecret = encryptedSharedSecretForRecipient.ciphertext,
                    ephemeralPublicKey = encryptedSharedSecretForRecipient.ephemeralPublicKeyData,
                    publicSignature = encryptedSharedSecretForRecipient.signature
                )
            }
        }
    }


    override fun stop() {}


    private suspend fun loadStoredOutboundItems() {
        outboundQueueItemManager?.loadOutboundQueueItems()?.forEach { item ->
            if (item.operationState is OutboundQueueItem.OperationState.Failed) {
                listenerRegistry.notifyOfEnqueuing(item)
                when (item.operationState.uploadFailure) {
                    UploadFailure.ENCRYPTION -> {
                        listenerRegistry.notifyListenersFailedUploading(
                            outboundQueueItem = item
                        )
                    }

                    UploadFailure.UPLOAD -> {
                        listenerRegistry.notifyListenersFailedUploading(
                            outboundQueueItem = item
                        )
                    }

                    UploadFailure.SHARING -> {
                        listenerRegistry.notifyListenersFailedSharing(
                            outboundQueueItem = item
                        )
                    }
                }
            } else {
                sendToChannelAndNotifyListeners(item)
            }
        }

    }

    private suspend fun processItemsInQueue() {
        for (queueItem in outboundQueueItems) {
            when (queueItem.operationType) {
                OutboundQueueItem.OperationType.Upload -> {
                    upload(queueItem)
                }

                OutboundQueueItem.OperationType.Share -> {
                    share(queueItem)
                }
            }
        }
    }

    override suspend fun run() {
        coroutineScope {
            launch {
                launch {
                    loadStoredOutboundItems()
                }
                launch {
                    processItemsInQueue()
                }
            }.invokeOnCompletion {
                while (true) {
                    outboundQueueItems.tryReceive().getOrNull() ?: break
                }
            }
        }
    }
}