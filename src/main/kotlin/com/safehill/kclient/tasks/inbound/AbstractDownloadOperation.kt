package com.safehill.kclient.tasks.inbound

import com.safehill.SafehillClient
import com.safehill.kclient.controllers.AssetsDownloadManager
import com.safehill.kclient.errors.DownloadError
import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.DecryptedAsset
import com.safehill.kclient.models.assets.LibraryPhoto
import com.safehill.kclient.network.GlobalIdentifier
import com.safehill.kclient.tasks.BackgroundTask
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

abstract class AbstractDownloadOperation(
    safehillClient: SafehillClient
) : DownloadOperation, BackgroundTask {

    private val assetsDownloadManager: AssetsDownloadManager = AssetsDownloadManager(
        safehillClient
    )

    abstract val libraryPhotoProvider: LibraryPhotoProvider

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

    private suspend fun List<AssetDescriptor>.filterBackedUpAssetsAndInform(): List<AssetDescriptor> {
        val libraryAssets = libraryPhotoProvider.getLocalPhotos()
        val libraryAssetWithLocalIdentifier = libraryAssets.associateBy { it.localIdentifier }

        val remoteAssets = this
        val backedUpAssets = mutableMapOf<GlobalIdentifier, LibraryPhoto>()
        val nonBackedUpAssets = mutableListOf<AssetDescriptor>()

        remoteAssets.forEach { remoteAsset ->
            val backedUpAsset = libraryAssetWithLocalIdentifier[remoteAsset.localIdentifier]
            if (backedUpAsset != null) {
                backedUpAssets[remoteAsset.globalIdentifier] = backedUpAsset
            } else {
                nonBackedUpAssets.add(remoteAsset)
            }
        }
        listeners.forEach { it.didIdentify(backedUpAssets) }
        return nonBackedUpAssets
    }

    private suspend fun processAssetsInDescriptors(descriptors: List<AssetDescriptor>) {
        coroutineScope {
            val downloadableDescriptors = descriptors.filterBackedUpAssetsAndInform()
            downloadableDescriptors.forEach { descriptor ->
                launch {
                    assetsDownloadManager.downloadAsset(
                        descriptor = descriptor
                    ).onSuccess { decryptedAsset ->
                        decryptedAsset.informDelegates()
                    }.onFailure { error ->
                        error.handleDownloadError(
                            descriptor = descriptor
                        )
                    }
                }
            }
        }
    }

    private fun DecryptedAsset.informDelegates() {
        listeners.forEach { it.fetched(this) }
    }


    private fun Throwable.handleDownloadError(
        descriptor: AssetDescriptor
    ) {
        if (this is DownloadError.IsBlacklisted) {
            listeners.forEach {
                it.didFailRepeatedlyDownloadOfAsset(
                    globalIdentifier = descriptor.globalIdentifier,
                )
            }
        } else {
            listeners.forEach {
                it.didFailDownloadOfAsset(
                    globalIdentifier = descriptor.globalIdentifier,
                    error = this
                )
            }
        }
    }
}

interface LibraryPhotoProvider {
    suspend fun getLocalPhotos(): List<LibraryPhoto>
}