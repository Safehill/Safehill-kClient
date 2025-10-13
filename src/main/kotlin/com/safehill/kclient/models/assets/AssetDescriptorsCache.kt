package com.safehill.kclient.models.assets

import com.safehill.safehillclient.ClientScope
import com.safehill.utils.flow.mapState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update


class AssetDescriptorsCache(
    clientScope: ClientScope
) {

    private val _assetDescriptors =
        MutableStateFlow(mapOf<AssetGlobalIdentifier, AssetDescriptor>())

    val assetDescriptors = _assetDescriptors.mapState { assetDescriptorMap ->
        assetDescriptorMap.values.toList()
    }

    val assetDescriptorDiffs = _assetDescriptors.zipWithNext().map { (previous, current) ->
        AssetDescriptorDiff.generateFrom(previous, current)
    }.shareIn(clientScope, SharingStarted.Eagerly)

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

    fun removeDescriptor(globalIdentifiers: List<AssetGlobalIdentifier>) {
        _assetDescriptors.update { initialMap ->
            initialMap - globalIdentifiers
        }
    }

    fun clearAssetDescriptors() {
        _assetDescriptors.update { mapOf() }
    }
}

fun <T> Flow<T>.zipWithNext(): Flow<Pair<T, T>> = flow {
    var previous: T? = null
    collect { current ->
        previous?.let { prev ->
            emit(prev to current)
        }
        previous = current
    }
}
