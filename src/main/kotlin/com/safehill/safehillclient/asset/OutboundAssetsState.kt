package com.safehill.safehillclient.asset

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.ServerUser
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.tasks.outbound.OutboundQueueItem
import com.safehill.kclient.tasks.outbound.UploadFailure
import com.safehill.kclient.tasks.outbound.UploadOperationErrorListener
import com.safehill.kclient.tasks.outbound.model.OutboundAsset
import com.safehill.kclient.tasks.outbound.model.OutboundAssets
import com.safehill.safehillclient.SafehillClient
import com.safehill.safehillclient.utils.extensions.assetModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

class OutboundAssetsState : UploadOperationErrorListener {

    private val _outboundAssets = MutableStateFlow<Map<GroupId, OutboundAssets>>(mapOf())
    val outboundAssets = _outboundAssets.asStateFlow()

    override fun onError(
        globalIdentifier: AssetGlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: GroupId,
        uploadFailure: UploadFailure
    ) {
        upsertSharingAssets(
            globalIdentifier = globalIdentifier,
            localIdentifier = localIdentifier,
            groupId = groupId,
            state = OutboundAsset.State.Failed(uploadFailure)
        )
    }

    override fun finishedUploading(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId
    ) {
        removeOutboundAssets(
            groupId = groupId,
            globalIdentifier = globalIdentifier,
            localIdentifier = localIdentifier
        )
    }

    override fun finishedSharing(
        localIdentifier: AssetLocalIdentifier,
        globalIdentifier: AssetGlobalIdentifier,
        groupId: GroupId,
        users: List<ServerUser>
    ) {
        removeOutboundAssets(
            groupId = groupId,
            globalIdentifier = globalIdentifier,
            localIdentifier = localIdentifier
        )
    }

    override fun enqueued(
        outboundQueueItem: OutboundQueueItem
    ) {
        upsertSharingAssets(
            globalIdentifier = outboundQueueItem.globalIdentifier,
            localIdentifier = outboundQueueItem.localIdentifier,
            groupId = outboundQueueItem.groupId,
            state = when (outboundQueueItem.operationType) {
                OutboundQueueItem.OperationType.Upload -> OutboundAsset.State.Uploading
                OutboundQueueItem.OperationType.Share -> OutboundAsset.State.Sharing(
                    outboundQueueItem.recipientIds
                )
            }
        )
    }


    private fun upsertSharingAssets(
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        groupId: String,
        state: OutboundAsset.State
    ) {
        _outboundAssets.update {
            val outboundAsset = it.getOrElse(groupId) {
                OutboundAssets(
                    groupId = groupId,
                    outboundAssets = listOf(),
                    uploadedAt = Instant.now()
                )
            }
            val newEntry = groupId to outboundAsset.upsertSharingAsset(
                globalIdentifier = globalIdentifier,
                localIdentifier = localIdentifier,
                state = state,
                groupId = groupId
            )
            it + newEntry
        }
    }

    private fun removeOutboundAssets(
        groupId: GroupId,
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier
    ) {
        _outboundAssets.update { initial ->
            val outboundAsset = initial[groupId]
            if (outboundAsset == null) {
                initial
            } else {
                outboundAsset.removeOutboundAsset(
                    globalIdentifier = globalIdentifier,
                    localIdentifier = localIdentifier
                ).takeIf { it.outboundAssets.isNotEmpty() }
                    ?.let { updatedSharingAssets -> initial + (groupId to updatedSharingAssets) }
                    ?: (initial - groupId)
            }
        }
    }
}

val SafehillClient.outboundAssetsState
    get() = this.assetModule.outboundAssetsState