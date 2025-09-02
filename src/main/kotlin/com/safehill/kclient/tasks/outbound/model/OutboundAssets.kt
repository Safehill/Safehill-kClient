package com.safehill.kclient.tasks.outbound.model

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.models.users.UserIdentifier
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.tasks.outbound.UploadFailure
import java.time.Instant

data class OutboundAssets(
    val groupId: String,
    val outboundAssets: List<OutboundAsset>,
    val uploadedAt: Instant
) {
    val inProgressAssets = outboundAssets.filter { it.state.inProgress }

    val failedAssets = outboundAssets.filter { !it.state.inProgress }

    fun upsertSharingAsset(
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        state: OutboundAsset.State,
        groupId: GroupId
    ): OutboundAssets {
        val newOutboundAsset = OutboundAsset(
            assetGlobalIdentifier = globalIdentifier,
            localIdentifier = localIdentifier,
            state = state,
            groupId = groupId
        )
        val updatedSharingAssets = listOf(newOutboundAsset) + outboundAssets
        return copy(
            outboundAssets = updatedSharingAssets
                .distinctBy { it.assetGlobalIdentifier }
                .distinctBy { it.localIdentifier }
                .sortedBy { it.assetGlobalIdentifier }
        )
    }

    fun removeOutboundAsset(
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier
    ): OutboundAssets {
        return copy(
            outboundAssets = outboundAssets
                .filterNot {
                    it.assetGlobalIdentifier == globalIdentifier && it.localIdentifier == localIdentifier
                }
        )
    }
}


class OutboundAsset(
    val assetGlobalIdentifier: AssetGlobalIdentifier,
    val localIdentifier: AssetLocalIdentifier,
    val state: State,
    val groupId: String
) {
    sealed class State(
        val inProgress: Boolean
    ) {

        data object Uploading : State(true)

        data class Sharing(val userIdentifiers: List<UserIdentifier>) : State(true)

        data class Failed(
            val uploadFailure: UploadFailure
        ) : State(false)
    }
}