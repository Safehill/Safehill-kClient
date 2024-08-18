package com.safehill.kclient.tasks.inbound

import com.safehill.kclient.models.assets.AssetDescriptor


interface DownloadOperationListener {

    fun onNewAssetDescriptors(
        assetDescriptors: List<AssetDescriptor>
    )


}