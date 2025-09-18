package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.models.assets.AssetGlobalIdentifier
import com.safehill.kclient.models.dtos.websockets.AssetDescriptorsChanged
import com.safehill.kclient.network.ServerProxy
import com.safehill.kclient.network.WebSocketApi
import com.safehill.kclient.tasks.BackgroundTask
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class RemoteDownloadOperation(
    private val assetDescriptorsCache: AssetDescriptorsCache,
    private val serverProxy: ServerProxy,
    private val webSocketApi: WebSocketApi
) : BackgroundTask {

    override suspend fun run() {
        coroutineScope {
            launch {
                webSocketApi.isConnected.collect { isConnected ->
                    if (isConnected) {
                        refreshDescriptors(null)
                    }
                }
            }
            launch {
                webSocketApi.socketMessages.collect { socketMessage ->
                    if (socketMessage is AssetDescriptorsChanged) {
                        refreshDescriptors(globalIdentifiers = socketMessage.descriptors)
                    }
                }
            }
        }
    }


    private suspend fun refreshDescriptors(globalIdentifiers: List<AssetGlobalIdentifier>? = null) {
        val remoteDescriptors = serverProxy.remoteServer.getAssetDescriptors(
            assetGlobalIdentifiers = globalIdentifiers,
            groupIds = null,
            after = null
        )
        val filteredDescriptors = remoteDescriptors
            .filter { it.uploadState.isDownloadable() }
        filteredDescriptors.forEach { descriptor ->
            serverProxy.localServer.storeAssetDescriptor(descriptor)
        }
        assetDescriptorsCache.upsertAssetDescriptors(filteredDescriptors)
    }
}