package com.safehill.kclient.models.assets

import com.safehill.kclient.models.users.UserProvider
import com.safehill.kclient.models.users.getOrNull
import com.safehill.utils.flow.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class AssetDescriptorsCache(
    private val userProvider: UserProvider
) {
    private val _assetDescriptors =
        MutableStateFlow(mapOf<AssetGlobalIdentifier, AssetDescriptor>())

    val assetDescriptors = _assetDescriptors.mapState { assetDescriptorMap ->
        assetDescriptorMap.values
            .filter { it.isSharedWithCurrentUser() }
    }

    fun upsertAssetDescriptor(assetDescriptor: AssetDescriptor) {
        upsertAssetDescriptors(listOf(assetDescriptor))
    }

    fun upsertAssetDescriptors(assetDescriptors: List<AssetDescriptor>) {
        _assetDescriptors.update { initialMap ->
            initialMap + assetDescriptors.associateBy { it.globalIdentifier }
        }
    }

    fun getDescriptor(globalIdentifier: AssetGlobalIdentifier): AssetDescriptor? {
        return _assetDescriptors.value[globalIdentifier]
    }

    fun clearAssetDescriptors() {
        _assetDescriptors.update { mapOf() }
    }

    private fun AssetDescriptor.isSharedWithCurrentUser() =
        this.sharingInfo.groupIdsByRecipientUserIdentifier.any {
            it.key == userProvider.getOrNull()?.identifier
        }
}