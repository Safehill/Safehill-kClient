package com.safehill.safehillclient.data.threads.model

import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.assets.AssetLocalIdentifier
import com.safehill.kclient.network.GlobalIdentifier
import java.time.Instant

data class SharingAssets(
    val groupId: String,
    val assets: List<SharingAsset>,
    val uploadedAt: Instant
) {

    fun upsertSharingAsset(
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier,
        state: SharingAsset.State
    ): SharingAssets {
        val newSharingAsset = SharingAsset(
            assetGlobalIdentifier = globalIdentifier,
            localIdentifier = localIdentifier,
            state = state
        )
        val updatedSharingAssets = listOf(newSharingAsset) + assets
        return copy(
            assets = updatedSharingAssets
                .distinctBy { it.assetGlobalIdentifier }
                .distinctBy { it.localIdentifier }
        )
    }

    fun removeSharingAsset(
        globalIdentifier: GlobalIdentifier,
        localIdentifier: AssetLocalIdentifier
    ): SharingAssets {
        return copy(
            assets = assets.filter {
                it.assetGlobalIdentifier == globalIdentifier && it.localIdentifier == localIdentifier
            }
        )
    }
}

data class SharingAsset(
    val assetGlobalIdentifier: AssetGlobalIdentifier,
    val localIdentifier: AssetLocalIdentifier,
    val state: State
) {
    sealed class State {
        data object Uploading : State()
        data class Failed(
            val errorMsg: String
        ) : State()
    }
}