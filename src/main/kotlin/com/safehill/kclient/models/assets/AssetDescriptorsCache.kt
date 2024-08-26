package com.safehill.kclient.models.assets

import com.safehill.SafehillClient
import com.safehill.kclient.models.users.LocalUser
import com.safehill.utils.flow.mapState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class AssetDescriptorsCache(
    private val currentUser: LocalUser
) {
    private val _assetDescriptors =
        MutableStateFlow(mapOf<AssetGlobalIdentifier, AssetDescriptor>())
    val assetDescriptors = _assetDescriptors.mapState { assetDescriptorMap ->
        assetDescriptorMap.filter {
            SafehillClient.logger.error(
                it.value.toString()
            )
            it.value.isSharedWithCurrentUser()
        }
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
        this.sharingInfo.groupIdsByRecipientUserIdentifier.contains(currentUser.identifier)
}