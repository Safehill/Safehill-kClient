package com.safehill.kclient.models.assets

import com.safehill.utils.flow.mapState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update

class AssetDescriptorsCache {

    private val _descriptorAddedEvent =
        MutableSharedFlow<List<AssetDescriptor>>(extraBufferCapacity = 128)
    val descriptorAddedEvent = _descriptorAddedEvent.asSharedFlow()

    private val _assetDescriptors =
        MutableStateFlow(mapOf<AssetGlobalIdentifier, AssetDescriptor>())

    val assetDescriptors = _assetDescriptors.mapState { assetDescriptorMap ->
        assetDescriptorMap.values.toList()
    }

    fun upsertAssetDescriptor(assetDescriptor: AssetDescriptor) {
        upsertAssetDescriptors(listOf(assetDescriptor))
    }

    fun upsertAssetDescriptors(assetDescriptors: List<AssetDescriptor>) {
        _descriptorAddedEvent.tryEmit(assetDescriptors)
        _assetDescriptors.update { initialMap ->
            initialMap + assetDescriptors.associateBy { it.globalIdentifier }
        }
    }

    fun getDescriptor(globalIdentifier: AssetGlobalIdentifier): AssetDescriptor? {
        return _assetDescriptors.value[globalIdentifier]
    }

    fun removeDescriptor(globalIdentifiers: List<AssetGlobalIdentifier>) {
        _assetDescriptors.update { initialMap ->
            initialMap - globalIdentifiers
        }
    }

    fun clearAssetDescriptors() {
        _assetDescriptors.update { mapOf() }
    }
}