package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor
import com.safehill.kclient.models.assets.LibraryPhoto
import com.safehill.kclient.network.GlobalIdentifier


interface DownloadOperationListener {

    fun fetched(
        assetDescriptor: AssetDescriptor,
    )

    fun didIdentify(libraryPhotos: Map<GlobalIdentifier,LibraryPhoto>)

    fun didFailDownloadOfAsset(
        globalIdentifier: GlobalIdentifier,
        error: Throwable
    )

    fun didFailRepeatedlyDownloadOfAsset(
        globalIdentifier: GlobalIdentifier,
    )

}