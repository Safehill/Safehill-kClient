package com.safehill.safehillclient.module.asset

import com.safehill.kclient.models.assets.AssetDescriptorsCache
import com.safehill.kclient.tasks.outbound.AssetEncrypter
import com.safehill.safehillclient.asset.AssetsUploadPipelineStateHolder
import com.safehill.safehillclient.module.platform.PlatformModule

class AssetModule(
    private val platformModule: PlatformModule,
) {

    val assetsUploadPipelineStateHolder: AssetsUploadPipelineStateHolder by lazy {
        AssetsUploadPipelineStateHolder()
    }

    val assetEncrypter by lazy {
        AssetEncrypter(
            resizer = platformModule.imageResizer,
            localAssetGetter = platformModule.localAssetGetter
        )
    }

    val assetDescriptorCache by lazy {
        AssetDescriptorsCache()
    }

}