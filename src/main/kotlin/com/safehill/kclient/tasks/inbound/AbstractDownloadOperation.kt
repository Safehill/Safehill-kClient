package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.tasks.BackgroundTask

abstract class AbstractDownloadOperation : DownloadOperation, BackgroundTask {

    abstract suspend fun getDescriptors(): List<AssetDescriptor>

    override suspend fun run() {
        try {
            val descriptors = getDescriptors()
            if (descriptors.isNotEmpty()) {
                processAssetsInDescriptors(
                    descriptors = descriptors
                )
            }
        } catch (e: Exception) {
            println("Error in download operation:$e " + e.message)
        }
    }

    private fun processAssetsInDescriptors(descriptors: List<AssetDescriptor>) {
        safehillClient.assetDescriptorCache.upsertAssetDescriptors(descriptors)

        // Will this be redundant listener?
        // Can we collect asset descriptors from descriptors cache?
        listeners.forEach {
            it.onNewAssetDescriptors(descriptors)
        }
    }
}
