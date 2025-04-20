package com.safehill.kclient.tasks.outbound

import com.safehill.kclient.controllers.LocalAssetsStoreController
import com.safehill.kclient.controllers.UserController
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
import com.safehill.safehillclient.module.platform.UserModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class UploadOperationImpl(
    val serverProxy: ServerProxy,
    override val listeners: MutableList<UploadOperationListener>,
    private val encrypter: AssetEncrypter,
    private val userModule: UserModule,
    private val userProvider: UserProvider,
    private val userController: UserController,
    private val localAssetsStoreController: LocalAssetsStoreController
) : UploadOperation {

    private val outboundQueueItemManager: OutboundQueueItemManagerInterface?
        get() {
            val currentUser = userProvider.getOrNull() ?: return null
            return userModule.getOutboundQueueItemManager(currentUser)
        }

    private val outboundQueueItems: Channel<OutboundQueueItem> = Channel(Channel.UNLIMITED)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override val user: LocalUser
        get() = serverProxy.requestor

    override fun enqueueUpload(
        localIdentifier: AssetLocalIdentifier,
        assetQualities: List<AssetQuality>,
        groupId: GroupId,
        recipientIds: List<UserIdentifier>,
        threadId: String?
    ) {
        scope.launch {
            val globalIdentifier = UUID.randomUUID().toString()
            val item = OutboundQueueItem(
                operationType = OutboundQueueItem.OperationType.Upload,
                assetQualities = assetQualities,
                localIdentifier = localIdentifier,
                globalIdentifier = globalIdentifier,
                groupId = groupId,
                recipientIds = recipientIds,
                threadId = threadId
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
        scope.launch {
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
        val threadId = outboundQueueItem.threadId
        if (threadId != null) {
            listeners.forEach {
                it.enqueued(
                    threadId = threadId,
                    localIdentifier = outboundQueueItem.localIdentifier,
                    globalIdentifier = outboundQueueItem.globalIdentifier,
                    groupId = outboundQueueItem.groupId
                )
            }
        }
    }

    private fun shareIfRecipientsExist(
        outboundQueueItem: OutboundQueueItem,
        globalIdentifier: String
    ) {
        if (outboundQueueItem.recipientIds.isNotEmpty()) {
            scope.launch {
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
            assetQualities = assetQualities
        )
        sendToChannelAndNotifyListeners(item)
        outboundQueueItemManager?.addOutboundQueueItem(item)
    }

    private suspend fun upload(outboundQueueItem: OutboundQueueItem) {
        try {
            val globalIdentifier = outboundQueueItem.globalIdentifier

            // *******encrypting*******
            notifyListenersStartedEncrypting(outboundQueueItem)

            val encryptedAsset = encrypter.encryptedAsset(
                outboundQueueItem = outboundQueueItem,
                user = user
            )

            notifyListenersFinishedEncrypting(outboundQueueItem)
            // #######encrypting#######

            // *******uploading********
            notifyListenersStartedUploading(outboundQueueItem)

            uploadAsset(outboundQueueItem, encryptedAsset)

            notifyListenersFinishedUploading(outboundQueueItem, globalIdentifier)
            // #######uploading########

            shareIfRecipientsExist(outboundQueueItem, globalIdentifier)

        } catch (exception: Exception) {
            handleUploadException(outboundQueueItem, exception)
        }
    }

    private fun notifyListenersStartedEncrypting(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedEncrypting(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    private fun notifyListenersFinishedEncrypting(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.finishedEncrypting(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    private fun notifyListenersStartedUploading(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.startedUploading(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    private fun notifyListenersFinishedUploading(
        outboundQueueItem: OutboundQueueItem,
        globalIdentifier: String
    ) {
        listeners.forEach {
            it.finishedUploading(
                outboundQueueItem.localIdentifier,
                globalIdentifier,
                outboundQueueItem.groupId
            )
        }
    }

    private fun notifyListenersFailedUploading(outboundQueueItem: OutboundQueueItem) {
        listeners.forEach {
            it.failedUploading(
                outboundQueueItem.localIdentifier,
                outboundQueueItem.groupId
            )
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

    private fun handleUploadException(
        outboundQueueItem: OutboundQueueItem,
        exception: Exception
    ) {
        when (exception) {
            is SafehillError.ClientError.Conflict,
            is SafehillError.ClientError.BadRequest,
            SafehillError.ClientError.Unauthorized,
            SafehillError.ClientError.PaymentRequired -> {
                notifyListenersFailedUploading(outboundQueueItem)
                return
            }
        }
        // Move the item to the end of the queue and reduce the number of remaining retries
        scope.launch {
            sendToChannelAndNotifyListeners(outboundQueueItem)
            outboundQueueItemManager?.addOutboundQueueItem(outboundQueueItem)
        }
    }

    private suspend fun share(outboundQueueItem: OutboundQueueItem) {
        val (sender, recipients) = getSenderAndRecipients(outboundQueueItem)
        try {
            notifyListenersStartedSharing(outboundQueueItem, recipients)
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
            notifyListenersFinishedSharing(outboundQueueItem, recipients)
        } catch (_: Exception) {
            notifyListenersFailedSharing(outboundQueueItem)
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

    private fun notifyListenersFinishedSharing(
        outboundQueueItem: OutboundQueueItem,
        recipients: List<ServerUser>
    ) {
        listeners.forEach {
            it.finishedSharing(
                localIdentifier = outboundQueueItem.localIdentifier,
                globalIdentifier = outboundQueueItem.globalIdentifier,
                groupId = outboundQueueItem.groupId,
                users = recipients
            )
        }
    }

    private fun notifyListenersFailedSharing(
        outboundQueueItem: OutboundQueueItem
    ) {
        listeners.forEach {
            it.failedSharing(
                localIdentifier = outboundQueueItem.localIdentifier,
                globalIdentifier = outboundQueueItem.globalIdentifier,
                groupId = outboundQueueItem.groupId,
                users = listOf()
            )
        }
    }

    private fun notifyListenersStartedSharing(
        outboundQueueItem: OutboundQueueItem,
        recipients: List<ServerUser>
    ) {
        listeners.forEach {
            it.startedSharing(
                localIdentifier = outboundQueueItem.localIdentifier,
                globalIdentifier = outboundQueueItem.globalIdentifier,
                groupId = outboundQueueItem.groupId,
                users = recipients
            )
        }
    }

    override fun stop() {}

    override suspend fun run() {
        coroutineScope {
            launch {
                launch {
                    outboundQueueItemManager?.loadOutboundQueueItems()?.forEach {
                        sendToChannelAndNotifyListeners(it)
                    }
                }
                launch {
                    for (queueItem in outboundQueueItems) {
                        when (queueItem.operationType) {
                            OutboundQueueItem.OperationType.Upload -> {
                                upload(queueItem)
                            }

                            OutboundQueueItem.OperationType.Share -> {
                                share(queueItem)
                            }
                        }
                        outboundQueueItemManager?.removeOutboundQueueItem(queueItem)
                    }
                }
            }.invokeOnCompletion {
                while (true) {
                    outboundQueueItems.tryReceive().getOrNull() ?: break
                }
            }
        }
    }
}