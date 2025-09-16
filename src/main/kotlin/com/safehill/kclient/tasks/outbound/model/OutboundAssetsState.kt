package com.safehill.kclient.tasks.outbound.model

import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.tasks.outbound.sharing.SharingItem
import com.safehill.kclient.tasks.outbound.sharing.SharingState
import com.safehill.kclient.tasks.outbound.sharing.SharingStates
import com.safehill.kclient.tasks.outbound.upload.UploadStates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * State management for outbound asset operations
 * Generates OutboundAssets from UploadStates and SharingStates
 */
class OutboundAssetsState(
    private val uploadStates: UploadStates,
    private val sharingStates: SharingStates
) {

    val outboundAssets: Flow<Map<GroupId, OutboundAssets>> = combine(
        uploadStates.uploadItems,
        sharingStates.sharingItems
    ) { uploads, shares ->
        generateOutboundAssets(uploads, shares)
    }

    private fun generateOutboundAssets(
        uploads: List<UploadItem>,
        shares: List<SharingItem>
    ): Map<GroupId, OutboundAssets> {
        // Convert uploads to outbound assets
        val uploadAssets = uploads.map { uploadItem ->
            OutboundAsset(
                assetGlobalIdentifier = uploadItem.request.globalIdentifier,
                localIdentifier = uploadItem.request.localIdentifier,
                state = mapUploadStateToOutboundState(uploadItem.state),
                groupId = uploadItem.request.groupId
            )
        }

        // Convert shares to outbound assets
        val sharingAssets = shares.map { sharingItem ->
            OutboundAsset(
                assetGlobalIdentifier = sharingItem.request.globalIdentifier,
                localIdentifier = sharingItem.request.localIdentifier,
                state = mapSharingStateToOutboundState(sharingItem.state, sharingItem.request.recipients),
                groupId = sharingItem.request.groupId
            )
        }

        // Combine and group by groupId
        return (uploadAssets + sharingAssets)
            .groupBy { it.groupId }
            .mapValues { (groupId, assets) ->
                OutboundAssets(
                    groupId = groupId,
                    outboundAssets = assets,
                    uploadedAt = Instant.now()
                )
            }
    }

    private fun mapUploadStateToOutboundState(uploadState: UploadState): OutboundAsset.State {
        return when (uploadState) {
            UploadState.Pending,
            is UploadState.InProgress -> OutboundAsset.State.Uploading
            UploadState.Success -> OutboundAsset.State.Uploading // Will be removed when completed
            is UploadState.Failed -> OutboundAsset.State.Failed(
                // Convert UploadState.Failed to UploadFailure if needed
                // For now, create a basic failure
                com.safehill.kclient.tasks.outbound.UploadFailure.Unknown(uploadState.error)
            )
            UploadState.Cancelled -> OutboundAsset.State.Failed(
                com.safehill.kclient.tasks.outbound.UploadFailure.Cancelled
            )
        }
    }

    private fun mapSharingStateToOutboundState(
        sharingState: SharingState,
        recipients: List<String>
    ): OutboundAsset.State {
        return when (sharingState) {
            SharingState.Pending,
            is SharingState.InProgress -> OutboundAsset.State.Sharing(recipients)
            SharingState.Success -> OutboundAsset.State.Sharing(recipients) // Will be removed when completed
            is SharingState.Failed -> OutboundAsset.State.Failed(
                com.safehill.kclient.tasks.outbound.UploadFailure.Unknown(sharingState.error)
            )
            SharingState.Cancelled -> OutboundAsset.State.Failed(
                com.safehill.kclient.tasks.outbound.UploadFailure.Cancelled
            )
        }
    }
}

