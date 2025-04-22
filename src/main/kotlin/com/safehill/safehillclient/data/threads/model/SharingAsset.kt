package com.safehill.safehillclient.data.threads.model

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.models.assets.GroupId
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.tasks.outbound.UploadFailure
import java.time.Instant

data class SharingAssets(
    val groupId: String,
    val assets: List<SharingAsset>,
    val uploadedAt: Instant
) {

    val inProgressAssets = assets.filter { it.state is SharingAsset.State.Uploading }

    val failedAssets = assets.filter { it.state is SharingAsset.State.Failed }

    fun upsertSharingAsset(
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        state: SharingAsset.State,
        groupId: GroupId
    ): SharingAssets {
        val newSharingAsset = SharingAsset(
            assetGlobalIdentifier = globalIdentifier,
            localIdentifier = localIdentifier,
            state = state,
            groupId = groupId
        )
        val updatedSharingAssets = listOf(newSharingAsset) + assets
        return copy(
            assets = updatedSharingAssets
                .distinctBy { it.assetGlobalIdentifier }
                .distinctBy { it.localIdentifier }
                .sortedBy { it.assetGlobalIdentifier }
        )
    }

    fun removeSharingAsset(
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier
    ): SharingAssets {
        return copy(
            assets = assets
                .filterNot {
                    it.assetGlobalIdentifier == globalIdentifier && it.localIdentifier == localIdentifier
                }
        )
    }
}

data class SharingAsset(
    val assetGlobalIdentifier: AssetGlobalIdentifier,
    val localIdentifier: AssetLocalIdentifier,
    val state: State,
    val groupId: String
) {
    sealed class State {
        data object Uploading : State()
        data class Failed(
            val uploadFailure: UploadFailure
        ) : State()
    }
}